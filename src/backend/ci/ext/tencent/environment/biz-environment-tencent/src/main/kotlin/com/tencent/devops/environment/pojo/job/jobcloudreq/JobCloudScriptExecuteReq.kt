package com.tencent.devops.environment.pojo.job.jobcloudreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Suppress("ALL")
data class JobCloudScriptExecuteReq(
    @get:Schema(title = "资源范围类型", description = "biz - 业务，biz_set - 业务集", required = true)
    @JsonProperty("bk_scope_type")
    override var bkScopeType: String? = "",
    @get:Schema(title = "资源范围ID", description = "与bk_scope_type对应, 表示业务ID或者业务集ID", required = true)
    @JsonProperty("bk_scope_id")
    override var bkScopeId: String? = "",
    @get:Schema(title = "脚本内容Base64")
    @JsonProperty("script_content")
    val scriptContent: String?,
    @get:Schema(title = "脚本执行参数")
    @JsonProperty("script_param")
    val scriptParam: String?,
    @get:Schema(title = "脚本执行超时时间", description = "单位：秒，默认7200秒，取值范围1-86400")
    @JsonProperty("timeout")
    val timeout: Long,
    @get:Schema(title = "执行帐号别名", description = "和accountId必须存在一个。同时存在时，accountId优先。")
    @JsonProperty("account_alias")
    val accountAlias: String?,
    @get:Schema(title = "机器执行账号ID", description = "和accountAlias必须存在一个。同时存在时，accountId优先。")
    @JsonProperty("account_id")
    val accountId: Long?,
    @get:Schema(title = "是否执行敏感参数", description = "0：不是（默认），1：是")
    @JsonProperty("is_param_sensitive")
    val isParamSensitive: Int,
    @get:Schema(title = "脚本类型", description = "1(shell脚本)、2(bat脚本)、3(perl脚本)、4(python脚本)、5(powershell脚本)")
    @JsonProperty("script_language")
    val scriptLanguage: Int?,
    @get:Schema(title = "执行目标主机列表")
    @JsonProperty("target_server")
    val targetServer: JobCloudExecuteTarget?,
    @get:Schema(title = "自定义作业名称")
    @JsonProperty("task_name")
    val taskName: String?,
    @get:Schema(title = "当前用户用户名", required = true)
    @JsonProperty("bk_username")
    override var bkUsername: String
) : JobCloudPermission(bkScopeType, bkScopeId, bkUsername)