package com.tencent.devops.stream.listener

import com.tencent.devops.stream.constant.MQ as StreamMQ
import com.tencent.devops.common.api.enums.BuildReviewType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildReviewBroadCastEvent
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.listener.components.SendCommitCheck
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
class BuildReviewListener @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val streamPipelineService: StreamPipelineService,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val sendCommitCheck: SendCommitCheck
) {

    companion object {
        private val logger = LoggerFactory.getLogger(BuildReviewListener::class.java)
    }

    @RabbitListener(
        bindings = [(QueueBinding(
            value = Queue(value = StreamMQ.QUEUE_PIPELINE_BUILD_REVIEW_STREAM, durable = "true"),
            exchange = Exchange(
                value = MQ.EXCHANGE_PIPELINE_BUILD_REVIEW_FANOUT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.FANOUT
            )
        ))]
    )
    fun buildReviewListener(buildReviewEvent: PipelineBuildReviewBroadCastEvent) {
        try {
            // stream目前没有人工审核插件
            if (buildReviewEvent.reviewType == BuildReviewType.TASK_REVIEW) {
                return
            }

            logger.info("buildReviewListener buildReviewEvent: $buildReviewEvent")
            val streamBuild = gitRequestEventBuildDao.getByBuildId(dslContext, buildReviewEvent.buildId)
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

            when (buildReviewEvent.reviewType) {
                BuildReviewType.STAGE_REVIEW, BuildReviewType.QUALITY_CHECK_IN, BuildReviewType.QUALITY_CHECK_OUT -> {
                    // 推送构建消息
                    sendCommitCheck.sendCommitCheck(
                        StreamBuildStageListenerContextV2(
                            buildEvent = BuildEvent(
                                projectId = buildReviewEvent.projectId,
                                pipelineId = buildReviewEvent.pipelineId,
                                userId = buildReviewEvent.userId,
                                buildId = buildReviewEvent.buildId,
                                status = buildReviewEvent.status,
                                startTime = streamBuild.createTime.timestampmilli(),
                                stageId = buildReviewEvent.stageId
                            ),
                            requestEvent = requestEvent,
                            streamBuildEvent = buildEvent,
                            pipeline = pipeline,
                            streamSetting = v2GitSetting,
                            reviewType = buildReviewEvent.reviewType
                        )
                    )
                }
                // 这里先这么写，未来如果这么枚举扩展代码编译时可以第一时间感知，防止漏过事件
                BuildReviewType.TASK_REVIEW -> {
                    logger.warn("buildReviewListener event not match: ${buildReviewEvent.reviewType}")
                }
            }
        } catch (e: Exception) {
            logger.warn("buildReviewListener ${buildReviewEvent.buildId} error: ${e.message}")
        }
    }
}
