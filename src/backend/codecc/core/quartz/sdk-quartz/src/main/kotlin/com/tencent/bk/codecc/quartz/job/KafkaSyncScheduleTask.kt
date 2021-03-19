package com.tencent.bk.codecc.quartz.job

import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext
import com.tencent.devops.common.web.mq.EXCHANGE_KAFKA_DATA_PLATFORM
import com.tencent.devops.common.web.mq.ROUTE_KAFKA_DATA_TRIGGER_TASK
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired

class KafkaSyncScheduleTask @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate
) : IScheduleTask{

    companion object {
        private val logger = LoggerFactory.getLogger(KafkaSyncScheduleTask::class.java)
    }

    override fun executeTask(quartzJobContext: QuartzJobContext) {
        rabbitTemplate.convertAndSend(EXCHANGE_KAFKA_DATA_PLATFORM, ROUTE_KAFKA_DATA_TRIGGER_TASK, "1")
    }
}