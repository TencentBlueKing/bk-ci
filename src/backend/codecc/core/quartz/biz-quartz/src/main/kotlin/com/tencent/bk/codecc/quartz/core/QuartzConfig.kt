package com.tencent.bk.codecc.quartz.core

import com.tencent.bk.codecc.quartz.service.JobManageService
import com.tencent.bk.codecc.quartz.service.ShardingRouterService
import org.quartz.Scheduler
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.quartz.AdaptableJobFactory
import org.springframework.scheduling.quartz.SchedulerFactoryBean

@Configuration
open class QuartzConfig {

    @Bean
    open fun jobFactory() = CustomJobFactory()

    @Bean
    open fun schedulerFactoryBean(@Autowired jobFactory: AdaptableJobFactory): SchedulerFactoryBean {
        val schedulerFactoryBean = SchedulerFactoryBean()
//        schedulerFactoryBean.setDataSource(null)
        schedulerFactoryBean.setJobFactory(jobFactory)
        val res = ClassPathResource("quartz.properties")
        schedulerFactoryBean.setConfigLocation(res)
        return schedulerFactoryBean
    }

    @Bean
    open fun shardingListener(
        @Autowired redisTemplate: RedisTemplate<String, String>,
        @Autowired jobManageService: JobManageService
    ) =
        ShardingListener(redisTemplate, jobManageService)

    @Bean(destroyMethod = "stop")
    open fun schedulerManager(
        @Autowired scheduler: Scheduler,
        @Autowired applicationContext: ApplicationContext,
        @Autowired shardingRouterService: ShardingRouterService,
        @Autowired jobManageService: JobManageService,
        @Autowired shardingListener: ShardingListener,
        @Autowired redisTemplate: RedisTemplate<String, String>,
        @Autowired rabbitTemplate: RabbitTemplate
    ) =
        CustomSchedulerManager(
            applicationContext, shardingRouterService,
            jobManageService, scheduler, shardingListener, redisTemplate,
            rabbitTemplate
        )
}