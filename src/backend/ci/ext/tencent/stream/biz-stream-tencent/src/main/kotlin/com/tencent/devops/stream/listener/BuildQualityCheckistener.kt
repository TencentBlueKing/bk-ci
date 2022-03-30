package com.tencent.devops.stream.listener

import com.tencent.devops.stream.constant.MQ as StreamMQ
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQualityCheckBroadCastEvent
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.listener.components.SendQualityMrComment
import com.tencent.devops.stream.v2.dao.StreamBasicSettingDao
import com.tencent.devops.stream.v2.service.StreamPipelineService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BuildQualityCheckistener @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val streamPipelineService: StreamPipelineService,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val sendQualityMrComment: SendQualityMrComment
) {

    companion object {
        private val logger = LoggerFactory.getLogger(BuildQualityCheckistener::class.java)
    }

    @RabbitListener(
        bindings = [(QueueBinding(
            value = Queue(value = StreamMQ.QUEUE_PIPELINE_BUILD_QUALITY_CHECK_STREAM, durable = "true"),
            exchange = Exchange(
                value = MQ.EXCHANGE_PIPELINE_BUILD_QUALITY_CHECK_FANOUT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.FANOUT
            )
        ))]
    )
    fun buildQualityCheckListener(buildQualityEvent: PipelineBuildQualityCheckBroadCastEvent) {
        try {
            logger.info("buildQualityCheckListener buildReviewEvent: $buildQualityEvent")
            val streamBuild = gitRequestEventBuildDao.getByBuildId(dslContext, buildQualityEvent.buildId)
            val buildEvent = streamBuild?.let {
                StreamBuildEvent(
                    id = it.id,
                    eventId = it.eventId,
                    pipelineId = it.pipelineId,
                    version = it.version,
                    normalizedYaml = it.normalizedYaml
                )
            } ?: return
            val requestEvent = gitRequestEventDao.getWithEvent(dslContext, buildEvent.eventId) ?: return
            val pipelineId = buildEvent.pipelineId

            val pipeline = streamPipelineService.getPipelineById(pipelineId)
                ?: throw OperationException("git ci pipeline not exist")
            val v2GitSetting = streamBasicSettingDao.getSetting(dslContext, pipeline.gitProjectId)
                ?: throw OperationException("git ci all projectCode not exist")

            sendQualityMrComment.sendMrComment(
                StreamBuildListenerContextV2(
                    buildEvent = BuildEvent(
                        projectId = buildQualityEvent.projectId,
                        pipelineId = buildQualityEvent.pipelineId,
                        userId = buildQualityEvent.userId,
                        buildId = buildQualityEvent.buildId,
                        status = buildQualityEvent.status,
                        startTime = streamBuild.createTime.timestampmilli()
                    ),
                    requestEvent = requestEvent,
                    streamBuildEvent = buildEvent,
                    pipeline = pipeline,
                    streamSetting = v2GitSetting
                ),
                ruleIds = buildQualityEvent.ruleIds
                    ?: throw RuntimeException("${buildQualityEvent.buildId} have none ruleids")
            )
        } catch (e: Exception) {
            logger.warn("buildQualityCheckListener ${buildQualityEvent.buildId} error: ${e.message}")
        }
    }
}
