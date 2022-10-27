package com.tencent.devops.stream.trigger.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.BuildReviewType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildReviewBroadCastEvent
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.trigger.actions.EventActionFactory
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting
import com.tencent.devops.stream.trigger.actions.data.context.BuildFinishStageData
import com.tencent.devops.stream.trigger.listener.components.SendCommitCheck
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.tencent.devops.stream.constant.MQ as StreamMQ

@Suppress("ALL")
@Service
class StreamBuildReviewListener @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val actionFactory: EventActionFactory,
    private val streamGitConfig: StreamGitConfig,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val sendCommitCheck: SendCommitCheck
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamBuildReviewListener::class.java)
    }

    @RabbitListener(
        bindings = [
            (
                QueueBinding(
                    value = Queue(value = StreamMQ.QUEUE_PIPELINE_BUILD_REVIEW_STREAM, durable = "true"),
                    exchange = Exchange(
                        value = MQ.EXCHANGE_PIPELINE_BUILD_REVIEW_FANOUT,
                        durable = "true",
                        delayed = "true",
                        type = ExchangeTypes.FANOUT
                    )
                )
                )
        ]
    )
    fun buildReviewListener(buildReviewEvent: PipelineBuildReviewBroadCastEvent) {
        try {
            // stream目前没有人工审核插件
            if (buildReviewEvent.reviewType == BuildReviewType.TASK_REVIEW) {
                return
            }

            logger.info("buildReviewListener buildReviewEvent: $buildReviewEvent")
            val buildEvent = gitRequestEventBuildDao.getByBuildId(dslContext, buildReviewEvent.buildId) ?: return
            val requestEvent = gitRequestEventDao.getWithEvent(dslContext, buildEvent.eventId) ?: return
            val pipelineId = buildEvent.pipelineId

            val pipeline = gitPipelineResourceDao.getPipelinesInIds(
                dslContext = dslContext,
                gitProjectId = null,
                pipelineIds = listOf(pipelineId)
            ).getOrNull(0)?.let {
                StreamTriggerPipeline(
                    gitProjectId = it.gitProjectId.toString(),
                    pipelineId = it.pipelineId,
                    filePath = it.filePath,
                    displayName = it.displayName,
                    enabled = it.enabled,
                    creator = it.creator
                )
            } ?: throw OperationException("stream pipeline not exist")

            // 改为利用pipeline信息反查projectId 保证流水线和项目是绑定的
            val setting = streamBasicSettingDao.getSetting(dslContext, pipeline.gitProjectId.toLong())?.let {
                StreamTriggerSetting(it)
            } ?: throw OperationException("stream all projectCode not exist")

            // 加载action，并填充上下文，手动和定时触发需要自己的事件
            val action = when (requestEvent.objectKind) {
                StreamObjectKind.MANUAL.value -> actionFactory.loadManualAction(
                    setting = setting,
                    event = objectMapper.readValue(requestEvent.event)
                )
                StreamObjectKind.SCHEDULE.value -> actionFactory.loadScheduleAction(
                    setting = setting,
                    event = objectMapper.readValue(requestEvent.event)
                )
                StreamObjectKind.OPENAPI.value -> {
                    // openApi可以手工触发也可以模拟事件触发,所以event有两种结构
                    try {
                        actionFactory.loadManualAction(
                            setting = setting,
                            event = objectMapper.readValue(requestEvent.event)
                        )
                    } catch (ignore: Exception) {
                        actionFactory.load(
                            actionFactory.loadEvent(
                                requestEvent.event,
                                streamGitConfig.getScmType(),
                                requestEvent.objectKind
                            )
                        )
                    }
                }
                else -> actionFactory.load(
                    actionFactory.loadEvent(
                        requestEvent.event,
                        streamGitConfig.getScmType(),
                        requestEvent.objectKind
                    )
                )
            } ?: throw OperationException("stream not support action ${requestEvent.event}")

            action.data.setting = setting
            action.data.context.pipeline = pipeline
            action.data.context.finishData = BuildFinishStageData(
                streamBuildId = buildEvent.id,
                eventId = buildEvent.eventId,
                version = buildEvent.version,
                normalizedYaml = buildEvent.normalizedYaml,
                projectId = buildReviewEvent.projectId,
                pipelineId = buildReviewEvent.pipelineId,
                userId = buildReviewEvent.userId,
                buildId = buildReviewEvent.buildId,
                status = buildReviewEvent.status,
                startTime = buildEvent.createTime.timestampmilli(),
                stageId = buildReviewEvent.stageId,
                reviewType = buildReviewEvent.reviewType
            )
            action.data.context.requestEventId = requestEvent.id

            when (buildReviewEvent.reviewType) {
                BuildReviewType.STAGE_REVIEW, BuildReviewType.QUALITY_CHECK_IN, BuildReviewType.QUALITY_CHECK_OUT -> {
                    // 推送构建消息
                    sendCommitCheck.sendCommitCheck(action)
                }
                // 这里先这么写，未来如果这么枚举扩展代码编译时可以第一时间感知，防止漏过事件
                BuildReviewType.TASK_REVIEW -> {
                    logger.warn(
                        "StreamBuildReviewListener|buildReviewListener" +
                            "|event not match|${buildReviewEvent.reviewType}"
                    )
                }
            }
        } catch (e: Exception) {
            logger.warn("StreamBuildReviewListener|buildReviewListener|${buildReviewEvent.buildId}|error|${e.message}")
        }
    }
}
