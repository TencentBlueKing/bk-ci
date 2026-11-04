package com.tencent.devops.process.trigger.artifact

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.pojo.element.trigger.ArtifactTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ArtifactTriggerInput
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactKind
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactRepositoryType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactTriggerEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TYPE
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ARTIFACT_KIND
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ARTIFACT_REPO_TYPE
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactEvent
import com.tencent.devops.process.trigger.artifact.handler.ArtifactEventHandlerManager
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactTriggerParam
import com.tencent.devops.process.trigger.enums.MatchStatus
import com.tencent.devops.process.trigger.pojo.WebhookAtomResponse
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_WEBHOOK_USER_ID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 制品触发匹配器（唯一入口，仿 ScmWebhookTriggerMatcher）
 *
 * 命中后按三部分组装启动参数：匹配输出、事件输出、插件输出。
 */
@Service
class ArtifactTriggerMatcher(
    private val eventHandlerManager: ArtifactEventHandlerManager
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactTriggerMatcher::class.java)
    }

    fun matches(
        projectId: String,
        pipelineId: String,
        element: ArtifactTriggerElement,
        event: ArtifactEvent,
        variables: Map<String, String>
    ): WebhookAtomResponse {
        val input = parseInput(element.data.input, variables)
        val result = eventHandlerManager.evaluate(
            projectId = projectId,
            pipelineId = pipelineId,
            params = input,
            event = event
        )
        return when {
            result.isMatch -> {
                logger.info(
                    "artifact trigger match success|project=$projectId|pipeline=$pipelineId|element=${element.id}"
                )
                val startParams = mutableMapOf<String, Any>()
                startParams.putAll(result.extra)
                startParams.putAll(eventHandlerManager.outputs(event))
                startParams.putAll(elementOutputs(input = input, element = element, event = event))
                WebhookAtomResponse(matchStatus = MatchStatus.SUCCESS, outputVars = startParams)
            }
            // 无失败原因表示本插件非该事件目标（如目录归档未完成），跳过且不记录触发事件
            result.reason == null -> {
                logger.info(
                    "artifact trigger skip|project=$projectId|pipeline=$pipelineId|element=${element.id}"
                )
                WebhookAtomResponse(matchStatus = MatchStatus.SKIP)
            }
            else -> {
                logger.info(
                    "artifact trigger match failed|project=$projectId|pipeline=$pipelineId|" +
                        "element=${element.id}|reason=${result.reason}"
                )
                WebhookAtomResponse(
                    matchStatus = MatchStatus.CONDITION_NOT_MATCH,
                    failedReason = result.reason
                )
            }
        }
    }

    private fun parseInput(
        input: ArtifactTriggerInput,
        variables: Map<String, String>
    ): ArtifactTriggerParam {
        fun parse(value: String?) = value?.let { EnvUtils.parseEnv(it, variables) }
        fun parseList(value: String?) = WebhookUtils.convert(parse(value))
        val watchRootPath = parse(input.watchRootPath)?.ifBlank { null }
        return ArtifactTriggerParam(
            repository = input.repository,
            watchPipeline = parse(input.watchPipeline)?.ifBlank { null },
            watchRootPath = watchRootPath,
            kind = input.kind,
            artifactsName = parseList(input.artifactsName),
            artifactsNameIgnore = parseList(input.artifactsNameIgnore),
            paths = parseList(input.paths).map {
                ArtifactWebhookUtils.joinPath(watchRootPath, it)
            },
            pathsIgnore = parseList(input.pathsIgnore).map {
                ArtifactWebhookUtils.joinPath(watchRootPath, it)
            },
            image = parse(input.image),
            tags = parseList(input.tags),
            tagsIgnore = parseList(input.tagsIgnore),
            metadata = input.metadata
        )
    }

    /**
     * 插件输出：仓库类型、触发形态，以及启动构建所需的公共 webhook 参数。
     * 镜像仓库的 kind 固定为 image。
     */
    private fun elementOutputs(
        input: ArtifactTriggerParam,
        element: ArtifactTriggerElement,
        event: ArtifactEvent
    ): Map<String, Any> {
        val kind = if (input.repository == ArtifactRepositoryType.IMAGE) {
            ArtifactKind.IMAGE.value
        } else {
            (input.kind ?: ArtifactKind.FILE).value
        }
        val params = mutableMapOf<String, Any>(
            CI_ARTIFACT_REPO_TYPE to input.repository.value,
            CI_ARTIFACT_KIND to kind,
            BK_REPO_WEBHOOK_REPO_TYPE to CodeType.ARTIFACT.name,
            PIPELINE_WEBHOOK_TYPE to CodeType.ARTIFACT.name,
            PIPELINE_WEBHOOK_EVENT_TYPE to ArtifactTriggerEventType.ARRIVED.name,
            PIPELINE_START_WEBHOOK_USER_ID to event.user.userId
        )
        element.id?.let { params[PIPELINE_START_TASK_ID] = it }
        return params
    }
}
