package com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * token注册信息
 */
@Schema(title = "token注册信息")
data class CoffeeAIToken(
    @get:Schema(title = "用户id", required = true)
    @JsonProperty("userId")
    val userId: String,

    @get:Schema(title = "token", required = true)
    @JsonProperty("token")
    val token: String,

    @get:Schema(title = "过期时间", required = true)
    @JsonProperty("expirationMinutes")
    val expirationMinutes: Long
)

/**
 * 工作空间注册请求
 */
@Schema(title = "工作空间注册请求")
data class WorkspaceRegistration(
    @get:Schema(title = "工作空间名称", required = true)
    @JsonProperty("workspaceName")
    val workspaceName: String,

    @get:Schema(title = "环境ID", required = true)
    @JsonProperty("envId")
    val envId: String,

    @get:Schema(title = "主机IP", required = true)
    @JsonProperty("hostIp")
    val hostIp: String,

    @get:Schema(title = "拥有者", required = true)
    @JsonProperty("owner")
    val owner: String,

    @get:Schema(title = "项目ID", required = false)
    @JsonProperty("projectId")
    val projectId: String,

    @get:Schema(title = "地域类型", required = false)
    @JsonProperty("zoneConfigType")
    val zoneConfigType: String,

    @get:Schema(title = "工作空间描述", required = false)
    @JsonProperty("description")
    val description: String? = null,

    @get:Schema(title = "标签", required = false)
    @JsonProperty("tags")
    val tags: List<String>? = null
)
