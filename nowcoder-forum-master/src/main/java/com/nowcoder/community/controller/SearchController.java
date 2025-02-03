package com.nowcoder.community.controller;

import com.nowcoder.community.constant.CommentEntityConstant;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.service.es.ElasticsearchService;
import com.nowcoder.community.util.CommonUtil;
import com.nowcoder.community.vo.PageInfo;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索功能控制层
 * @author Alex
 * @version 1.0
 * @date 2022/2/18 16:12
 */
@Controller
@RequestMapping("/search")
@Api(tags = "搜索功能接口")
public class SearchController {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    /**
     * 搜索方法
     *      提交方式为get,提交路径/search/search_kw?keyword=xxx
     * @param keyword
     * @param pageInfo
     * @param model
     * @return
     */
    @RequestMapping(value = "/search_kw",method = RequestMethod.GET)
    public String search(String keyword, PageInfo pageInfo, Model model){

        // 搜索帖子
        Page<DiscussPost> searchResultPage = elasticsearchService.searchDiscussPostByCondition(keyword, pageInfo.getCurrent() - 1, pageInfo.getLimit());

        // 聚合数据，封装数据
        List<Map<String,Object>> discussPosts = new ArrayList<>();
        if(!CommonUtil.isEmtpy(searchResultPage)){
            for (DiscussPost discussPost : searchResultPage) {
                Map<String,Object> map = new HashMap<>(16);
                // 帖子数据
                map.put("post",discussPost);
                // 用户数据
                map.put("user",userService.findUserById(discussPost.getUserId()));
                // 帖子点赞数据
                map.put("likeCount",likeService.findEntityLikeCount(CommentEntityConstant.ENTITY_TYPE_POST.getType(),discussPost.getId()));
                discussPosts.add(map);
            }
        }

        // 传入作用域数据给页面模板
        model.addAttribute("discussPosts",discussPosts);
        model.addAttribute("keyword",keyword);

        //设置分页信息
        pageInfo.setPath("/search/search_kw?keyword=" + keyword);
        pageInfo.setRows(searchResultPage==null?0: (int) searchResultPage.getTotalElements());
        return "/site/search";
    }
}
