package com.tencent.devops.process.pojo.trigger.artifact

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 制品事件触发用户（bkrepo webhook payload 中的 user 对象）
 */
@Schema(title = "制品事件触发用户")
@JsonIgnoreProperties(ignoreUnknown = true)
data class ArtifactEventUser(
    @get:Schema(title = "用户ID")
    val userId: String,
    @get:Schema(title = "用户名")
    val name: String,
    @get:Schema(title = "邮箱")
    val email: String? = null,
    @get:Schema(title = "电话")
    val phone: String? = null,
    @get:Schema(title = "是否锁定")
    val locked: Boolean? = null,
    @get:Schema(title = "是否管理员")
    val admin: Boolean? = null
)
