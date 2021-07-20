package com.tencent.bk.codecc.quartz.job

import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext
import com.tencent.devops.common.web.mq.EXCHANGE_REFRESH_CHECKERSET_USAGE
import com.tencent.devops.common.web.mq.ROUTE_REFRESH_CHECKERSET_USAGE
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired

/**
 * 刷新规则集的使用量，每10分钟刷新一次
 * cronExpression：0 0/10 * * * ? *
 *
 * @date 2019/11/11
 * @version V1.0
 */
class RefreshCheckerSetUsageTask @Autowired constructor(
        private val rabbitTemplate: RabbitTemplate
) : IScheduleTask {

    companion object {
        private val logger = LoggerFactory.getLogger(RefreshCheckerSetUsageTask::class.java)
    }

    override fun executeTask(quartzJobContext: QuartzJobContext) {
        logger.info("refresh checker set usage.")
        rabbitTemplate.convertAndSend(EXCHANGE_REFRESH_CHECKERSET_USAGE, ROUTE_REFRESH_CHECKERSET_USAGE, "")
    }
}