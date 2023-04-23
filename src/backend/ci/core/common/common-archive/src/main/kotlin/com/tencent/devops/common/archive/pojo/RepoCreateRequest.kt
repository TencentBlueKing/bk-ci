package com.tencent.devops.common.archive.pojo

import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.RepositoryConfiguration
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * 创建仓库请求
 */
@ApiModel("创建仓库请求")
data class RepoCreateRequest(
    @ApiModelProperty("所属项目id", required = true)
    val projectId: String,
    @ApiModelProperty("仓库名称", required = true)
    val name: String,
    @ApiModelProperty("仓库类型", required = true)
    val type: RepositoryType,
    @ApiModelProperty("仓库类别", required = true)
    val category: RepositoryCategory,
    @ApiModelProperty("是否公开", required = true)
    val public: Boolean,
    @ApiModelProperty("简要描述", required = false)
    val description: String? = null,
    @ApiModelProperty("仓库配置", required = true)
    val configuration: RepositoryConfiguration? = null,
    @ApiModelProperty("存储凭证key", required = false)
    val storageCredentialsKey: String? = null,
    @ApiModelProperty("仓库配额", required = false)
    val quota: Long? = null,
    @ApiModelProperty("是否展示", required = false)
    val display: Boolean = true,
    @ApiModelProperty("操作用户", required = false)
    val operator: String = SYSTEM_USER
)
