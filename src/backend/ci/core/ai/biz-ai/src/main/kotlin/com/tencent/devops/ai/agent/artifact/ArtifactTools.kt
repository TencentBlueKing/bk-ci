package com.tencent.devops.ai.agent.artifact

import com.tencent.devops.ai.agent.BaseTools
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.constant.REPO_NAME_CUSTOM
import com.tencent.devops.artifactory.constant.REPO_NAME_PIPELINE
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.client.Client
import io.agentscope.core.tool.Tool
import io.agentscope.core.tool.ToolParam
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Supplier

/**
 * 制品管理工具集，提供制品检索与下载链接生成能力。
 *
 * 所有工具方法通过 [ServiceArtifactoryResource] 调用制品微服务，
 * 操作人身份由 [userIdSupplier] 在运行时注入。
 */
class ArtifactTools(
    client: Client,
    userIdSupplier: Supplier<String>
) : BaseTools(client, userIdSupplier) {

    override val logger: Logger = LoggerFactory.getLogger(ArtifactTools::class.java)

    private fun artifactoryResource() = client.get(ServiceArtifactoryResource::class)

    @Tool(
        name = "搜索制品",
        description = "按流水线ID和构建ID搜索项目下的制品文件。"
    )
    fun search(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "pipelineId", description = "流水线ID")
        pipelineId: String,
        @ToolParam(name = "buildId", description = "构建ID")
        buildId: String,
        @ToolParam(name = "page", description = "页码，默认0", required = false)
        page: Int? = null,
        @ToolParam(
            name = "pageSize",
            description = "每页条数，默认100，最大1000",
            required = false
        )
        pageSize: Int? = null
    ): String {
        return safeQuery("ArtifactTool", "search") {
            val result = artifactoryResource().search(
                userId = getOperatorUserId(),
                projectId = projectId,
                page = page ?: DEFAULT_PAGE,
                pageSize = (pageSize ?: DEFAULT_PAGE_SIZE).coerceIn(1, MAX_PAGE_SIZE),
                searchProps = listOf(
                    Property(key = "pipelineId", value = pipelineId),
                    Property(key = "buildId", value = buildId)
                )
            )
            val data = result.data ?: return@safeQuery "查询失败: ${result.message}"
            if (data.records.isEmpty()) return@safeQuery "未找到符合条件的制品"
            data.records.forEach {
                val repoName = it.artifactoryType.toBkrepoName()
                if ((repoName == REPO_NAME_CUSTOM || repoName == REPO_NAME_PIPELINE) && !it.folder) {
                    it.downloadUrl = "/bkrepo/api/user/generic/$projectId/$repoName${it.fullPath}"
                }
            }
            toJson(data)
        }
    }

    @Tool(
        name = "生成制品分享下载链接",
        description = "为指定制品生成分享下载链接。" +
                "分享链接可在浏览器和脚本中使用，在有效期内任意用户都可下载，不依赖当前蓝盾登录态。" +
                "适用于分享给他人或脚本集成下载。artifactoryType 可选值包括 PIPELINE、CUSTOM_DIR。"
    )
    fun shareDownloadUrl(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(
            name = "artifactoryType",
            description = "仓库类型，如 PIPELINE、CUSTOM_DIR"
        )
        artifactoryType: String,
        @ToolParam(name = "path", description = "制品路径")
        path: String,
        @ToolParam(name = "ttl", description = "链接有效期（秒），默认3600", required = false)
        ttl: Int? = null,
    ): String {
        return safeOperate(
            "ArtifactTool", "shareDownloadUrl", mapOf(
                "projectId" to projectId,
                "artifactoryType" to artifactoryType,
                "path" to path,
                "ttl" to ttl
            )
        ) {
            val result = artifactoryResource().downloadUrl(
                projectId = projectId,
                artifactoryType = ArtifactoryType.valueOf(artifactoryType.uppercase()),
                userId = getOperatorUserId(),
                path = path,
                ttl = ttl ?: DEFAULT_TTL_SECONDS,
                directed = true
            )
            val data = result.data ?: return@safeOperate "生成下载链接失败: ${result.message}"
            toJson(data)
        }
    }

    @Tool(
        name = "生成制品用户态下载链接",
        description = "为指定制品生成用户态下载链接。" +
                "用户态下载链接主要用于浏览器内访问，需要已登录蓝盾，无法直接用于脚本下载，权限按当前用户身份校验。" +
                "适用于当前用户在页面中点击下载。artifactoryType 可选值包括 PIPELINE、CUSTOM_DIR。"
    )
    fun userDownloadUrl(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(
            name = "artifactoryType",
            description = "仓库类型，如 PIPELINE、CUSTOM_DIR"
        )
        artifactoryType: String,
        @ToolParam(name = "path", description = "制品路径")
        path: String
    ): String {
        return safeQuery("ArtifactTool", "userDownloadUrl") {
            val result = artifactoryResource().downloadUrlForOpenApi(
                userId = getOperatorUserId(),
                projectId = projectId,
                artifactoryType = ArtifactoryType.valueOf(artifactoryType.uppercase()),
                path = path
            )
            val data = result.data ?: return@safeQuery "生成用户态下载链接失败: ${result.message}"
            toJson(data)
        }
    }

    companion object {
        private const val DEFAULT_PAGE = 0
        private const val DEFAULT_PAGE_SIZE = 100
        private const val MAX_PAGE_SIZE = 1000
        private const val DEFAULT_TTL_SECONDS = 3600
    }
}