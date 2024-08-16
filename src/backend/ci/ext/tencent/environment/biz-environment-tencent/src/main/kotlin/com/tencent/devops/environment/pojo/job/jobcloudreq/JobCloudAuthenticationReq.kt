package com.tencent.devops.environment.pojo.job.jobcloudreq

import com.tencent.devops.common.api.annotation.SkipLogField
import io.swagger.v3.oas.annotations.media.Schema

data class JobCloudAuthenticationReq(
    @get:Schema(title = "上云job接口url", required = true)
    val url: String,
    @get:Schema(title = "放在请求头中的鉴权信息", required = true)
    @SkipLogField
    val bkAuthorization: String,
    @get:Schema(title = "资源范围类型", description = "biz - 业务，biz_set - 业务集", required = true)
    val bkScopeType: String,
    @get:Schema(title = "资源范围ID", description = "与bk_scope_type对应, 表示业务ID或者业务集ID", required = true)
    val bkScopeId: String
)