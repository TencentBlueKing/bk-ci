package com.tencent.devops.environment.pojo.job.jobCloudReq

import com.tencent.devops.common.api.annotation.SkipLogField
import io.swagger.annotations.ApiModelProperty

data class JobCloudAuthenticationReq(
    @ApiModelProperty(value = "上云job接口url", required = true)
    val url: String,
    @ApiModelProperty(value = "放在请求头中的鉴权信息", required = true)
    @SkipLogField
    val bkAuthorization: String,
    @ApiModelProperty(value = "资源范围类型", notes = "biz - 业务，biz_set - 业务集", required = true)
    val bkScopeType: String,
    @ApiModelProperty(value = "资源范围ID", notes = "与bk_scope_type对应, 表示业务ID或者业务集ID", required = true)
    val bkScopeId: String
)