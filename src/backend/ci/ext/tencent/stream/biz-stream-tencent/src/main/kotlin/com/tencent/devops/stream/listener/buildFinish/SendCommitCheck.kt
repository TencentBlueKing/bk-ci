package com.tencent.devops.stream.listener.buildFinish

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.stream.client.ScmClient
import com.tencent.devops.stream.config.StreamBuildFinishConfig
import com.tencent.devops.stream.pojo.enums.StreamMrEventAction
import com.tencent.devops.stream.pojo.git.GitEvent
import com.tencent.devops.stream.pojo.git.GitMergeRequestEvent
import com.tencent.devops.stream.pojo.isMr
import com.tencent.devops.stream.pojo.sendCommitCheck
import com.tencent.devops.stream.trigger.GitCheckService
import com.tencent.devops.stream.utils.GitCIPipelineUtils
import com.tencent.devops.stream.utils.StreamTriggerMessageUtils
import com.tencent.devops.stream.v2.service.StreamQualityService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SendCommitCheck @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val client: Client,
    private val scmClient: ScmClient,
    private val config: StreamBuildFinishConfig,
    private val gitCheckService: GitCheckService,
    private val triggerMessageUtil: StreamTriggerMessageUtils,
    private val streamQualityService: StreamQualityService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SendCommitCheck::class.java)
        private const val BUILD_SUCCESS_DESC = "Your pipeline「%s」 is succeed."
        private const val BUILD_CANCEL_DESC = "Your pipeline「%s」 was cancelled."
        private const val BUILD_FAILED_DESC = "Your pipeline「%s」 is failed."
    }

    fun sendCommitCheck(
        context: StreamFinishContext
    ) {
        // 当人工触发时不推送CommitCheck消息
        if (!context.requestEvent.sendCommitCheck()) {
            return
        }

        when (context) {
            is StreamFinishContextV2 -> {
                sendCommitCheckV2(context)
            }
            is StreamFinishContextV1 -> {
                sendCommitCheckV1(context)
            }
        }
    }

    private fun sendCommitCheckV2(
        context: StreamFinishContextV2
    ) {
        with(context) {
            // gitRequestEvent中存的为mriid不是mrid
            val gitEvent = try {
                objectMapper.readValue<GitEvent>(requestEvent.event)
            } catch (e: Throwable) {
                logger.error("push commit check get mergeId error ${e.message}")
                null
            }

            gitCheckService.pushCommitCheck(
                commitId = requestEvent.commitId,
                description = triggerMessageUtil.getCommitCheckDesc(
                    prefix = getDescByBuildStatus(buildStatus, pipeline.displayName),
                    objectKind = requestEvent.objectKind,
                    action = if (gitEvent is GitMergeRequestEvent) {
                        StreamMrEventAction.getActionValue(gitEvent) ?: ""
                    } else {
                        ""
                    },
                    userId = buildFinishEvent.userId
                ),
                mergeRequestId = if (gitEvent is GitMergeRequestEvent) {
                    gitEvent.object_attributes.id
                } else {
                    0L
                },
                buildId = buildFinishEvent.buildId,
                userId = buildFinishEvent.userId,
                status = getGitCommitCheckState(),
                context = "${pipeline.filePath}@${requestEvent.objectKind.toUpperCase()}",
                gitCIBasicSetting = streamSetting,
                pipelineId = buildFinishEvent.pipelineId,
                block = requestEvent.isMr() && !buildStatus.isSuccess() && streamSetting.enableMrBlock,
                reportData = streamQualityService.getQualityGitMrResult(
                    client = client,
                    gitProjectId = streamSetting.gitProjectId,
                    pipelineName = pipeline.displayName,
                    event = buildFinishEvent
                ),
                targetUrl = GitCIPipelineUtils.genGitCIV2BuildUrl(
                    homePage = config.v2GitUrl ?: throw ParamBlankException("启动配置缺少 rtx.v2GitUrl"),
                    gitProjectId = streamSetting.gitProjectId,
                    pipelineId = pipeline.pipelineId,
                    buildId = buildFinishEvent.buildId
                )
            )
        }
    }

    // 根据状态切换描述
    private fun getDescByBuildStatus(buildStatus: BuildStatus, pipelineName: String): String {
        return when {
            buildStatus.isSuccess() -> {
                BUILD_SUCCESS_DESC.format(pipelineName)
            }
            buildStatus.isCancel() -> {
                BUILD_CANCEL_DESC.format(pipelineName)
            }
            else -> {
                BUILD_FAILED_DESC.format(pipelineName)
            }
        }
    }

    private fun sendCommitCheckV1(
        context: StreamFinishContextV1
    ) {
        with(context) {
            scmClient.pushCommitCheck(
                commitId = requestEvent.commitId,
                description = requestEvent.commitMsg ?: "",
                mergeRequestId = requestEvent.mergeRequestId ?: 0L,
                pipelineId = pipeline.pipelineId,
                buildId = buildFinishEvent.buildId,
                userId = buildFinishEvent.userId,
                status = getGitCommitCheckState(),
                context = "${pipeline.displayName}(${pipeline.filePath})",
                gitProjectConf = streamSetting
            )
        }
    }
}
