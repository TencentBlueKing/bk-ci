package com.tencent.devops.environment.pojo.job.jobcloudreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Suppress("ALL")
class JobCloudQueryJobInstanceLogsReq(
    @get:Schema(title = "资源范围类型", description = "biz - 业务，biz_set - 业务集", required = true)
    @JsonProperty("bk_scope_type")
    override var bkScopeType: String? = "",
    @get:Schema(title = "资源范围ID", description = "与bk_scope_type对应, 表示业务ID或者业务集ID", required = true)
    @JsonProperty("bk_scope_id")
    override var bkScopeId: String? = "",
    @get:Schema(title = "作业实例ID", required = true)
    @JsonProperty("job_instance_id")
    val jobInstanceId: Long,
    @get:Schema(title = "步骤实例ID", required = true)
    @JsonProperty("step_instance_id")
    val stepInstanceId: Long,
    @get:Schema(title = "主机IP信息列表", required = true)
    @JsonProperty("ip_list")
    val ipList: List<JobCloudIpInfo>?,
    @get:Schema(title = "主机ID列表", required = true)
    @JsonProperty("host_id_list")
    val hostIdList: List<Long>?,
    @get:Schema(title = "当前用户用户名", required = true)
    @JsonProperty("bk_username")
    override var bkUsername: String
) : JobCloudPermission(bkScopeType, bkScopeId, bkUsername)