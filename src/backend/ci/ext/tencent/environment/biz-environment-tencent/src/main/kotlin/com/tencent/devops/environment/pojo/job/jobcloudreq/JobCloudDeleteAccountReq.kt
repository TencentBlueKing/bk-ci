package com.tencent.devops.environment.pojo.job.jobcloudreq

import com.fasterxml.jackson.annotation.JsonProperty

import io.swagger.v3.oas.annotations.media.Schema

data class JobCloudDeleteAccountReq(
    @get:Schema(title = "帐号ID", required = true)
    val id: Long,
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