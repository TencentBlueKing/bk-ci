package com.tencent.devops.environment.pojo.job.jobcloudreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class JobCloudCreateAccountReq(
    @get:Schema(title = "帐号名称", required = true)
    val account: String,
    @get:Schema(title = "账号类型", description = "1：Linux，2：Windows", required = true)
    val type: Int,
    @get:Schema(title = "账号用途", description = "1：系统账号", required = true)
    val category: Int,
    @get:Schema(title = "系统账号密码", description = "账号用途为系统账号 且 账号类型为Windows时,必传")
    val password: String?,
    @get:Schema(title = "别名", description = "不传则以账号名称作为别名")
    val alias: String?,
    @get:Schema(title = "描述")
    val description: String?,
    @get:Schema(title = "资源范围类型", description = "biz - 业务，biz_set - 业务集", required = true)
    @JsonProperty("bk_scope_type")
    override var bkScopeType: String? = "",
    @get:Schema(title = "资源范围ID", description = "与bk_scope_type对应, 表示业务ID或者业务集ID", required = true)
    @JsonProperty("bk_scope_id")
    override var bkScopeId: String? = "",
    @get:Schema(title = "当前用户用户名", required = true)
    @JsonProperty("bk_username")
    override var bkUsername: String
) : JobCloudPermission(bkScopeType, bkScopeId, bkUsername)