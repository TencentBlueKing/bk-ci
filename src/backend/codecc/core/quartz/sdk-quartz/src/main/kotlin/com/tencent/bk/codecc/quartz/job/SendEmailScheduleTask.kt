package com.tencent.bk.codecc.quartz.job

import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext
import com.tencent.bk.codecc.task.enums.EmailType
import com.tencent.bk.codecc.task.pojo.EmailNotifyModel
import com.tencent.devops.common.web.mq.EXCHANGE_CODECC_GENERAL_NOTIFY
import com.tencent.devops.common.web.mq.ROUTE_CODECC_EMAIL_NOTIFY
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired

class SendEmailScheduleTask @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate
) : IScheduleTask {

    companion object {
        private val logger = LoggerFactory.getLogger(SendEmailScheduleTask::class.java)
    }

    override fun executeTask(quartzJobContext: QuartzJobContext) {
        val jobCustomParam = quartzJobContext.jobCustomParam
        if (null == jobCustomParam) {
            logger.info("job custom param is null!")
            return
        }

        val taskId = jobCustomParam["taskId"].toString()
        val emailNotifyModel = EmailNotifyModel(
            taskId.toLong(),
            null,
            EmailType.DAILY
        )
        rabbitTemplate.convertAndSend(EXCHANGE_CODECC_GENERAL_NOTIFY, ROUTE_CODECC_EMAIL_NOTIFY, emailNotifyModel)
    }
}