package com.tencent.devops.process.trigger.artifact

import com.tencent.devops.common.archive.constant.REPO_CUSTOM
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactKind
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactEvent
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.DOCKER_PACKAGE_KEY_PREFIX
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.METADATA_BK_CI_BID
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.METADATA_BUILD_ID
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.METADATA_SIZE
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.REPO_PIPELINE
import com.tencent.devops.process.pojo.trigger.artifact.NodeArtifactEvent
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
     * 规范化监听路径：补齐前后 `/`。仓库根目录 `/` 返回 null，避免整仓监听。
     * 例：aaa/bbb -> /aaa/bbb/
     */
    fun normalizePath(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank() || trimmed == "/") return null
        val withPrefix = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        return if (withPrefix.endsWith("/")) withPrefix else "$withPrefix/"
    }

    /**
     * 流水线仓库 EVENT_SCOPE：从 path 只取第一层。
     * 例：/p-xxx/b-xxx/ -> /p-xxx/
     */
    fun getPipelineRepoScope(path: String): String? {
        val first = path.trim('/').split('/').firstOrNull { it.isNotBlank() } ?: return null
        return "/$first/"
    }

    /**
     * 自定义仓库 EVENT_SCOPE：把 path 拆成全部目录前缀。
     * 例：/aaa/bbb/ccc/ -> [/aaa/, /aaa/bbb/, /aaa/bbb/ccc/]
     */
    fun getCustomRepoScope(path: String): List<String> {
        val segments = path.trim('/').split('/').filter { it.isNotBlank() }
        return List(segments.size) { index ->
            "/" + segments.take(index + 1).joinToString("/") + "/"
        }
    }

    /**
     * 订阅反查 scopes：流水线仓取一层，自定义仓拆前缀。镜像仓没有 PATH，返回 null。
     */
    fun buildQueryScopes(repoName: String, path: String): List<String>? {
        return when (repoName) {
            REPO_PIPELINE -> listOfNotNull(getPipelineRepoScope(path))
            REPO_CUSTOM -> getCustomRepoScope(path)
            else -> null
        }
    }

    /**
     * 按事件计算订阅反查 scopes。
     * 镜像仓库没有 PATH，返回 null，反查不加 EVENT_SCOPE 条件。
     */
    fun buildQueryScopes(event: ArtifactEvent): List<String>? {
        return when (event) {
            is NodeArtifactEvent -> buildQueryScopes(event.node.repoName, event.node.path)
            else -> emptyList()
        }
    }

    /**
     * 镜像名：去掉 docker:// 协议前缀。
     */
    fun imageName(packageKey: String): String = packageKey.removePrefix(DOCKER_PACKAGE_KEY_PREFIX)

    /**
     * 事件目录是否以监听前缀为目录边界前缀。
     */
    fun startsWithWatchPath(eventPath: String, watchPath: String): Boolean {
        val watch = normalizePath(watchPath) ?: return false
        val event = normalizePath(eventPath) ?: return false
        return event.startsWith(watch)
    }

    /**
     * 把 paths Glob 拼到监听根路径上，再和事件完整路径对比。
     * 例：watch=/aaa/bbb/ 与相对 Glob 拼接为 /aaa/bbb/ + 相对路径。
     */
    fun joinPath(rootPath: String?, relativePath: String): String {
        val rel = relativePath.trim()
        if (rel.isBlank()) return rel
        val watch = rootPath?.let { normalizePath(it) }
        if (watch.isNullOrBlank()) {
            return if (rel.startsWith("/")) rel else "/$rel"
        }
        return watch + rel.trimStart('/')
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
