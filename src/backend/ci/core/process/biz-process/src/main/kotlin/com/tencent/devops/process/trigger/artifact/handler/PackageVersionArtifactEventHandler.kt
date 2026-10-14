package com.tencent.devops.process.trigger.artifact.handler

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactKind
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.service.code.pojo.WebhookMatchResult
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ARTIFACT_COUNT
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ARTIFACT_IMAGE_DIGEST
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ARTIFACT_IMAGE_NAME
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ARTIFACT_IMAGE_TAG
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ARTIFACT_SOURCE_BUILD_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ARTIFACT_SOURCE_PIPELINE
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.engine.service.PipelineCacheService
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactEvent
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.DOCKER_PACKAGE_KEY_PREFIX
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.METADATA_BUILD_ID
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.METADATA_BUILD_NO
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.METADATA_DOCKER_MANIFEST_DIGEST
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.METADATA_PIPELINE_ID
import com.tencent.devops.process.pojo.trigger.artifact.PackageVersionArtifactEvent
import com.tencent.devops.process.trigger.artifact.ArtifactWebhookUtils
import com.tencent.devops.process.trigger.artifact.condition.ArtifactConditionChain
import com.tencent.devops.process.trigger.artifact.condition.ImageNameCondition
import com.tencent.devops.process.trigger.artifact.condition.MetadataCondition
import com.tencent.devops.process.trigger.artifact.condition.SourcePipelineCondition
import com.tencent.devops.process.trigger.artifact.condition.TagCondition
import com.tencent.devops.process.trigger.artifact.condition.WatchPipelineCondition
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactConditionContext
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactFactParam
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactTriggerParam
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import org.springframework.stereotype.Service

/**
 * 包版本事件处理器（容器镜像 / 包制品，触发形态 image）
 *
 * 条件链：[防循环, 镜像名, tag, 监听流水线(可选), 元数据]
 */
@Service
class PackageVersionArtifactEventHandler(
    private val pipelineCacheService: PipelineCacheService,
    private val pipelineUrlBean: PipelineUrlBean
) : ArtifactEventHandler {

    override fun support(event: ArtifactEvent): Boolean = event is PackageVersionArtifactEvent

    override fun getEventDesc(event: ArtifactEvent): I18Variable {
        val pkgEvent = event as PackageVersionArtifactEvent
        val metadata = metadataOf(pkgEvent)
        val pipelineId = metadata[METADATA_PIPELINE_ID]?.toString().orEmpty()
        val buildId = metadata[METADATA_BUILD_ID]?.toString().orEmpty()
        val buildNo = metadata[METADATA_BUILD_NO]?.toString().orEmpty()
        val pipelineName = pipelineCacheService.getPipelineName(pkgEvent.projectId, pipelineId) ?: pipelineId
        val channelCode = pipelineCacheService.getChannelCode(pkgEvent.projectId, pipelineId)
        val detailUrl = ArtifactWebhookUtils.buildDetailUrl(
            pipelineUrlBean, pkgEvent.projectId, pipelineId, buildId, channelCode
        )
        return I18Variable(
            code = WebhookI18nConstants.BK_ARTIFACT_IMAGE_ARRIVED_EVENT_DESC,
            params = listOf(
                ArtifactWebhookUtils.buildOutputsUrl(detailUrl),
                "${imageName(pkgEvent)}:${pkgEvent.packageVersion.name}",
                detailUrl,
                ArtifactWebhookUtils.sourceDisplayName(pipelineName, buildNo),
                pkgEvent.user.userId
            )
        )
    }

    override fun evaluate(
        projectId: String,
        pipelineId: String,
        triggerParams: ArtifactTriggerParam,
        event: ArtifactEvent
    ): WebhookMatchResult {
        val pkgEvent = event as PackageVersionArtifactEvent
        val packageVersion = pkgEvent.packageVersion
        val metadata = metadataOf(pkgEvent)
        val factParam = ArtifactFactParam(
            projectId = event.projectId,
            repoName = event.repoName,
            image = imageName(pkgEvent),
            version = packageVersion.name,
            sourcePipelineId = metadata[METADATA_PIPELINE_ID]?.toString(),
            sourceBuildId = metadata[METADATA_BUILD_ID]?.toString(),
            metadata = metadata
        )
        val context = ArtifactConditionContext(
            projectId = projectId,
            pipelineId = pipelineId,
            triggerParam = triggerParams,
            factParam = factParam
        )

        val conditions = listOf(
            SourcePipelineCondition(),
            WatchPipelineCondition(),
            ImageNameCondition(),
            TagCondition(),
            MetadataCondition()
        )
        return ArtifactConditionChain(conditions).match(context)
    }

    /**
     * 镜像事件输出：注入 image_name / image_tag / digest，不注入 path、dir、items。
     */
    override fun outputs(event: ArtifactEvent): Map<String, Any> {
        val pkgEvent = event as PackageVersionArtifactEvent
        val metadata = metadataOf(pkgEvent)
        val result = mutableMapOf<String, Any>(
            CI_ARTIFACT_COUNT to 1,
            CI_ARTIFACT_IMAGE_NAME to imageName(pkgEvent),
            CI_ARTIFACT_IMAGE_TAG to pkgEvent.packageVersion.name
        )
        metadata[METADATA_DOCKER_MANIFEST_DIGEST]?.toString()?.takeIf { it.isNotBlank() }?.let {
            result[CI_ARTIFACT_IMAGE_DIGEST] = it
        }
        metadata[METADATA_PIPELINE_ID]?.toString()?.takeIf { it.isNotBlank() }?.let {
            result[CI_ARTIFACT_SOURCE_PIPELINE] = it
        }
        metadata[METADATA_BUILD_ID]?.toString()?.takeIf { it.isNotBlank() }?.let {
            result[CI_ARTIFACT_SOURCE_BUILD_ID] = it
        }
        result[PIPELINE_BUILD_MSG] = ArtifactWebhookUtils.arrivedBuildMsg(
            "${imageName(pkgEvent)}:${pkgEvent.packageVersion.name}",
            ArtifactKind.IMAGE
        )
        return result
    }

    private fun metadataOf(event: PackageVersionArtifactEvent): Map<String, Any?> {
        return event.packageVersion.packageMetadata.associate { (it.key ?: "") to it.value }
    }

    private fun imageName(event: PackageVersionArtifactEvent): String {
        return event.packageKey.removePrefix(DOCKER_PACKAGE_KEY_PREFIX)
    }
}
