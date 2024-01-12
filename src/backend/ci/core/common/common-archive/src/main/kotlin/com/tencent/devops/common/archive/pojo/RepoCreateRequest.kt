package com.tencent.devops.common.archive.pojo

import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.RepositoryConfiguration
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 创建仓库请求
 */
@Schema(description = "创建仓库请求")
data class RepoCreateRequest(
    @Schema(description = "所属项目id", required = true)
    val projectId: String,
    @Schema(description = "仓库名称", required = true)
    val name: String,
    @Schema(description = "仓库类型", required = true)
    val type: RepositoryType,
    @Schema(description = "仓库类别", required = true)
    val category: RepositoryCategory,
    @Schema(description = "是否公开", required = true)
    val public: Boolean,
    @Schema(description = "简要描述", required = false)
    val description: String? = null,
    @Schema(description = "仓库配置", required = true)
    val configuration: RepositoryConfiguration? = null,
    @Schema(description = "存储凭证key", required = false)
    val storageCredentialsKey: String? = null,
    @Schema(description = "仓库配额", required = false)
    val quota: Long? = null,
    @Schema(description = "是否展示", required = false)
    val display: Boolean = true,
    @Schema(description = "操作用户", required = false)
    val operator: String = SYSTEM_USER
)
