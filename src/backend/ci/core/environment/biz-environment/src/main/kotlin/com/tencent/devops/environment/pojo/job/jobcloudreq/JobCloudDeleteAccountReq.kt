package com.tencent.devops.environment.pojo.job.jobcloudreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class JobCloudDeleteAccountReq(
    @ApiModelProperty(value = "帐号ID", required = true)
    val id: Long,
    @ApiModelProperty(value = "资源范围类型", notes = "biz - 业务，biz_set - 业务集", required = true)
    @JsonProperty("bk_scope_type")
    override var bkScopeType: String? = "",
    @ApiModelProperty(value = "资源范围ID", notes = "与bk_scope_type对应, 表示业务ID或者业务集ID", required = true)
    @JsonProperty("bk_scope_id")
    override var bkScopeId: String? = "",
    @ApiModelProperty(value = "当前用户用户名", required = true)
    @JsonProperty("bk_username")
    override var bkUsername: String
) : JobCloudPermission(bkScopeType, bkScopeId, bkUsername)