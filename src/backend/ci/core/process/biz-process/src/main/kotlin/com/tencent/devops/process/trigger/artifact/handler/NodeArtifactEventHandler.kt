package com.tencent.devops.process.trigger.artifact.handler

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactKind
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactRepositoryType
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.service.code.pojo.WebhookMatchResult
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ARTIFACT_COUNT
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ARTIFACT_DIR
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ARTIFACT_NAME
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ARTIFACT_PATH
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ARTIFACT_SHA256
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ARTIFACT_SIZE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ARTIFACT_SOURCE_BUILD_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ARTIFACT_SOURCE_PIPELINE
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.engine.service.PipelineCacheService
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactEvent
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.METADATA_BUILD_NO
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.METADATA_PIPELINE_ID
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.METADATA_SIZE
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.REPO_CUSTOM
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.REPO_PIPELINE
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.SENTINEL_FILE
import com.tencent.devops.process.pojo.trigger.artifact.NodeArtifactEvent
import com.tencent.devops.process.trigger.artifact.ArtifactWebhookUtils
import com.tencent.devops.process.trigger.artifact.condition.ArtifactConditionChain
import com.tencent.devops.process.trigger.artifact.condition.ArtifactsNameCondition
import com.tencent.devops.process.trigger.artifact.condition.MetadataCondition
import com.tencent.devops.process.trigger.artifact.condition.PathCondition
import com.tencent.devops.process.trigger.artifact.condition.RepositoryTypeCondition
import com.tencent.devops.process.trigger.artifact.condition.SourcePipelineCondition
import com.tencent.devops.process.trigger.artifact.condition.WatchPipelineCondition
import com.tencent.devops.process.trigger.artifact.condition.WatchRootPathCondition
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactConditionContext
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactFactParam
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactTriggerParam
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import org.springframework.stereotype.Service

/**
 * 节点事件处理器（文件制品，触发形态 file/folder）
 *
 * 流水线仓库与自定义仓库统一条件链：
 * [防循环, 监听流水线, 路径/名称, 元数据]
 */
@Service
class NodeArtifactEventHandler(
    private val pipelineCacheService: PipelineCacheService,
    private val pipelineUrlBean: PipelineUrlBean
) : ArtifactEventHandler {

    override fun support(event: ArtifactEvent): Boolean = event is NodeArtifactEvent

    override fun getEventDesc(event: ArtifactEvent): I18Variable {
        val nodeEvent = event as NodeArtifactEvent
        val node = nodeEvent.node
        val metadata = metadataOf(nodeEvent)
        val isFolder = node.name == SENTINEL_FILE
        val code = if (isFolder) {
            WebhookI18nConstants.BK_ARTIFACT_FOLDER_ARRIVED_EVENT_DESC
        } else {
            WebhookI18nConstants.BK_ARTIFACT_FILE_ARRIVED_EVENT_DESC
        }
        val pipelineId = metadata[METADATA_PIPELINE_ID]?.toString().orEmpty()
        val buildId = ArtifactWebhookUtils.metadataBuildId(metadata).orEmpty()
        val buildNo = metadata[METADATA_BUILD_NO]?.toString().orEmpty()
        val pipelineName = pipelineCacheService.getPipelineName(node.projectId, pipelineId) ?: pipelineId
        val channelCode = pipelineCacheService.getChannelCode(node.projectId, pipelineId)
        val artifactName = if (isFolder) {
            if (node.repoName == REPO_PIPELINE) {
                ArtifactWebhookUtils.getPipelineRepoPath(node.path)
            } else {
                node.path
            }
        } else {
            node.name
        }
        val sizeText = if (isFolder) {
            ArtifactWebhookUtils.sizeText(ArtifactWebhookUtils.metadataLong(metadata, METADATA_SIZE))
        } else {
            ArtifactWebhookUtils.sizeText(node.size)
        }
        val detailUrl = ArtifactWebhookUtils.buildDetailUrl(
            pipelineUrlBean, node.projectId, pipelineId, buildId, channelCode
        )
        return I18Variable(
            code = code,
            params = listOf(
                ArtifactWebhookUtils.buildOutputsUrl(detailUrl),
                artifactName,
                sizeText,
                detailUrl,
                ArtifactWebhookUtils.sourceDisplayName(pipelineName, buildNo),
                nodeEvent.user.userId,
                node.repoName
            )
        )
    }

    override fun evaluate(
        projectId: String,
        pipelineId: String,
        triggerParams: ArtifactTriggerParam,
        event: ArtifactEvent
    ): WebhookMatchResult {
        val nodeEvent = event as NodeArtifactEvent
        val node = nodeEvent.node
        val metadata = metadataOf(nodeEvent)
        // kind=folder：目录逐个文件上传，每个文件一条 webhook；仅哨兵到达才视为目录到齐，中间文件跳过。
        // kind=file：哨兵不是用户制品，文件触发应跳过哨兵。
        // 跳过均返回无原因的不匹配，匹配器识别为 SKIP，不记录触发事件。
        val isSentinel = node.name == SENTINEL_FILE
        val skip = when (triggerParams.kind) {
            ArtifactKind.FOLDER -> !isSentinel
            else -> isSentinel
        }
        if (skip) {
            return WebhookMatchResult(false)
        }
        // 根路径仅自定义仓库有值
        val rootPath = if (node.repoName == REPO_CUSTOM) {
            ArtifactWebhookUtils.getCustomRootPath(node.path)
        } else {
            null
        }
        // 制品名称仅流水线仓库有值
        val artifactsName = if (node.repoName == REPO_PIPELINE) {
            if (isSentinel) {
                ArtifactWebhookUtils.getPipelineRepoPath(node.path)
            } else {
                node.name
            }
        } else {
            null
        }
        // 匹配路径仅自定义仓库有值，均去掉根目录：文件取相对路径（含文件名），目录取归档目录路径（不含其中文件）
        val paths = if (node.repoName == REPO_CUSTOM) {
            val rawPath = if (isSentinel) node.path else node.fullPath
            listOf(ArtifactWebhookUtils.getCustomRelativePath(rawPath))
        } else {
            null
        }
        val factParam = ArtifactFactParam(
            projectId = node.projectId,
            repoName = node.repoName,
            rootPath = rootPath,
            artifactsName = artifactsName,
            paths = paths,
            sourcePipelineId = metadata[METADATA_PIPELINE_ID]?.toString(),
            sourceBuildId = ArtifactWebhookUtils.metadataBuildId(metadata),
            metadata = metadata
        )

        val context = ArtifactConditionContext(
            projectId = projectId,
            pipelineId = pipelineId,
            triggerParam = triggerParams,
            factParam = factParam
        )
        val conditions = mutableListOf(
            RepositoryTypeCondition(),
            SourcePipelineCondition(),
            WatchPipelineCondition()
        )
        when (triggerParams.repository) {
            ArtifactRepositoryType.PIPELINE -> {
                conditions.add(ArtifactsNameCondition())
            }

            ArtifactRepositoryType.CUSTOM -> {
                conditions.add(WatchRootPathCondition())
                conditions.add(PathCondition())
            }

            else -> {
                return WebhookMatchResult(false)
            }
        }
        conditions.add(MetadataCondition())
        return ArtifactConditionChain(conditions).match(context)
    }

    /**
     * 节点事件输出。目录通过哨兵文件 [SENTINEL_FILE] 判定：
     * 文件注入 name/path/sha256/size，目录注入 dir，两者互斥。
     */
    override fun outputs(event: ArtifactEvent): Map<String, Any> {
        val nodeEvent = event as NodeArtifactEvent
        val node = nodeEvent.node
        val metadata = metadataOf(nodeEvent)
        val result = mutableMapOf<String, Any>(CI_ARTIFACT_COUNT to 1)
        metadata[METADATA_PIPELINE_ID]?.toString()?.takeIf { it.isNotBlank() }?.let {
            result[CI_ARTIFACT_SOURCE_PIPELINE] = it
        }
        ArtifactWebhookUtils.metadataBuildId(metadata)?.let {
            result[CI_ARTIFACT_SOURCE_BUILD_ID] = it
        }
        val isFolder = node.name == SENTINEL_FILE
        if (isFolder) {
            result[CI_ARTIFACT_DIR] = node.path
        } else {
            result[CI_ARTIFACT_NAME] = node.name
            result[CI_ARTIFACT_PATH] = node.path
            if (node.sha256.isNotBlank()) {
                result[CI_ARTIFACT_SHA256] = node.sha256
            }
            result[CI_ARTIFACT_SIZE] = node.size
        }
        val artifactName = if (isFolder) {
            if (node.repoName == REPO_PIPELINE) {
                ArtifactWebhookUtils.getPipelineRepoPath(node.path)
            } else {
                node.path
            }
        } else {
            node.name
        }
        if (artifactName.isNotBlank()) {
            result[PIPELINE_BUILD_MSG] = ArtifactWebhookUtils.arrivedBuildMsg(
                artifactName,
                if (isFolder) ArtifactKind.FOLDER else ArtifactKind.FILE
            )
        }
        return result
    }

    private fun metadataOf(event: NodeArtifactEvent): Map<String, Any?> {
        return event.node.nodeMetadata.associate { (it.key ?: "") to it.value }
    }
}
