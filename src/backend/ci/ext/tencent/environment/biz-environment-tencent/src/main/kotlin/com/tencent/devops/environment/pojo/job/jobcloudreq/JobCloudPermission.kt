package com.tencent.devops.environment.pojo.job.jobcloudreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Suppress("ALL")
abstract class JobCloudPermission(
    @get:Schema(title = "资源范围类型", description = "biz - 业务，biz_set - 业务集")
    @JsonProperty("bk_scope_type")
    open var bkScopeType: String? = "",
    @get:Schema(title = "资源范围ID", description = "与bk_scope_type对应, 表示业务ID或者业务集ID")
    @JsonProperty("bk_scope_id")
    open var bkScopeId: String? = "",
    @get:Schema(title = "当前用户用户名", required = true)
    @JsonProperty("bk_username")
    open var bkUsername: String
)