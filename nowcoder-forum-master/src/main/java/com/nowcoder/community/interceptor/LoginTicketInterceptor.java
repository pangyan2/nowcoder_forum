package com.nowcoder.community.interceptor;

import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.LoginTicketService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommonUtil;
import com.nowcoder.community.util.CookieUtil;
import com.nowcoder.community.util.ThreadLocalHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Date;

/**
 * @author Alex
 * @version 1.0
 * @date 2022/2/19 16:51
 */
@Component
@Slf4j
public class LoginTicketInterceptor implements HandlerInterceptor {
    @Autowired
    private ThreadLocalHolder<User> userThreadLocalHolder;

    @Autowired
    private LoginTicketService loginTicketService;

    @Autowired
    private UserService userService;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求路径
        String uri = request.getRequestURI();
        log.info("当前请求路径为:{}",uri);
        // 从cookie中获取凭证
        String ticket = CookieUtil.getValue(request, "ticket");
        if(!CommonUtil.isEmtpy(ticket)){
            // 查询凭证
            LoginTicket loginTicket = loginTicketService.findLoginTicket(ticket);
            if(CommonUtil.isEmtpy(loginTicket)){
                response.sendRedirect(request.getContextPath() + "/user/loginPage");
                String remoteHost = request.getRemoteHost();
                log.info("用户{}未登录,正在转向登录页面......",remoteHost);
                return false;
            }else{
                // 检查登录凭证是否有效
                if(loginTicket.getStatus()==0&&loginTicket.getExpired().after(new Date())){
                    // 根据凭证查询用户
                    User user = userService.findUserById(loginTicket.getUserId());
                    // 在本次请求中持有用户信息
                    userThreadLocalHolder.setCache(user);
                    log.info("{}用户登录成功,当前登录时间为{}",user.getUsername(),CommonUtil.getFormatDate(new Date()));
                    // 构建用户认证的结果，并存入SecurityContext,便于security授权
                    Authentication authentication = new UsernamePasswordAuthenticationToken(user, user.getPassword(), userService.getAuthorites(user.getId()));
                    SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
                    return true;
                }
            }

        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        log.info("postHandle..............");
        User user = userThreadLocalHolder.getCache();
        if(!CommonUtil.isEmtpy(user)){
            modelAndView.addObject("loginUser",user);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        log.info("afterCompletion..............");
        // 清除用户信息
        userThreadLocalHolder.clear();
        SecurityContextHolder.clearContext();
    }
}
