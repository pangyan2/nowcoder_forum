package com.nowcoder.community.config.job;

import com.nowcoder.community.constant.CommentEntityConstant;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.es.ElasticsearchService;
import com.nowcoder.community.util.CommonUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 热门帖子排行业务
 *  帖子分数刷新计算定时任务
 * @author Alex
 * @version 1.0
 * @company xxx
 * @copyright (c)  xxxInc. All rights reserved.
 * @date 2022/2/21 17:56
 */
@Slf4j
public class DiscussPostScoreRefreshJob implements Job {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    private static final Date EPOCH;

    static {
        try {
            EPOCH = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2014-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化牛科纪元失败!",e);
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String postScoreKey = RedisKeyUtil.getPostScoreKey();
        BoundSetOperations operations = redisTemplate.boundSetOps(postScoreKey);
        if (operations.size() == 0) {
            log.info("[任务取消]:没有要刷新的帖子");
            return;
        }

        log.info("[任务开始]:正在刷新帖子分数："+ operations.size());

        while (operations.size()>0){
            this.refresh((Integer) operations.pop());
        }

        log.info("[任务结束]：帖子分数刷新完毕!");
    }

    /**
     * 刷新单个帖子的分数
     * @param postId
     */
    private void refresh(int postId){
        DiscussPost discussPost = discussPostService.findDiscussPostById(postId);

        if (CommonUtil.isEmtpy(discussPost)){
            log.error("该帖子不存在:id="+ postId);
            return;
        }

        // 是否加精
        boolean isFine = discussPost.getStatus() == 1;

        // 评论数量
        int commentCount = discussPost.getCommentCount();

        // 点赞数量
        long likeCount = likeService.findEntityLikeCount(CommentEntityConstant.ENTITY_TYPE_POST.getType(), postId);

        // 计算权重
        double w = (isFine?75:0) + commentCount * 10 + likeCount * 2;

        // 分数 = 帖子权重 + 距离天数
        double score = Math.log10(Math.max(w,1)) + (discussPost.getCreateTime().getTime() - EPOCH.getTime()) / (1000 * 3600 * 24);
        // 更新帖子分数
        discussPostService.updateScore(postId,score);

        // 同步搜索数据到es服务器
        discussPost.setScore(score);
        elasticsearchService.saveDiscussPost(discussPost);

    }
}
