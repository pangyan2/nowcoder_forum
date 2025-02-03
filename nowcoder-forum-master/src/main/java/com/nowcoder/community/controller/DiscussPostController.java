package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.config.event.EventProducer;
import com.nowcoder.community.constant.CommentEntityConstant;
import com.nowcoder.community.constant.MessageConstant;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommonUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import com.nowcoder.community.util.ThreadLocalHolder;
import com.nowcoder.community.vo.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * 帖子管理控制层
 * @author Alex
 * @version 1.0
 * @date 2022/2/6 21:48
 */
@Controller
@RequestMapping("/discuss")
@Api(tags = "帖子接口")
public class DiscussPostController {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ThreadLocalHolder<User> userThreadLocalHolder;

    @RequestMapping(path = "/add",method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired
    @ApiOperation(value = "添加帖子")
    public String addDiscussPost(String title,String content){
        User user = userThreadLocalHolder.getCache();
        if(CommonUtil.isEmtpy(user)){
            return CommonUtil.getJsonString(403,"你还没有登录哦!");
        }

        DiscussPost discussPost = new DiscussPost();
        discussPost.setUserId(user.getId());
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setCreateTime(new Date());
        discussPostService.addDiscussPost(discussPost);

        // 触发发帖事件,将新发布的帖子异步同步到es服务器
        Event event = new Event()
                .setTopic(MessageConstant.TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(CommentEntityConstant.ENTITY_TYPE_POST.getType())
                .setEntityId(discussPost.getId());

        eventProducer.handleEvent(event);

        // 计算帖子分数
        String postScoreKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(postScoreKey,discussPost.getId());


        // 报错的情况在全局异常处理器中处理
        return CommonUtil.getJsonString(200,"发布成功");
    }

    @RequestMapping(path = "/detail/{discussPostId}",method = RequestMethod.GET)
    @LoginRequired
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, PageInfo pageInfo){
        // 查询帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post",post);

        // 查询作者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user",user);

        // 点赞
        // 点赞数量
        long likeCount = likeService.findEntityLikeCount(CommentEntityConstant.ENTITY_TYPE_POST.getType(),discussPostId);
        model.addAttribute("likeCount",likeCount);
        // 点赞状态
        int likeStatus = userThreadLocalHolder.getCache()==null ? 0:likeService.findEntityLikeStatus(userThreadLocalHolder.getCache().getId(),CommentEntityConstant.ENTITY_TYPE_POST.getType(),discussPostId);
        model.addAttribute("likeStatus",likeStatus);

        // 分页查询帖子评论
        pageInfo.setLimit(5);
        pageInfo.setPath("/discuss/detail/" + discussPostId);
        pageInfo.setRows(post.getCommentCount());
        List<Comment> commentList = commentService.findCommentByEntity(CommentEntityConstant.ENTITY_TYPE_POST.getType(), post.getId(), pageInfo.getOffset(), pageInfo.getLimit());

        // 封装所有查询到的结果
        // 评论：给帖子的评论
        // 回复：给评论的评论
        // 评论列表
        List<Map<String,Object>> commentVoList = new ArrayList<>();
        if(!CommonUtil.isEmtpy(commentList)){
            for (Comment comment:commentList){
                // 一个评论的VO
                Map<String,Object> commentVo = new HashMap<>(10);
                // 评论
                commentVo.put("comment",comment);
                // 评论作者
                commentVo.put("user",userService.findUserById(comment.getUserId()));
                // 回复列表：评论的评论
                List<Comment> replyList = commentService.findCommentByEntity(CommentEntityConstant.ENTITY_TYPE_COMMENT.getType(), comment.getId(), 0, Integer.MAX_VALUE);
                // 点赞数量
                likeCount = likeService.findEntityLikeCount(CommentEntityConstant.ENTITY_TYPE_COMMENT.getType(),comment.getId());
                commentVo.put("likeCount",likeCount);
                // 点赞状态
                likeStatus = userThreadLocalHolder.getCache()==null ? 0:likeService.findEntityLikeStatus(userThreadLocalHolder.getCache().getId(),CommentEntityConstant.ENTITY_TYPE_COMMENT.getType(),comment.getId());
                commentVo.put("likeStatus",likeStatus);
                // 回复VO列表
                List<Map<String,Object>> replyVoList = new ArrayList<>();
                if(!CommonUtil.isEmtpy(replyList)){
                    for(Comment reply:replyList){
                        Map<String,Object> replyVo = new HashMap<>(10);
                        // 回复
                        replyVo.put("reply",reply);
                        // 作者
                        replyVo.put("user",userService.findUserById(reply.getUserId()));
                        // 点赞数量
                        likeCount = likeService.findEntityLikeCount(CommentEntityConstant.ENTITY_TYPE_COMMENT.getType(),reply.getId());
                        replyVo.put("likeCount",likeCount);
                        // 点赞状态
                        likeStatus = userThreadLocalHolder.getCache()==null ? 0:likeService.findEntityLikeStatus(userThreadLocalHolder.getCache().getId(),CommentEntityConstant.ENTITY_TYPE_COMMENT.getType(),reply.getId());
                        replyVo.put("likeStatus",likeStatus);
                        // 回复目标
                        User target = reply.getTargetId()==0?null:userService.findUserById(reply.getTargetId());
                        replyVo.put("target",target);

                        replyVoList.add(replyVo);
                    }
                }

                commentVo.put("replys",replyVoList);

                // 回复数量
                int replyCount = commentService.findCommentCount(CommentEntityConstant.ENTITY_TYPE_COMMENT.getType(), comment.getId());
                commentVo.put("replyCount",replyCount);
                commentVoList.add(commentVo);
            }
        }

        model.addAttribute("comments",commentVoList);

        return "site/discuss-detail";
    }

    /**
     * 帖子置顶
     * @param id
     * @return
     */
    @RequestMapping(path = "/top",method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "帖子置顶")
    public String setTop(int id){
        User user = userThreadLocalHolder.getCache();
        if(CommonUtil.isEmtpy(user)){
            return CommonUtil.getJsonString(403,"你还没有登录哦!");
        }
        // 1 帖子状态置顶
        discussPostService.updateType(id,1);

        // 触发一次发帖事件，异步同步到es服务器中，使得能够搜索到最新的帖子
        Event event = new Event()
                .setTopic(MessageConstant.TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(CommentEntityConstant.ENTITY_TYPE_POST.getType())
                .setEntityId(id);

        eventProducer.handleEvent(event);
        return CommonUtil.getJsonString(0);
    }

    /**
     * 帖子置顶
     * @param id
     * @return
     */
    @RequestMapping(path = "/fine",method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "帖子加精")
    public String setFine(int id){
        User user = userThreadLocalHolder.getCache();
        if(CommonUtil.isEmtpy(user)){
            return CommonUtil.getJsonString(403,"你还没有登录哦!");
        }
        // 1 帖子状态置顶
        discussPostService.updateStatus(id,1);

        // 触发一次发帖事件，异步同步到es服务器中，使得能够搜索到最新的帖子
        Event event = new Event()
                .setTopic(MessageConstant.TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(CommentEntityConstant.ENTITY_TYPE_POST.getType())
                .setEntityId(id);

        eventProducer.handleEvent(event);
        // 记录影响帖子分数帖子id
        String postScoreKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(postScoreKey,id);
        return CommonUtil.getJsonString(0);
    }

    /**
     * 帖子拉黑/删除
     * @param id
     * @return
     */
    @RequestMapping(path = "/delete",method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "删除帖子")
    public String setDelete(int id){
        User user = userThreadLocalHolder.getCache();
        if(CommonUtil.isEmtpy(user)){
            return CommonUtil.getJsonString(403,"你还没有登录哦!");
        }
        // 2 帖子状态删除
        discussPostService.updateStatus(id,2);

        // 触发一次发帖事件，异步同步到es服务器中，使得能够搜索到最新的帖子
        Event event = new Event()
                .setTopic(MessageConstant.TOPIC_DELETE)
                .setUserId(user.getId())
                .setEntityType(CommentEntityConstant.ENTITY_TYPE_POST.getType())
                .setEntityId(id);

        eventProducer.handleEvent(event);

        return CommonUtil.getJsonString(0);
    }

}
