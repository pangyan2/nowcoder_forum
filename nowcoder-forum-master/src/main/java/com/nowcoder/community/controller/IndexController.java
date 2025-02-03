package com.nowcoder.community.controller;

import com.nowcoder.community.constant.CommentEntityConstant;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;

import com.nowcoder.community.util.CommonUtil;
import com.nowcoder.community.util.ThreadLocalHolder;
import com.nowcoder.community.vo.PageInfo;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 主页控制层
 * @author Alex
 * @version 1.0
 * @date 2022/1/30 17:09
 */
@Controller
@Slf4j
@Api(tags = "首页接口")
public class IndexController {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ThreadLocalHolder<User> userThreadLocalHolder;

    /**
     * 首页信息
     * @param model 作用域对象
     * @param pageInfo 分页对象
     * @param orderMode 表示是否显示热度排行版
     * @return
     */
    @RequestMapping(path = {"/index","/"},method = RequestMethod.GET)
    public String getIndexPage(Model model, PageInfo pageInfo, @RequestParam(name = "orderMode",defaultValue = "0") int orderMode){
        pageInfo.setRows(discussPostService.findDiscussPostCount(0));
        pageInfo.setPath("/index?orderMode="+orderMode);
        List<DiscussPost> discussPostList = discussPostService.findDiscussPostList(0, pageInfo.getOffset(), pageInfo.getLimit(),orderMode);
        List<Map<String,Object>> userDiscussPosts = new ArrayList<>();
        if(discussPostList!=null){
            // 迭代容器装配用户对应的帖子
            for (DiscussPost discussPost : discussPostList) {
                Map<String,Object> map = new HashMap<>(16);
                map.put("post",discussPost);
                User user = userService.findUserById(discussPost.getUserId());
                map.put("user",user);

                long likeCount = likeService.findEntityLikeCount(CommentEntityConstant.ENTITY_TYPE_POST.getType(),discussPost.getId());
                map.put("likeCount",likeCount);
                userDiscussPosts.add(map);
            }

        }
        model.addAttribute("discussPosts",userDiscussPosts);
        model.addAttribute("orderMode",orderMode);
        return "/index";
    }

    /**
     * 返回500服务器处理错误页面
     * @return
     */
    @RequestMapping(path = "/error",method = RequestMethod.GET)
    public String getErrorPage(){
        return "/error/500";
    }

    /**
     * 权限不够，访问拒绝，服务器找不到资源的跳转的页面
     * @return
     */
    @RequestMapping(path = "/denied",method = RequestMethod.GET)
    public String getDeniedPage(){
        return "/error/404";
    }



}
