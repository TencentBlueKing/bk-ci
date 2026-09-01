package com.tencent.devops.ticket.pojo

import com.tencent.devops.ticket.pojo.enums.CredentialType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "凭据-基础信息")
data class CredentialBasicInfo(
    @get:Schema(title = "凭据ID", required = true)
    val credentialId: String,
    @get:Schema(title = "凭据名称", required = true)
    val credentialName: String? = null,
    @get:Schema(title = "凭据类型", required = true)
    val credentialType: CredentialType,
    @get:Schema(title = "凭据描述", required = false)
    val credentialRemark: String? = null,
    @get:Schema(title = "最后更新时间", required = true)
    val updatedTime: Long? = null,
    @get:Schema(title = "最后更新者", required = true)
    val updateUser: String? = null,
    @get:Schema(title = "创建人", required = true)
    val createUser: String? = null
)
