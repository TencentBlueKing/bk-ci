package com.tencent.bk.codecc.quartz.job

import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext
import com.tencent.devops.common.web.mq.EXCHANGE_CHECK_THREAD_ALIVE
import com.tencent.devops.common.web.mq.ROUTE_CHECK_THREAD_ALIVE
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired

/**
 * 检查线程是否还在分析主机上存活的定时任务
 * cronExpression：0 0/1 * * * ?
 *
 * @date 2019/11/11
 * @version V1.0
 */
class CheckAnalyzeHostThreadScheduleTask @Autowired constructor(
        private val rabbitTemplate: RabbitTemplate
) : IScheduleTask {

    companion object {
        private val logger = LoggerFactory.getLogger(CheckAnalyzeHostThreadScheduleTask::class.java)
    }

    override fun executeTask(quartzJobContext: QuartzJobContext) {

        rabbitTemplate.convertAndSend(EXCHANGE_CHECK_THREAD_ALIVE, ROUTE_CHECK_THREAD_ALIVE, "")
    }
}