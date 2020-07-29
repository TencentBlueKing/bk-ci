package com.tencent.bk.codecc.quartz.job

import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext
import com.tencent.bk.codecc.task.pojo.TriggerPipelineModel
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.mq.EXCHANGE_GONGFENG_CODECC_SCAN
import com.tencent.devops.common.web.mq.ROUTE_GONGFENG_TRIGGER_PIPELINE
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired

class TriggerPipelineScheduleTask @Autowired constructor(
        private val client: Client,
        private val rabbitTemplate: RabbitTemplate
) : IScheduleTask {

    companion object {
        private val logger = LoggerFactory.getLogger(CreateTaskScheduleTask::class.java)
    }

    override fun executeTask(quartzJobContext: QuartzJobContext) {
        val jobCustomParam = quartzJobContext.jobCustomParam
        if (null == jobCustomParam) {
            logger.info("job custom param is null!")
            return
        }
        val projectId = jobCustomParam["projectId"] as String
        val pipelineId = jobCustomParam["pipelineId"] as String
        val taskId = jobCustomParam["taskId"].toString()
        val gongfengId = jobCustomParam["gongfengId"] as Int
        val userName = jobCustomParam["owner"] as String
        val triggerPipelineModel = TriggerPipelineModel(
                projectId = projectId,
                pipelineId = pipelineId,
                taskId = taskId.toLong(),
                gongfengId = gongfengId,
                owner = userName
        )
        rabbitTemplate.convertAndSend(EXCHANGE_GONGFENG_CODECC_SCAN, ROUTE_GONGFENG_TRIGGER_PIPELINE, triggerPipelineModel)
    }
}