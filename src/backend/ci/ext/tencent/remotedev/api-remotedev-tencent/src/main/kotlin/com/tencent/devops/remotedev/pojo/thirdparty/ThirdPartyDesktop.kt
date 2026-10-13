package com.tencent.devops.remotedev.pojo.thirdparty

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "第三方云桌面 token 对应的信息，存于 Redis")
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
    @get:Schema(title = "是否为重装 token：基于已有 cdsId 签发，不能注册新机器")
    val reinstall: Boolean = false,
    @get:Schema(title = "分配时的拥有者，为空时使用 userId")
    val assignOwner: String? = null
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

/**
 * 注册成功后 cdsId 与注册人的长期绑定，存 Redis
 * 用于机器未分配时的重装场景：此时还没有工作空间记录，只能靠它确认机器归属
 */
data class ThirdPartyDesktopBinding(
    val userId: String,
    val projectId: String,
    val os: String,
    val zoneId: String,
    val ip: String
)
