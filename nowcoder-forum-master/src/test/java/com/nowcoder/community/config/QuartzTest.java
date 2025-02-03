package com.nowcoder.community.config;

import com.nowcoder.community.CommunityApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Alex
 * @version 1.0
 * @company xxx
 * @copyright (c)  xxxInc. All rights reserved.
 * @date 2022/2/21 17:19
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
@Slf4j
public class QuartzTest {

    @Autowired
    private Scheduler scheduler;

    /**
     * 删除Job/Trigger
     */
    @Test
    public void testDeleteJob(){
        try {
            boolean result = scheduler.deleteJob(new JobKey("SimpleJob", "SimpleJobGroup"));
            System.out.println(result);
        } catch (SchedulerException e) {
            log.error("删除任务失败:{}",e);
        }
    }
}
