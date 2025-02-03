package com.nowcoder.community.config;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author Alex
 * @version 1.0
 * @company xxx
 * @copyright (c)  xxxInc. All rights reserved.
 * @date 2022/2/22 11:48
 */
@Slf4j
public class WkTest {

    public static void main(String[] args) {
        String cmd = "d:/software/wkhtmltopdf/bin/wkhtmltoimage --quality 75 https://www.nowcoder.com d:/work/data/image/nowcoder.png";
        try {
            // 交给操作系统执行，并发异步
            Runtime.getRuntime().exec(cmd);
            log.info("file generate successful...");
        } catch (IOException e) {
            log.error(e.getMessage());
        }

    }
}
