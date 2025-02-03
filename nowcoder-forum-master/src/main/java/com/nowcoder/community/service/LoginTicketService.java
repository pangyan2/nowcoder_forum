package com.nowcoder.community.service;

import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 登录凭证业务层
 * @author Alex
 * @version 1.0
 * @date 2022/2/6 11:21
 */
@Service
public class LoginTicketService {


    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据用户浏览器cookie携带的ticket查询用户登录凭证
     * @param ticket
     * @return
     */
    public LoginTicket findLoginTicket(String ticket){
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket)redisTemplate.opsForValue().get(ticketKey);
    }
}
