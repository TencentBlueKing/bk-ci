package com.tencent.devops.environment.pojo.job

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@Suppress("ALL")
class JobCloudQueryJobInstanceLogsReq(
    @ApiModelProperty(value = "资源范围类型", notes = "biz - 业务，biz_set - 业务集", required = true)
    @JsonProperty("bk_scope_type")
    var bkScopeType: String,
    @ApiModelProperty(value = "资源范围ID", notes = "与bk_scope_type对应, 表示业务ID或者业务集ID", required = true)
    @JsonProperty("bk_scope_id")
    var bkScopeId: String,
    @ApiModelProperty(value = "作业实例ID", required = true)
    @JsonProperty("job_instance_id")
    val jobInstanceId: Long,
    @ApiModelProperty(value = "步骤实例ID", required = true)
    @JsonProperty("step_instance_id")
    val stepInstanceId: Long,
    @ApiModelProperty(value = "主机/主机ID列表", required = true)
    @JsonProperty("ip_list")
    val hostList: List<JobCloudHost>?,
    @ApiModelProperty(value = "应用ID", required = true)
    @JsonProperty("bk_app_code")
    override var bkAppCode: String,
    @ApiModelProperty(value = "安全秘钥", required = true)
    @JsonProperty("bk_app_secret")
    override var bkAppSecret: String,
    @ApiModelProperty(value = "当前用户用户名", required = true)
    @JsonProperty("bk_username")
    override var bkUsername: String
) : JobCloudPermission(bkAppCode, bkAppSecret, bkUsername)