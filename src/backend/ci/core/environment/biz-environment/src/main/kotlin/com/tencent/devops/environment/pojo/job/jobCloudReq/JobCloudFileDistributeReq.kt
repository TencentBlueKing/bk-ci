package com.tencent.devops.environment.pojo.job.jobCloudReq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@Suppress("ALL")
data class JobCloudFileDistributeReq(
    @ApiModelProperty(value = "资源范围类型", notes = "biz - 业务，biz_set - 业务集", required = true)
    @JsonProperty("bk_scope_type")
    override var bkScopeType: String? = "",
    @ApiModelProperty(value = "资源范围ID", notes = "与bk_scope_type对应, 表示业务ID或者业务集ID", required = true)
    @JsonProperty("bk_scope_id")
    override var bkScopeId: String? = "",
    @ApiModelProperty(value = "源文件列表", required = true)
    @JsonProperty("file_source_list")
    val fileSourceList: List<JobCloudFileSource>,
    @ApiModelProperty(value = "文件传输目标路径", required = true)
    @JsonProperty("file_target_path")
    val fileTargetPath: String,
    @ApiModelProperty(value = "传输模式", notes = "1 - 严谨模式, 2 - 强制模式, 默认2")
    @JsonProperty("transfer_mode")
    val transferMode: Int = 2,
    @ApiModelProperty(value = "执行目标", required = true)
    @JsonProperty("target_server")
    val executeTarget: JobCloudExecuteTarget,
    @ApiModelProperty(value = "机器执行帐号别名")
    @JsonProperty("account_alias")
    val accountAlias: String,
    @ApiModelProperty(value = "机器执行帐号别名")
    @JsonProperty("account_id")
    val accountId: Long?,
    @ApiModelProperty(value = "文件分发超时时间", notes = "单位：秒，默认7200秒，取值范围1-86400。")
    @JsonProperty("timeout")
    val timeout: Long,
    @ApiModelProperty(value = "当前用户用户名", required = true)
    @JsonProperty("bk_username")
    override var bkUsername: String
) : JobCloudPermission(bkScopeType, bkScopeId, bkUsername)