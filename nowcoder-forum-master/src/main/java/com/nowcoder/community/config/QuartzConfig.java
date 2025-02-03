package com.nowcoder.community.config;

import com.nowcoder.community.config.job.DiscussPostScoreRefreshJob;
import com.nowcoder.community.config.job.SimpleJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

/**
 * quartz配置
 *      配置 -> 数据库 -> 调用
 * @author Alex
 * @version 1.0
 * @company xxx
 * @copyright (c)  xxxInc. All rights reserved.
 * @date 2022/2/21 16:11
 */
@Configuration
public class QuartzConfig {

    /**
     * FactoryBean简化bean的实例化过程:
     *      spring通过FactoryBean封装Bean的实例化过程。
     *      FactoryBean装配到spring容器中。
     *      将FactoryBean注入给其他bean。
     *      该bean得到的是FactoryBean所管理的对象实例.
     *
     */

    /**
     * 配置JobDetail:初始化JobDetailFactoryBean，实现简化JobDetail实例化过程
     * @return
     */
    @Bean
    public JobDetailFactoryBean discussPostScoreRefreshJobDetailFactoryBean(){
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(DiscussPostScoreRefreshJob.class);
        factoryBean.setName("DiscussPostScoreRefreshJob");
        factoryBean.setGroup("DiscussPostScoreRefreshJobGroup");
        // 任务是否长久保存
        factoryBean.setDurability(true);
        // 任务是否可恢复
        factoryBean.setRequestsRecovery(true);
        return factoryBean;
    }

    /**
     * 配置Trigger(SimpleTriggerFactoryBean,CronTriggerFactoryBean)
     *      SimpleTriggerFactoryBean：简单触发器工厂bean 触发方式：每隔多少时间做一次任务
     *      CronTriggerFactoryBean:复杂~,使用cron表达式执行定时任务：在某个特定时间点执行任务
     * @param discussPostScoreRefreshJobDetailFactoryBean
     * @return
     */
    @Bean
    public SimpleTriggerFactoryBean getDiscussPostScoreRefreshTriggerFactoryBean(JobDetail discussPostScoreRefreshJobDetailFactoryBean){
        SimpleTriggerFactoryBean discussPostScoreRefreshTriggerFactoryBean = new SimpleTriggerFactoryBean();
        discussPostScoreRefreshTriggerFactoryBean.setJobDetail(discussPostScoreRefreshJobDetailFactoryBean);
        discussPostScoreRefreshTriggerFactoryBean.setName("DiscussPostScoreRefreshTrigger");
        // 执行频率
        discussPostScoreRefreshTriggerFactoryBean.setRepeatInterval(1000 * 60 * 5);
        discussPostScoreRefreshTriggerFactoryBean.setGroup("DiscussPostScoreRefreshTriggerGroup");
        // JobDataMap存储job的状态
        discussPostScoreRefreshTriggerFactoryBean.setJobDataMap(new JobDataMap());

        return discussPostScoreRefreshTriggerFactoryBean;
    }
}
