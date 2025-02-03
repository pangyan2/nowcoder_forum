package com.nowcoder.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.constant.MessageConstant;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
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
import org.springframework.web.util.HtmlUtils;

import java.util.*;

/**
 * 消息管理控制层
 *
 * @author Alex
 * @version 1.0
 * @date 2022/2/9 17:27
 */
@Controller
@RequestMapping("/message")
@Api(tags = "私信接口")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private ThreadLocalHolder<User> userThreadLocalHolder;

    @RequestMapping(path = "/letter/list", method = RequestMethod.GET)
    @LoginRequired
    public String getLetterList(Model model, PageInfo pageInfo) {
        //获取登录用户信息
        User user = userThreadLocalHolder.getCache();

        //设置分页信息
        pageInfo.setLimit(5);
        pageInfo.setPath("/message/letter/list");
        pageInfo.setRows(messageService.findConversationCount(user.getId()));

        // 查询会话列表
        List<Message> conversationList = messageService.findConversations(user.getId(), pageInfo.getOffset(), pageInfo.getLimit());
        // 封装数据
        List<Map<String, Object>> conversations = new ArrayList<>();
        if (!CommonUtil.isEmtpy(conversationList)) {
            for (Message message : conversationList) {
                Map<String, Object> map = new HashMap<>(10);
                map.put("conversation", message);
                map.put("letterCount", messageService.findLetterCount(message.getConversationId()));
                map.put("unreadCount", messageService.findLetterUnreadCount(user.getId(), message.getConversationId()));
                int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
                map.put("target", userService.findUserById(targetId));
                conversations.add(map);
            }
        }
        model.addAttribute("conversations", conversations);

        //查询未读消息总数
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);
        return "/site/letter";
    }

    @RequestMapping(path = "/letter/detail/{conversationId}", method = RequestMethod.GET)
    @LoginRequired
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Model model, PageInfo pageInfo) {
        // 设置分页信息
        pageInfo.setLimit(5);
        pageInfo.setPath("/message/letter/detail/" + conversationId);
        pageInfo.setRows(messageService.findLetterCount(conversationId));

        // 得到私信列表
        List<Message> letterList = messageService.findLetters(conversationId, pageInfo.getOffset(), pageInfo.getLimit());

        // 封装页面数据
        List<Map<String, Object>> letters = new ArrayList<>();
        if (!CommonUtil.isEmtpy(letterList)) {
            for (Message message : letterList) {
                Map<String, Object> map = new HashMap<>(10);
                map.put("letter", message);
                map.put("fromUser", userService.findUserById(message.getFromId()));
                letters.add(map);
            }
        }

        model.addAttribute("letters", letters);

        // 获取私信目标
        model.addAttribute("target", getLetterTarget(conversationId));

        // 将所有未读消息设置为已读状态
        List<Integer> letterIds = getLetterIds(letterList);
        if (!CommonUtil.isEmtpy(letterIds)) {
            messageService.readMessage(letterIds);
        }

        return "/site/letter-detail";

    }

    /**
     * 得到集合私信列表中未读消息的id
     *
     * @param letterList
     * @return
     */
    private List<Integer> getLetterIds(List<Message> letterList) {
        List<Integer> ids = new ArrayList<>();
        if (!CommonUtil.isEmtpy(letterList)) {
            for (Message message : letterList) {
                // 以接收者身份读取未读消息
                if (message.getToId() == userThreadLocalHolder.getCache().getId() && message.getStatus() == 0) {
                    ids.add(message.getId());
                }
            }
        }
        return ids;
    }

    /**
     * 根据会话id获取私信接收人信息
     *
     * @param conversationId
     * @return
     */
    private User getLetterTarget(String conversationId) {
        String[] ids = conversationId.split("_");
        int d0 = Integer.parseInt(ids[0]);
        int d1 = Integer.parseInt(ids[1]);

        if (userThreadLocalHolder.getCache().getId() == d0) {
            return userService.findUserById(d1);
        } else {
            return userService.findUserById(d0);
        }
    }

    @RequestMapping(path = "/letter/send", method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired
    @ApiOperation(value = "发送私信")
    public String sendLetter(String toName, String content) {
        User target = userService.findUserByUserName(toName);
        if (CommonUtil.isEmtpy(target)) {
            return CommonUtil.getJsonString(1, "目标用户不存在");
        }

        // 构造私信消息对象
        Message message = new Message();
        message.setFromId(userThreadLocalHolder.getCache().getId());
        message.setToId(target.getId());
        // 会话id生成规则：小的用户id在下划线前，大的用户id在下划线后
        if (message.getFromId() < message.getToId()) {
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        } else {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        }
        message.setContent(content);
        message.setCreateTime(new Date());

        // 执行插入操作
        messageService.addMessage(message);

        return CommonUtil.getJsonString(0);

    }

    @RequestMapping(path = "/notice/list", method = RequestMethod.GET)
    @LoginRequired
    public String getNoticeList(Model model) {
        User user = userThreadLocalHolder.getCache();

        // 查询评论类通知
        Message latestCommentNotice = messageService.findLatestNotice(user.getId(), MessageConstant.TOPIC_COMMENT);
        Map<String, Object> latestCommentNoticeVo = getNotice(latestCommentNotice, user,MessageConstant.TOPIC_COMMENT);
        if (!CommonUtil.isEmtpy(latestCommentNoticeVo)) {
            model.addAttribute("commentNotice", latestCommentNoticeVo);
        }

        // 查询点赞类的通知
        Message latestLikeNotice = messageService.findLatestNotice(user.getId(), MessageConstant.TOPIC_LIKE);
        Map<String, Object> latestLikeNoticeVo = getNotice(latestLikeNotice, user,MessageConstant.TOPIC_LIKE);
        if (!CommonUtil.isEmtpy(latestLikeNoticeVo)) {
            model.addAttribute("likeNotice", latestLikeNoticeVo);
        }

        // 查询关注类的通知
        Message latestFollowNotice = messageService.findLatestNotice(user.getId(), MessageConstant.TOPIC_FOLLOW);
        Map<String, Object> latestFollowNoticeVo = getNotice(latestFollowNotice, user,MessageConstant.TOPIC_FOLLOW);
        if (!CommonUtil.isEmtpy(latestFollowNoticeVo)) {
            model.addAttribute("followNotice", latestFollowNoticeVo);
        }

        // 查询未读消息数量
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);

        return "/site/notice";
    }

    /**
     * 封装系统通知
     * @param notice
     * @param user
     * @param topic
     * @return
     */
    private Map<String, Object> getNotice(Message notice, User user,String topic) {
        Map<String, Object> map = new HashMap<>(10);
        if (!CommonUtil.isEmtpy(notice)) {
            map.put("message", notice);
            String content = HtmlUtils.htmlUnescape(notice.getContent());
            HashMap data = JSONObject.parseObject(content, HashMap.class);
            map.put("user", userService.findUserById((Integer) data.get("userId")));
            map.put("entityType", data.get("entityType"));
            map.put("entityId", data.get("entityId"));

            int count = messageService.findNoticeCount(user.getId(), topic);
            map.put("count", count);
            int unread = messageService.findNoticeUnreadCount(user.getId(), topic);
            map.put("unread", unread);
        }
        return map;
    }

    @RequestMapping(path = "/notice/detail/{topic}", method = RequestMethod.GET)
    @LoginRequired
    public String getNoticeDetail(Model model, PageInfo pageInfo, @PathVariable("topic") String topic) {
        // 获取当前用户信息
        User user = userThreadLocalHolder.getCache();

        // 设置分页信息
        pageInfo.setLimit(5);
        pageInfo.setPath("/message/notice/detail/" + topic);
        pageInfo.setRows(messageService.findNoticeCount(user.getId(), topic));

        // 查询通知消息列表
        List<Message> noticeList = messageService.findNoticeList(user.getId(), topic, pageInfo.getOffset(), pageInfo.getLimit());
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        if (!CommonUtil.isEmtpy(noticeList)) {
            for (Message notice : noticeList) {
                Map<String, Object> map = new HashMap<>(10);
                // 通知
                map.put("notice", notice);

                // 内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                JSONObject data = JSONObject.parseObject(content);
                map.put("user", userService.findUserById((Integer) data.get("userId")));
                map.put("entityType", data.get("entityType"));
                map.put("entityId", data.get("entityId"));
                map.put("postId", data.get("postId"));

                // 通知作者
                map.put("fromUser", userService.findUserById(notice.getFromId()));
                noticeVoList.add(map);
            }
        }

        model.addAttribute("notices", noticeVoList);

        //设置已读
        List<Integer> ids = getLetterIds(noticeList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        return "/site/notice-detail";
    }
}
