package com.nowcoder.community.config;

import com.nowcoder.community.constant.SystemConstant;
import com.nowcoder.community.util.CommonUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Spring Security相关配置
 * @author Alex
 * @version 1.0
 * @date 2022/2/19 15:58
 */
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    public static final String XML_HTTP_REQUEST = "XMLHttpRequest";

    @Override
    public void configure(WebSecurity web) throws Exception {
        // 忽略对静态资源的拦截 /resources/下静态资源都是直接访问，不用拦截
        web.ignoring().antMatchers("/resources/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 授权
        http.authorizeRequests()
                // 登录之后，如果含有有效的用户身份，这以下权限登录之后军具有
                .antMatchers(
                "/user/settings",
                        "/user/upload",
                        "/user/updatePassword",
                        "/user/mypost",
                        "/user/myreply",
                        "/discuss/add/",
                        "/comment/add/**",
                        "/message/**",
                        "/like/**",
                        "/follow/**"
        ).hasAnyAuthority(SystemConstant.AUTHORITY_ADMIN,
                SystemConstant.AUTHORITY_USER,
                SystemConstant.AUTHORITY_MODERATOR
        ).antMatchers(
                "/disucss/top",
                "/discuss/fine"
        ).hasAnyAuthority(
                SystemConstant.AUTHORITY_MODERATOR
        ).antMatchers(
                "/discuss/delete",
                "/data/**",
                "/*.html",
                "/database"
        ).hasAnyAuthority(
                SystemConstant.AUTHORITY_ADMIN
        ).anyRequest().permitAll()
        // 默认引入spring security之后，csrf防御是默认开启的,使用此配置禁用csrf攻击防御
         .and().csrf().disable();

        // 权限不够时的异常处理
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    // 未登录时的处理
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                String xRequestWith = request.getHeader("x-requested-with");
                if (XML_HTTP_REQUEST.equals(xRequestWith)){
                    response.setContentType("application/plain;charset=utf-8");
                    PrintWriter writer = response.getWriter();
                    writer.write(CommonUtil.getJsonString(403,"你还没有登录哦！"));
                }else{
                    response.sendRedirect(request.getContextPath()+"/user/loginPage");
                }
            }
        }).accessDeniedHandler(new AccessDeniedHandler() {
            // 权限不足的处理
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestWith = request.getHeader("x-requested-with");
                        if (XML_HTTP_REQUEST.equals(xRequestWith)){
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommonUtil.getJsonString(403,"你没有访问此功能的权限"));
                        }else{
                            response.sendRedirect(request.getContextPath()+"/denied");
                        }
                    }
                });

        // security底层默认会拦截/logout请求，进行退出处理，所以覆盖默认的逻辑，才能执行我们自己的推出代码
        // logout拦截路径修改为其他路径,这样就绕过了security的退出拦截
        http.logout().logoutUrl("/security-logout");
    }

    /**
     * 此配置允许不规范 URL 访问：解决头像显示url不规范问题
     * @return
     */
    @Bean
    public HttpFirewall httpFirewall() {
        return new DefaultHttpFirewall();
    }
}
