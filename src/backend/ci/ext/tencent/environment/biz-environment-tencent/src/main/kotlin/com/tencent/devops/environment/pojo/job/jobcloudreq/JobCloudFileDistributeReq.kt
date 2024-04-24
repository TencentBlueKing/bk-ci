package com.tencent.devops.environment.pojo.job.jobcloudreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Suppress("ALL")
data class JobCloudFileDistributeReq(
    @get:Schema(title = "资源范围类型", description = "biz - 业务，biz_set - 业务集", required = true)
    @JsonProperty("bk_scope_type")
    override var bkScopeType: String? = "",
    @get:Schema(title = "资源范围ID", description = "与bk_scope_type对应, 表示业务ID或者业务集ID", required = true)
    @JsonProperty("bk_scope_id")
    override var bkScopeId: String? = "",
    @get:Schema(title = "源文件列表", required = true)
    @JsonProperty("file_source_list")
    val fileSourceList: List<JobCloudFileSource>,
    @get:Schema(title = "文件传输目标路径", required = true)
    @JsonProperty("file_target_path")
    val fileTargetPath: String,
    @get:Schema(title = "传输模式", description = "1 - 严谨模式, 2 - 强制模式, 默认2")
    @JsonProperty("transfer_mode")
    val transferMode: Int = 2,
    @get:Schema(title = "执行目标", required = true)
    @JsonProperty("target_server")
    val executeTarget: JobCloudExecuteTarget,
    @get:Schema(title = "机器执行帐号别名")
    @JsonProperty("account_alias")
    val accountAlias: String,
    @get:Schema(title = "机器执行帐号别名")
    @JsonProperty("account_id")
    val accountId: Long?,
    @get:Schema(title = "文件分发超时时间", description = "单位：秒，默认7200秒，取值范围1-86400。")
    @JsonProperty("timeout")
    val timeout: Long,
    @get:Schema(title = "自定义作业名称")
    @JsonProperty("task_name")
    val taskName: String?,
    @get:Schema(title = "当前用户用户名", required = true)
    @JsonProperty("bk_username")
    override var bkUsername: String
) : JobCloudPermission(bkScopeType, bkScopeId, bkUsername)