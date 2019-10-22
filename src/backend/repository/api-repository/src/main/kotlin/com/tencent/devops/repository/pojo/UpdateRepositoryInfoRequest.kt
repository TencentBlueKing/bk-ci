package com.tencent.devops.repository.pojo

import com.tencent.devops.repository.pojo.enums.RepoAuthType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("代码库模型-基本信息")
data class UpdateRepositoryInfoRequest(
    @ApiModelProperty("用户ID", required = false)
    val userId: String? = null,
    @ApiModelProperty("仓库名称", required = false)
    val projectName: String? = null,
    @ApiModelProperty("凭证ID", required = false)
    val credentialId: String? = null,
    @ApiModelProperty("svn认证类型", required = false)
    val svnType: String? = null,
    @ApiModelProperty("git认证类型", required = false)
    val authType: RepoAuthType? = null
)