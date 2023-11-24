package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.environment.pojo.job.resp.Account
import io.swagger.annotations.ApiModelProperty

data class JobCloudScriptStepInfo(
    @ApiModelProperty(value = "脚本类型：1-本地脚本，2-引用业务脚本，3-引用公共脚本")
    @JsonProperty("script_type")
    val scriptType: Int,
    @ApiModelProperty(value = "脚本ID")
    @JsonProperty("script_id")
    val scriptId: String?,
    @ApiModelProperty(value = "脚本版本ID")
    @JsonProperty("script_version_id")
    val scriptVersionId: Long?,
    @ApiModelProperty(value = "脚本内容")
    @JsonProperty("script_content")
    val scriptContent: String?,
    @ApiModelProperty(value = "脚本语言：1-shell，2-bat，3-perl，4-python，5-powershell，6-sql")
    @JsonProperty("script_language")
    val scriptLanguage: String,
    @ApiModelProperty(value = "脚本参数")
    @JsonProperty("script_param")
    val scriptParam: String,
    @ApiModelProperty(value = "脚本超时时间，单位为秒")
    @JsonProperty("script_timeout")
    val scriptTimeout: Int,
    @ApiModelProperty(value = "执行账号")
    val account: JobCloudAccount,
    @ApiModelProperty(value = "执行目标机器")
    @JsonProperty("execute_target")
    val server: JobCloudVariableServer,
    @ApiModelProperty(value = "参数是否为敏感参数：0-不敏感，1-敏感")
    @JsonProperty("is_param_sensitive")
    val isParamSensitive: Int,
    @ApiModelProperty(value = "是否忽略错误：0-不忽略，1-忽略")
    @JsonProperty("is_ignore_error")
    val isIgnoreError: Int
)