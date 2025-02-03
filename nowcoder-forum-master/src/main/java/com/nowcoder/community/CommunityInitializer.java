package com.nowcoder.community;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * 程序打包部署到tomcat中时的tomcat调用入口类
 * @author Alex
 * @version 1.0
 * @company xxx
 * @copyright (c)  xxxInc. All rights reserved.
 * @date 2022/3/3 17:41
 */
public class CommunityInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        // 当部署到服务器tomcat中时，需要为tomcat指定程序启动类
        return builder.sources(CommunityApplication.class);
    }
}
