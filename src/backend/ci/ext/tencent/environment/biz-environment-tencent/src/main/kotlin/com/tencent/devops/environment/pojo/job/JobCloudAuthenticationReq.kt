package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModelProperty

data class JobCloudAuthenticationReq(
    @ApiModelProperty(value = "上云job接口url", required = true)
    val url: String,
    @ApiModelProperty(value = "放在请求头中的鉴权信息", required = true)
    val bkAuthorization: String,
    @ApiModelProperty(value = "应用ID", required = true)
    val bkAppCode: String,
    @ApiModelProperty(value = "安全秘钥", required = true)
    val bkAppSecret: String,
    @ApiModelProperty(value = "资源范围类型", notes = "biz - 业务，biz_set - 业务集", required = true)
    val bkScopeType: String,
    @ApiModelProperty(value = "资源范围ID", notes = "与bk_scope_type对应, 表示业务ID或者业务集ID", required = true)
    val bkScopeId: String
)