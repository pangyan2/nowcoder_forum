package com.nowcoder.community.controller;

import com.nowcoder.community.config.event.EventProducer;
import com.nowcoder.community.constant.MessageConstant;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.util.CommonUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alex
 * @version 1.0
 * @company xxx
 * @copyright (c)  xxxInc. All rights reserved.
 * @date 2022/2/22 12:06
 */
@Controller
@Slf4j
@RequestMapping("/share")
@Api(tags = "分享接口")
public class ShareController {
    /**
     * 生成图片时间比较长，一定是异步方式，最好使用事件驱动方式（分享事件）实现，controller->kafka 异步实现即可
     */
    @Autowired
    private EventProducer eventProducer;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${qiniu.bucket.share.url}")
    private String shareBucketUrl;

    @RequestMapping(path = "/shareImage",method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "分享长图")
    public String share(String htmlUrl){
        // 文件名
        String fileName = CommonUtil.generateUUID();

        // 构建kafka event,异步生成长图
        Event event = new Event()
                .setTopic(MessageConstant.TOPIC_SHARE)
                .setData("htmlUrl",htmlUrl)
                .setData("fileName",fileName)
                .setData("suffix",".png");

        eventProducer.handleEvent(event);

        // 返回访问路径
        Map<String, Object> map = new HashMap<>(16);
//        String url = domain + contextPath + "/share/image/" + fileName;
        map.put("shareUrl",shareBucketUrl + "/" + fileName);


        return CommonUtil.getJsonString(0,null,map);
    }

    @RequestMapping(path = "/image/{fileName}",method = RequestMethod.GET)
    @Deprecated
    public void getShareImage(@PathVariable("fileName") String fileName, HttpServletResponse response){
        if (StringUtils.isBlank(fileName)){
            throw new IllegalArgumentException("文件名不能为空");
        }

        response.setContentType("image/png");
        File file = new File(wkImageStorage + "/" + fileName + ".png");
        try {
            ServletOutputStream os = response.getOutputStream();
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int offset = 0;
            while ((offset=fis.read(buffer))!=-1) {
                os.write(buffer);
            }
        } catch (IOException e) {
            log.error("获取长图失败:"+e.getMessage());
        }

    }


}
