package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class JobCloudScriptStepInfo(
    @ApiModelProperty(value = "脚本类型：1-本地脚本，2-引用业务脚本，3-引用公共脚本")
    @JsonProperty("script_source")
    val scriptSource: Int,
    @ApiModelProperty(value = "脚本ID")
    @JsonProperty("script_id")
    val scriptId: String,
    @ApiModelProperty(value = "脚本版本ID")
    @JsonProperty("script_version_id")
    val scriptVersionId: Long,
    @ApiModelProperty(value = "BASE64编码的脚本内容")
    val content: String,
    @ApiModelProperty(value = "脚本语言：1-shell，2-bat，3-perl，4-python，5-powershell，6-sql")
    @JsonProperty("script_language")
    val scriptLanguage: String,
    @ApiModelProperty(value = "脚本参数")
    @JsonProperty("script_param")
    val scriptParam: String,
    @ApiModelProperty(value = "脚本超时时间，单位为秒")
    val timeout: Int,
    @ApiModelProperty(value = "执行账号ID")
    @JsonProperty("account_id")
    val accountId: Long,
    @ApiModelProperty(value = "执行账号名称")
    @JsonProperty("account_name")
    val accountName: String,
    @ApiModelProperty(value = "执行目标机器")
    @JsonProperty("execute_target")
    val executeTarget: JobCloudVariableServer,
    @ApiModelProperty(value = "参数是否为敏感参数：0-不敏感，1-敏感")
    @JsonProperty("secure_param")
    val secureParam: Int,
    @ApiModelProperty(value = "是否忽略错误：0-不忽略，1-忽略")
    @JsonProperty("ignore_error")
    val ignoreError: Int
)