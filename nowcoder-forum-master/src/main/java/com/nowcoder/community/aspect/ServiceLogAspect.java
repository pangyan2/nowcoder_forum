package com.nowcoder.community.aspect;

import com.nowcoder.community.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * 服务层日志切面
 *
 * @author Alex
 * @version 1.0
 * @date 2022/2/14 12:11
 */
@Component
@Aspect
@Slf4j
public class ServiceLogAspect {

    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")
    public void pointCut() {

    }

    @Before("pointCut()")
    public void before(JoinPoint joinPoint) {
        //用户IP地址[1.2.3.4]在{time},访问了com.nowcoder.community.service.*.*(..)
        //用户IP地址
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (CommonUtil.isEmtpy(requestAttributes)) {
            return;
        }
        HttpServletRequest request = requestAttributes.getRequest();
        String ip = request.getRemoteHost();

        // 获取当前时间
        String now = CommonUtil.getFormatDate(new Date());
        // 目标方法
        String target = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();

        log.info("用户{},在{},访问了{}.", ip, now, target);
    }

}
