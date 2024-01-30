package com.tencent.devops.environment.pojo.job.resp

import io.swagger.annotations.ApiModelProperty

data class ScriptStepInfo(
    @ApiModelProperty(value = "脚本类型：1-本地脚本，2-引用业务脚本，3-引用公共脚本")
    val scriptType: Int,
    @ApiModelProperty(value = "脚本ID")
    val scriptId: String?,
    @ApiModelProperty(value = "脚本版本ID")
    val scriptVersionId: Long?,
    @ApiModelProperty(value = "脚本内容")
    val scriptContent: String?,
    @ApiModelProperty(value = "脚本语言：1-shell，2-bat，3-perl，4-python，5-powershell，6-sql")
    val scriptLanguage: String,
    @ApiModelProperty(value = "脚本参数")
    val scriptParam: String?,
    @ApiModelProperty(value = "脚本超时时间，单位为秒")
    val scriptTimeout: Int,
    @ApiModelProperty(value = "执行账号")
    val account: Account,
    @ApiModelProperty(value = "执行目标机器")
    val server: VariableServer,
    @ApiModelProperty(value = "参数是否为敏感参数：0-不敏感，1-敏感")
    val isParamSensitive: Int,
    @ApiModelProperty(value = "是否忽略错误：0-不忽略，1-忽略")
    val isIgnoreError: Int
)