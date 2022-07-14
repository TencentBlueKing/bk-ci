package com.tencent.devops.turbo.job

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.Gray
import com.tencent.devops.common.util.constants.EXCHANGE_TURBO_REPORT
import com.tencent.devops.common.util.constants.ROUTE_TURBO_REPORT_UPDATE
import com.tencent.devops.common.web.mq.CORE_RABBIT_TEMPLATE_NAME
import com.tencent.devops.turbo.dto.TurboRecordUpdateDto
import com.tencent.devops.turbo.enums.EnumDistccTaskStatus
import com.tencent.devops.turbo.service.TurboRecordService
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

@Suppress("SpringJavaAutowiredMembersInspection","NestedBlockDepth","MaxLineLength")
class TBSUpdateDataJob @Autowired constructor(
    private val turboRecordService: TurboRecordService,
    @Qualifier(CORE_RABBIT_TEMPLATE_NAME)
    private val rabbitTemplate: RabbitTemplate,
    private val gray: Gray,
    private val redisOperation: RedisOperation
) : Job {

    companion object {
        private val logger = LoggerFactory.getLogger(TBSUpdateDataJob::class.java)
    }

    override fun execute(context: JobExecutionContext) {
        try {
            val jobParam = context.jobDetail.jobDataMap
            if (!jobParam.containsKey("engineCode")) {
                logger.info("model code is null!")
                return
            }
            val engineCode = jobParam["engineCode"] as String
            logger.info("[update turbo job|$engineCode] update job start")
            /**
             * 这里查询分为几个场景
             * 1.结合流水线创建的编译加速记录，一开始没有保存tbsRecordId，查询出来需要用buildId进行关联
             * 2.构建机创建的记录，由于在create的job中有保存tbsRecordId，则可以直接用tbsRecordId关联
             * 总结：tbsRecordId和buildId是同一个维度的指标，如果有tbsRecordId则用tbsRecordId关联，如果没有则用buildId关联
             */
            val unFinishedRecordList =
                turboRecordService.findByEngineCodeAndStatusNotIn(engineCode, setOf(EnumDistccTaskStatus.FINISH.getTBSStatus(), EnumDistccTaskStatus.FAILED.getTBSStatus()))
            logger.info("unfinished record list size: ${unFinishedRecordList.size}")
            if (!unFinishedRecordList.isNullOrEmpty()) {
                for (turboRecordEntity in unFinishedRecordList) {
                    if (gray.isGrayMatchProject(turboRecordEntity.projectId, redisOperation)) {
                        val turboRecordUpdateDto = TurboRecordUpdateDto(
                            engineCode = engineCode,
                            tbsTurboRecordId = turboRecordEntity.tbsRecordId,
                            buildId = turboRecordEntity.buildId,
                            turboPlanId = turboRecordEntity.turboPlanId
                        )
                        logger.info("[update turbo job|$engineCode|${turboRecordEntity.turboPlanId}] ready to send message")
                        rabbitTemplate.convertAndSend(EXCHANGE_TURBO_REPORT, ROUTE_TURBO_REPORT_UPDATE, turboRecordUpdateDto)
                    }
                }
            }
        } catch (e: Exception) {
            logger.info("execute update data job fail! message: ${e.message}")
        }
    }
}
