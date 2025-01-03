package com.tencent.devops.repository.pojo

import com.tencent.devops.common.api.enums.ScmType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户代码库Oauth授权信息")
data class UserOauthRepositoryInfo(
    @get:Schema(title = "授权账号")
    val username: String,
    @get:Schema(title = "授权代码库数量")
    val repoCount: Long,
    @get:Schema(title = "创建时间")
    val createTime: Long? = null,
    @get:Schema(title = "授权类型")
    val scmCode: String,
    @get:Schema(title = "是否过期")
    val expired: Boolean = false,
    @get:Schema(title = "是否已授权")
    val authorized: Boolean = true,
    @get:Schema(title = "操作人")
    val operator: String,
    @get:Schema(title = "操作人")
    val scmType: ScmType,
    @get:Schema(title = "代码库类型")
    val name: String
)
