package com.tencent.devops.process.trigger.artifact

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactKind
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.METADATA_BK_CI_BID
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.METADATA_BUILD_ID
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.METADATA_SIZE
import java.util.Locale

/**
 * 制品到达 Webhook 工具类
 */
object ArtifactWebhookUtils {

    /**
     * 流水线仓库相对路径：去掉 /{pipelineId}/{buildId} 前缀。
     * 例：/p-xxx/b-xxx/test/ -> test
     */
    fun getPipelineRepoPath(path: String): String {
        val segments = path.trim('/').split('/').filter { it.isNotBlank() }
        return segments.drop(2).joinToString("/")
    }

    /**
     * 自定义仓库根路径：取第一层目录。
     * 例：/2026-08-20/test1/ -> 2026-08-20
     */
    fun getCustomRootPath(path: String): String {
        return path.trim('/').substringBefore('/')
    }

    /**
     * 自定义仓库相对路径：去掉第一层根目录。
     * 例：/2026-08-20/test1/foo.jar -> test1/foo.jar
     */
    fun getCustomRelativePath(fullPath: String): String {
        val segments = fullPath.trim('/').split('/').filter { it.isNotBlank() }
        return segments.drop(1).joinToString("/")
    }

    /**
     * 来源构建详情页，复用 [PipelineUrlBean.genBuildDetailUrl]。
     * 缺少任一标识则返回空串，前端降级为纯文本。
     */
    fun buildDetailUrl(
        pipelineUrlBean: PipelineUrlBean,
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode? = null
    ): String {
        if (projectId.isBlank() || pipelineId.isBlank() || buildId.isBlank()) return ""
        return pipelineUrlBean.genBuildDetailUrl(
            projectCode = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            position = null,
            stageId = null,
            needShortUrl = false,
            channelCode = channelCode
        )
    }

    /**
     * 来源构建制品页：在详情 URL 后追加 outputs。
     */
    fun buildOutputsUrl(detailUrl: String): String {
        return if (detailUrl.isBlank()) "" else "$detailUrl/outputs"
    }

    /**
     * 将字节数格式化为可读大小，如 48.2 MB。
     */
    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(Locale.US, kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(Locale.US, mb)
        return "%.1f GB".format(Locale.US, mb / 1024.0)
    }

    /**
     * 读取来源构建 ID：优先 [METADATA_BUILD_ID]，归档目录可能只有 [METADATA_BK_CI_BID]。
     */
    fun metadataBuildId(metadata: Map<String, Any?>): String? {
        return metadata[METADATA_BUILD_ID]?.toString()?.takeIf { it.isNotBlank() }
            ?: metadata[METADATA_BK_CI_BID]?.toString()?.takeIf { it.isNotBlank() }
    }

    /**
     * 读取元数据数值。解析失败返回 null，不回退其它字段。
     */
    fun metadataLong(metadata: Map<String, Any?>, key: String = METADATA_SIZE): Long? {
        return when (val value = metadata[key]) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }

    /**
     * 事件描述中的 size 插槽：有值时带括号和尾空格，无值时为空串。
     */
    fun sizeText(bytes: Long?): String {
        return bytes?.let { "(${formatSize(it)}) " }.orEmpty()
    }

    /**
     * 来源展示名：有构建号时为「流水线名#构建号」，否则仅流水线名。
     */
    fun sourceDisplayName(pipelineName: String, buildNo: String): String {
        return if (buildNo.isBlank()) pipelineName else "$pipelineName#$buildNo"
    }

    /**
     * 构建信息：文件「制品{名称}到达」，目录「制品目录{名称}到达」，镜像「镜像{名称}到达」。
     */
    fun arrivedBuildMsg(
        artifactName: String,
        kind: ArtifactKind = ArtifactKind.FILE
    ): String {
        val messageCode = when (kind) {
            ArtifactKind.FOLDER -> WebhookI18nConstants.BK_ARTIFACT_FOLDER_ARRIVED_BUILD_MSG
            ArtifactKind.IMAGE -> WebhookI18nConstants.BK_ARTIFACT_IMAGE_ARRIVED_BUILD_MSG
            ArtifactKind.FILE -> WebhookI18nConstants.BK_ARTIFACT_ARRIVED_BUILD_MSG
        }
        val defaultMessage = when (kind) {
            ArtifactKind.FOLDER -> "Artifact directory $artifactName arrived"
            ArtifactKind.IMAGE -> "Image $artifactName arrived"
            ArtifactKind.FILE -> "Artifact $artifactName arrived"
        }
        return I18nUtil.getCodeLanMessage(
            messageCode = messageCode,
            params = arrayOf(artifactName),
            defaultMessage = defaultMessage
        )
    }
}
