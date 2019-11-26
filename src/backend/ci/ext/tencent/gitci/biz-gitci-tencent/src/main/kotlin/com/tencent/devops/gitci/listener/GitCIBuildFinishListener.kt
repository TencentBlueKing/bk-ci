package com.tencent.devops.gitci.listener

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.ci.OBJECT_KIND_MANUAL
import com.tencent.devops.gitci.client.ScmClient
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
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
class GitCIBuildFinishListener @Autowired constructor(
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitCISettingDao: GitCISettingDao,
    private val scmClient: ScmClient,
    private val dslContext: DSLContext
) {

    @RabbitListener(
        bindings = [(QueueBinding(
            value = Queue(value = MQ.QUEUE_PIPELINE_BUILD_FINISH_GITCI, durable = "true"),
            exchange = Exchange(
                value = MQ.EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.FANOUT
            )
        ))]
    )
    fun listenPipelineBuildFinishBroadCastEvent(buildFinishEvent: PipelineBuildFinishBroadCastEvent) {
        try {
            val record = gitRequestEventBuildDao.getEventByBuildId(dslContext, buildFinishEvent.buildId)
            if (record != null) {
                val objectKind = record["OBJECT_KIND"] as String

                // 推送结束构建消息,当人工触发时不推送构建消息
                if (objectKind != OBJECT_KIND_MANUAL) {
                    val commitId = record["COMMIT_ID"] as String
                    val gitProjectId = record["GIT_PROJECT_ID"] as Long
                    var mergeRequestId = 0L
                    if (record["MERGE_REQUEST_ID"] != null) {
                        mergeRequestId = record["MERGE_REQUEST_ID"] as Long
                    }
                    val description = record["DESCRIPTION"] as String

                    val gitProjectConf = gitCISettingDao.getSetting(dslContext, gitProjectId) ?: throw OperationException("git ci projectCode not exist")

                    // 检测状态
                    val state = if (BuildStatus.isFailure(BuildStatus.valueOf(buildFinishEvent.status))) {
                        "failure"
                    } else {
                        "success"
                    }

                    scmClient.pushCommitCheck(
                        commitId,
                        description,
                        mergeRequestId,
                        buildFinishEvent.buildId,
                        buildFinishEvent.userId,
                        state,
                        gitProjectConf
                    )
                }
            } else {
                logger.error("No event record about build(${buildFinishEvent.buildId}), ignore push commit check.")
            }
        } catch (e: Throwable) {
            logger.error("Fail to push commit check build(${buildFinishEvent.buildId})", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitCIBuildFinishListener::class.java)
    }
}
