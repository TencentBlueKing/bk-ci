package com.tencent.devops.common.archive.pojo

import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.RepositoryConfiguration
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 创建仓库请求
 */
@Schema(title = "创建仓库请求")
data class RepoCreateRequest(
    @get:Schema(title = "所属项目id", required = true)
    val projectId: String,
    @get:Schema(title = "仓库名称", required = true)
    val name: String,
    @get:Schema(title = "仓库类型", required = true)
    val type: RepositoryType,
    @get:Schema(title = "仓库类别", required = true)
    val category: RepositoryCategory,
    @get:Schema(title = "是否公开", required = true)
    val public: Boolean,
    @get:Schema(title = "简要描述", required = false)
    val description: String? = null,
    @get:Schema(title = "仓库配置", required = true)
    val configuration: RepositoryConfiguration? = null,
    @get:Schema(title = "存储凭证key", required = false)
    val storageCredentialsKey: String? = null,
    @get:Schema(title = "仓库配额", required = false)
    val quota: Long? = null,
    @get:Schema(title = "是否展示", required = false)
    val display: Boolean = true,
    @get:Schema(title = "操作用户", required = false)
    val operator: String = SYSTEM_USER
)
