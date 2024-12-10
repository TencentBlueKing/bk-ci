package com.tencent.devops.auth.pojo

import com.tencent.devops.auth.pojo.enum.OauthType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户Oauth授权信息")
data class UserOauthInfo(
    @get:Schema(title = "授权账号")
    val username: String,
    @get:Schema(title = "授权代码库数量")
    val repoCount: Long,
    @get:Schema(title = "创建时间")
    val createTime: Long?,
    @get:Schema(title = "授权类型")
    val type: OauthType,
    @get:Schema(title = "授权类型")
    val expired: Boolean
)
