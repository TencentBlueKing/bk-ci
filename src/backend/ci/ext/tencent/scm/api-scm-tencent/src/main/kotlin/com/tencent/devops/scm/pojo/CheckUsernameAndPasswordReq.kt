package com.tencent.devops.scm.pojo

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.scm.enums.CodeSvnRegion
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "验证用户名密码请求")
data class CheckUsernameAndPasswordReq(
    @get:Schema(title = "项目名称")
    val projectName: String,
    @get:Schema(title = "仓库地址")
    val url: String,
    @get:Schema(title = "仓库类型")
    val type: ScmType,
    @get:Schema(title = "用户名")
    val username: String,
    @get:Schema(title = "密码")
    val password: String,
    @get:Schema(title = "token")
    val token: String,
    @get:Schema(title = "仓库区域前缀（只有svn用到）")
    val region: CodeSvnRegion?,
    @get:Schema(title = "仓库对应的用户名")
    val repoUsername: String
)
