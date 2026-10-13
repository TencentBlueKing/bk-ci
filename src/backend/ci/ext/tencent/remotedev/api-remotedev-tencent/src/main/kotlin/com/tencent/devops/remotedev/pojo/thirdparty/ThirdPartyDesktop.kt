package com.tencent.devops.remotedev.pojo.thirdparty

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "第三方云桌面 token 携带的信息")
data class ThirdPartyDesktopTokenInfo(
    @get:Schema(title = "用户ID")
    val userId: String,
    @get:Schema(title = "操作系统，如 LINUX/WINDOWS")
    val os: String,
    @get:Schema(title = "地域，如 CQ12")
    val zoneId: String,
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "过期时间戳(毫秒)")
    val expireTime: Long,
    @get:Schema(title = "随机串，保证每次生成的 token 不同")
    val nonce: String
)

@Schema(title = "第三方云桌面 token 返回")
data class ThirdPartyDesktopTokenResp(
    @get:Schema(title = "token")
    val token: String,
    @get:Schema(title = "过期时间戳(毫秒)")
    val expireTime: Long
)

@Schema(title = "第三方云桌面注册请求")
data class ThirdPartyDesktopRegisterReq(
    @get:Schema(title = "机器IP", required = true)
    val ip: String
)

@Schema(title = "第三方云桌面 cdskey 返回")
data class ThirdPartyDesktopCdsKeyResp(
    @get:Schema(title = "cdskey")
    val cdsKey: String?
)
