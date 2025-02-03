package com.nowcoder.community.thread;

import com.nowcoder.community.CommunityApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 线程池测试类
 * @author Alex
 * @version 1.0
 * @company xxx
 * @copyright (c)  xxxInc. All rights reserved.
 * @date 2022/2/21 13:24
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
@Slf4j
public class ThreadPoolTest {


    // JDK普通线程池
    /**
     * 创建5各线程，反复服用线程池这五个线程
     */
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * JDK可执行定时任务的线程池
     */
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    private void sleep(long m){
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testExecutorService(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                log.info("hello service");
            }
        };

        for (int i=0;i<10;i++) {
            executorService.submit(task);
        }

        sleep(10000);
    }

    @Test
    public void testScheduledExecutorService(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                log.info("hello scheduled service");
            }
        };

        scheduledExecutorService.scheduleAtFixedRate(task,1000, 1000,TimeUnit.MILLISECONDS);

        sleep(30000);
    }

    /**
     * spring普通线程池：比jdk线程池更加灵活
     */
    @Test
    public void testThreadPoolTaskExecutor(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                log.info("hello ThreadPoolTaskExecutor");
            }
        };

        for (int i = 0; i < 10; i++) {
            taskExecutor.submit(task);
        }

        // 阻塞10s，看到多线程执行任务效果
        sleep(10000);
    }

    @Test
    public void testThreadPoolTaskScheduler(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                log.info("hello ThreadPoolTaskScheduler");
            }
        };
        Date date = new Date(System.currentTimeMillis() + 10000);
        threadPoolTaskScheduler.scheduleAtFixedRate(task,date,1000);

        sleep(30000);
    }

    /**
     * 测试 spring 定时任务线程池 简化版
     */
    @Test
    public void testThreadPoolTaskExecutorSimpleWay(){
        for (int i = 0; i < 3; i++) {
            // taskService.executeTask();
        }
    }

    /**
     * spring定时任务线程池，简化
     */
    @Test
    public void testThreadPoolTaskSchedulerSimpleWay(){
        sleep(30000);
    }

}
