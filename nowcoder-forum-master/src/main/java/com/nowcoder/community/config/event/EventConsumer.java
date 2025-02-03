package com.nowcoder.community.config.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.constant.MessageConstant;
import com.nowcoder.community.constant.SystemConstant;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.es.ElasticsearchService;
import com.nowcoder.community.util.CommonUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * 事件消费者:
 *      把事件对象转换成消息插入到数据库
 * @author Alex
 * @version 1.0
 * @date 2022/2/15 12:40
 */
@Component
@Slf4j
public class EventConsumer implements MessageConstant {

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${wk.image.command}")
    private String wkCommand;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    public final String JSON_DATA_KEY = "code";
    public final String JSON_DATA_VALUE = "0";


    /**
     * 一个方法可以消费kafka多个topic，一个topic可以被多个方法消费
     * @param record
     */
    @KafkaListener(topics = {TOPIC_FOLLOW,TOPIC_COMMENT,TOPIC_LIKE})
    public void handleCommentMessage(ConsumerRecord record){
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(!checkRecord(record,event)){
            return;
        }

        // 发送站内通知
        Message message = new Message();
        message.setFromId(SystemConstant.SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());
        message.setStatus(0);

        Map<String,Object> map = new HashMap<>(16);
        map.put("userId",event.getUserId());
        map.put("entityType",event.getEntityType());
        map.put("entityId",event.getEntityId());
        if (!event.getData().isEmpty()) {
            for (Map.Entry<String,Object> entry:event.getData().entrySet()) {
                map.put(entry.getKey(),entry.getValue());
            }
        }
        message.setContent(JSONObject.toJSONString(map));
        messageService.addMessage(message);
    }

    /**
     * 监听发帖事件
     * @param record
     */
    @KafkaListener(topics = {MessageConstant.TOPIC_PUBLISH})
    public void handlePublishEvent(ConsumerRecord record){
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(!checkRecord(record,event)){
            return;
        }

        DiscussPost discussPost = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(discussPost);
    }

    @KafkaListener(topics = {MessageConstant.TOPIC_DELETE})
    public void handleDeleteEvent(ConsumerRecord record){
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(!checkRecord(record,event)){
            return;
        }
        elasticsearchService.deleteDiscusspost(event.getEntityId());
    }

    private boolean checkRecord(ConsumerRecord record,Event event){
        if(CommonUtil.isEmtpy(record) || CommonUtil.isEmtpy(record.value())){
            log.error("消息内容为空.");
            return false;
        }

        if(CommonUtil.isEmtpy(event)){
            log.error("消息格式错误");
            return false;
        }
        return true;
    }

    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareMessage(ConsumerRecord record){
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(!checkRecord(record,event)){
            return;
        }

        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        // 拼接cmd命令
        String cmd = wkCommand + "--quality 75 " +
                htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;

        // 执行命令生成长图
        try {
            Runtime.getRuntime().exec(cmd);
            // 这句话比Runtime.getRuntime().exec(cmd)先执行，所以需要阻塞线程taskScheduler
            log.info("生成长图成功:" + cmd);
        } catch (IOException e) {
            log.error("生成长图失败：{}",e.getMessage());
        }

        // 消费分享事件

        // 启用定时器，监视图片是否生成，如果图片已生成，则上传至七牛云
        /**
         * 在这里使用taskScheduler，因为，消费者消费事件具有抢占机制，
         * 一个事件只有一台机器的消费者消费，一台服务器启用定时器和其他服务器没有影响
         * 所以不用分布式定时任务框架quartz,而使用taskScheduler线程池任务定时器
          */
        UploadTask uploadTask = new UploadTask(fileName,suffix);
        /**
         * Future 封装定时任务的状态，可以停止定时器
         */
        Future scheduledFuture = taskScheduler.scheduleAtFixedRate(uploadTask, 500);
        uploadTask.setFuture(scheduledFuture);


    }

    class UploadTask implements Runnable {

        /**
         * 文件名称
         */
        private String fileName;

        /**
         * 文件后缀
         */
        private String suffix;

        /**
         * 启动任务的返回值，用于停止定时器
         */
        private Future future;

        /**
         * 开始上传时间
         */
        private long startTime;

        /**
         * 上传次数
         */
        private int uploadTimes;

        public void setFuture(Future future) {
            this.future = future;
        }

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            // 极端情况，强制关闭定时器
            // 生成图片失败
            if (System.currentTimeMillis() - startTime > SystemConstant.UPLOAD_MILLISECONDS) {
                log.error("执行时间过程，终止任务:"+ fileName);
                future.cancel(true);
                return;
            }

            // 上传七牛云失败
            if (uploadTimes >= SystemConstant.UPLOAD_TIMES) {
                log.error("上传次数过多,终止任务："+ fileName);
                future.cancel(true);
                return;
            }

            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);

            if (file.exists()) {
                log.info(String .format("开始第%d次上传[%s].",++uploadTimes,fileName));
                // 设置响应信息
                StringMap policy = new StringMap();
                policy.put("returnBody",CommonUtil.getJsonString(0));

                // 生成上传凭证
                Auth auth = Auth.create(accessKey,secretKey);
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);

                // 指定上传的七牛云机房 : 华北机房
                UploadManager manager = new UploadManager(new Configuration(Zone.zone1()));

                try {
                    // 上传文件到七牛云
                    // 开始上传图片
                    Response response = manager.put(path, fileName, uploadToken, null, "images/" + suffix, false);
                    // 处理响应结果
                    JSONObject jsonData = JSONObject.parseObject(response.bodyString());



                    if (CommonUtil.isEmtpy(jsonData) || jsonData.get(JSON_DATA_KEY)==null||!JSON_DATA_VALUE.equals(jsonData.get(JSON_DATA_KEY))) {
                        log.info(String.format("第%d次上传失败[%s].",uploadTimes,fileName));
                    }else{
                        log.info(String.format("第%d次上传成功[%s].",uploadTimes,fileName));
                        future.cancel(true);
                    }
                }catch (QiniuException e) {
                    log.info(String.format("第%d次上传失败[%d].",uploadTimes,fileName));
                    log.error("文件上传失败:{}",e.getMessage());
                }
            }else {
                log.info("等待图片生成["+ fileName + "].");
            }

        }
    }


}
