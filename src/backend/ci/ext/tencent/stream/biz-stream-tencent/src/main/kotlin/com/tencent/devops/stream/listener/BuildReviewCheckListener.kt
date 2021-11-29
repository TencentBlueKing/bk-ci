package com.tencent.devops.stream.listener

import com.tencent.devops.stream.constant.MQ as StreamMQ
import com.tencent.devops.common.api.enums.BuildReviewType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildReviewCheckBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.listener.components.SendCommitCheck
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
class BuildReviewCheckListener @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val streamPipelineService: StreamPipelineService,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val sendCommitCheck: SendCommitCheck,
    private val sendQualityMrComment: SendQualityMrComment
) {

    companion object {
        private val logger = LoggerFactory.getLogger(BuildReviewCheckListener::class.java)
    }

    @RabbitListener(
        bindings = [(QueueBinding(
            value = Queue(value = StreamMQ.QUEUE_PIPELINE_BUILD_REVIEW_CHECK_STREAM, durable = "true"),
            exchange = Exchange(
                value = MQ.EXCHANGE_PIPELINE_BUILD_REVIEW_CHECK_FANOUT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.FANOUT
            )
        ))]
    )
    fun buildReviewAndCheckListener(buildReviewEvent: PipelineBuildReviewCheckBroadCastEvent) {
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

            val gitProjectId = requestEvent.gitProjectId
            val v2GitSetting = streamBasicSettingDao.getSetting(dslContext, gitProjectId)
                ?: throw OperationException("git ci all projectCode not exist")

            val pipeline = streamPipelineService.getPipelineById(gitProjectId, pipelineId)
                ?: throw OperationException("git ci pipeline not exist")

            val context = StreamBuildStageListenerContextV2(
                buildEvent = BuildEvent(
                    projectId = buildReviewEvent.projectId,
                    pipelineId = buildReviewEvent.pipelineId,
                    userId = buildReviewEvent.userId,
                    buildId = buildReviewEvent.buildId,
                    status = buildReviewEvent.status,
                    startTime = streamBuild.createTime.timestampmilli()
                ),
                requestEvent = requestEvent,
                streamBuildEvent = buildEvent,
                pipeline = pipeline,
                streamSetting = v2GitSetting,
                reviewType = buildReviewEvent.reviewType,
                qualityRuleIds = buildReviewEvent.ruleIds
            )

            when (buildReviewEvent.reviewType) {
                BuildReviewType.STAGE_REVIEW -> {
                    // 推送构建消息
                    sendCommitCheck.sendCommitCheck(context)
                }
                BuildReviewType.QUALITY_CHECK_IN, BuildReviewType.QUALITY_CHECK_OUT -> {
                    // 凡是检查有结果了都推送评论
                    sendQualityMrComment.sendMrComment(context)
                    // 如果是待把关的状态则发生审核消息
                    if (buildReviewEvent.status == BuildStatus.QUALITY_CHECK_WAIT.name) {
                        sendCommitCheck.sendCommitCheck(context)
                    }
                }
                // 这里先这么写，未来如果这么枚举扩展代码编译时可以第一时间感知，防止漏过事件
                BuildReviewType.TASK_REVIEW -> {
                    logger.warn("buildReviewListener event not match: ${buildReviewEvent.reviewType}")
                }
            }
        } catch (e: Exception) {
            logger.warn("buildReviewAndCheckListener error: ${e.message}")
        }
    }
}
