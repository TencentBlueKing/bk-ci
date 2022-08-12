package com.tencent.devops.turbo.job

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.Gray
import com.tencent.devops.common.util.constants.EXCHANGE_TURBO_REPORT
import com.tencent.devops.common.util.constants.ROUTE_TURBO_REPORT_CREATE
import com.tencent.devops.common.web.mq.CORE_RABBIT_TEMPLATE_NAME
import com.tencent.devops.turbo.dto.TurboRecordCreateDto
import com.tencent.devops.turbo.sdk.TBSSdkApi
import com.tencent.devops.turbo.service.TurboPlanService
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

@Suppress("SpringJavaAutowiredMembersInspection", "MaxLineLength")
class TBSCreateDataJob @Autowired constructor(
    @Qualifier(CORE_RABBIT_TEMPLATE_NAME)
    private val rabbitTemplate: RabbitTemplate,
    private val gray: Gray,
    private val turboPlanService: TurboPlanService,
    private val redisOperation: RedisOperation
) : Job {

    companion object {
        private val logger = LoggerFactory.getLogger(TBSCreateDataJob::class.java)
    }

    override fun execute(context: JobExecutionContext) {
        try {
            val jobParam = context.jobDetail.jobDataMap
            if (!jobParam.containsKey("engineCode")) {
                logger.info("model code is null!")
                return
            }
            val engineCode = jobParam["engineCode"] as String
            logger.info("[create turbo job|$engineCode] create job start")
            fetchNewTurboRecord(engineCode)
        } catch (e: Exception) {
            logger.info("execute create job fail! message: ${e.message}")
        }
    }

    private fun fetchNewTurboRecord(engineCode: String) {
        val compileTimeLeft = System.currentTimeMillis() / 1000 - 90
        val newTurboRecordList = TBSSdkApi.queryTurboRecordInfo(
            engineCode = engineCode,
            queryParam = mapOf(
                "create_time_left" to compileTimeLeft,
                "order" to "project_id,create_time"
            )
        )
        for (turboRecord in newTurboRecordList) {
            // 判断，如果灰度和节点配置相同，则下发
            // todo distask额外逻辑，需要截取下划线之前的子串
            val turboPlanId = (turboRecord["project_id"] as String?)?.substringBefore("_")
            if (turboPlanId.isNullOrBlank()) {
                logger.info("[create turbo job|$engineCode|$turboPlanId] no respective turbo plan id")
                continue
            }
            val turboPlanEntity = turboPlanService.findTurboPlanById(turboPlanId)
            if (null == turboPlanEntity) {
                logger.info("[create turbo job|$engineCode|$turboPlanId] no turbo plan info found with id $turboPlanId")
                continue
            }
            logger.info("[create turbo job|$engineCode|$turboPlanId] ready to send message")
            if (gray.isGrayMatchProject(turboPlanEntity.projectId, redisOperation)) {
                rabbitTemplate.convertAndSend(EXCHANGE_TURBO_REPORT, ROUTE_TURBO_REPORT_CREATE, TurboRecordCreateDto(engineCode, turboRecord))
            }
        }
    }
}
