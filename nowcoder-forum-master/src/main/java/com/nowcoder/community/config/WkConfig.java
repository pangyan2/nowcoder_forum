package com.nowcoder.community.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

/**
 * wkhtmltopdf配置
 * @author Alex
 * @version 1.0
 * @company xxx
 * @copyright (c)  xxxInc. All rights reserved.
 * @date 2022/2/22 11:59
 */
@Configuration
@Slf4j
public class WkConfig {

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @PostConstruct
    public void init(){
        // 创建wk图片目录
        File file = new File(wkImageStorage);

        if (!file.exists()) {
            file.mkdirs();
            log.info("创建wk图片目录:"+wkImageStorage);
        }
    }
}
