package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.config.event.EventProducer;
import com.nowcoder.community.constant.CommentEntityConstant;
import com.nowcoder.community.constant.MessageConstant;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.CommonUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import com.nowcoder.community.util.ThreadLocalHolder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * 点赞管理控制层
 * @author Alex
 * @version 1.0
 * @date 2022/2/13 16:06
 */
@Controller
@RequestMapping("/like")
@Api(tags = "点赞功能接口")
public class LikeController {

    @Autowired
    private LikeService likeService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private ThreadLocalHolder<User> userThreadLocalHolder;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/giveLike",method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired
    @ApiOperation(value = "用户点赞帖子")
    public String like(int entityType,int entityId,int entityUserId,int postId){
        User user = userThreadLocalHolder.getCache();

        // 点赞
        likeService.like(user.getId(),entityType,entityId,entityUserId);
        // 数量
        long likeCount = likeService.findEntityLikeCount(entityType,entityId);
        // 状态
        int likeStatus = likeService.findEntityLikeStatus(user.getId(),entityType,entityId);

        // 封装返回结果
        Map<String, Object> map = new HashMap<>(16);
        map.put("likeCount",likeCount);
        map.put("likeStatus",likeStatus);

        // 触发点赞事件
        if (likeStatus == 1) {
            Event event = new Event()
                    .setTopic(MessageConstant.TOPIC_LIKE)
                    .setUserId(userThreadLocalHolder.getCache().getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId",postId);
            eventProducer.handleEvent(event);
        }

        if (entityType == CommentEntityConstant.ENTITY_TYPE_POST.getType()){
            // 记录影响帖子分数帖子id
            String postScoreKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(postScoreKey,postId);
        }

        return CommonUtil.getJsonString(0,null,map);
    }
}
