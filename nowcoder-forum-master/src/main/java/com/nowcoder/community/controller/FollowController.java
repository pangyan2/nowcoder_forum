package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.config.event.EventProducer;
import com.nowcoder.community.constant.CommentEntityConstant;
import com.nowcoder.community.constant.MessageConstant;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommonUtil;
import com.nowcoder.community.util.ThreadLocalHolder;
import com.nowcoder.community.vo.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * 关注功能控制层
 * @author Alex
 * @version 1.0
 * @date 2022/2/13 21:03
 */
@Controller
@RequestMapping("/follow")
@Api(tags = "关注接口")       
public class FollowController {

    @Autowired
    private FollowService followService;

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private ThreadLocalHolder<User> userThreadLocalHolder;

    @RequestMapping(path = "/toFollow",method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired
    @ApiOperation(value = "关注")
    public String toFollow(int entityType,int entityId){
        User user = userThreadLocalHolder.getCache();
        followService.toFollow(user.getId(),entityType,entityId);
        // 触发关注事件
        Event event = new Event()
                .setTopic(MessageConstant.TOPIC_FOLLOW)
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId)
                .setUserId(entityId);
        eventProducer.handleEvent(event);
        return CommonUtil.getJsonString(0,"已关注");
    }

    @RequestMapping(path = "/unFollow",method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired
    @ApiOperation(value = "取消关注")
    public String unFollow(int entityType,int entityId){
        User user = userThreadLocalHolder.getCache();
        followService.unFollow(user.getId(),entityType,entityId);
        return CommonUtil.getJsonString(0,"已取消关注");
    }

    @RequestMapping(path = "/followees/{userId}",method = RequestMethod.GET)
    @LoginRequired
    public String getfollowees(@PathVariable("userId") int userId, PageInfo pageInfo, Model model){
        User user = userService.findUserById(userId);
        if(CommonUtil.isEmtpy(user)){
            throw new RuntimeException("该用户不存在");
        }
        model.addAttribute("user",user);

        // 设置分页
        pageInfo.setLimit(5);
        pageInfo.setPath("/followees/" + userId);
        pageInfo.setRows((int) followService.findFolloweeCount(userId, CommentEntityConstant.ENTITY_TYPE_USER.getType()));

        // 封装视图层数据
        List<Map<String,Object>> userList = followService.findFollowees(userId,pageInfo.getOffset(),pageInfo.getLimit());
        if(!CommonUtil.isEmtpy(userList)){
            for (Map<String,Object> map:userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        model.addAttribute("userList",userList);
        return "/site/followee";
    }

    @RequestMapping(path = "/followers/{userId}",method = RequestMethod.GET)
    @LoginRequired
    public String getfollowers(@PathVariable("userId") int userId, PageInfo pageInfo, Model model){
        User user = userService.findUserById(userId);
        if(CommonUtil.isEmtpy(user)){
            throw new RuntimeException("该用户不存在");
        }
        model.addAttribute("user",user);

        // 设置分页
        pageInfo.setLimit(5);
        pageInfo.setPath("/followers/" + userId);
        pageInfo.setRows((int) followService.findFollowerCount(CommentEntityConstant.ENTITY_TYPE_USER.getType(),userId));

        // 封装视图层数据
        List<Map<String,Object>> userList = followService.findFollowers(userId,pageInfo.getOffset(),pageInfo.getLimit());
        if(!CommonUtil.isEmtpy(userList)){
            for (Map<String,Object> map:userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        model.addAttribute("userList",userList);
        return "/site/follower";
    }

    /**
     * 判断当前用户是否关注过用户id为userId的用户
     * @param userId
     * @return true/false
     */
    private boolean hasFollowed(int userId){
        User user = userThreadLocalHolder.getCache();
        if(CommonUtil.isEmtpy(user)){
            return false;
        }
        return followService.hasFollowed(user.getId(),CommentEntityConstant.ENTITY_TYPE_USER.getType(),userId);
    }
}
