package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

data class ScriptStepInfo(
    @get:Schema(title = "脚本类型：1-本地脚本，2-引用业务脚本，3-引用公共脚本")
    val scriptType: Int,
    @get:Schema(title = "脚本ID")
    val scriptId: String?,
    @get:Schema(title = "脚本版本ID")
    val scriptVersionId: Long?,
    @get:Schema(title = "脚本内容")
    val scriptContent: String?,
    @get:Schema(title = "脚本语言：1-shell，2-bat，3-perl，4-python，5-powershell，6-sql")
    val scriptLanguage: String,
    @get:Schema(title = "脚本参数")
    val scriptParam: String?,
    @get:Schema(title = "脚本超时时间，单位为秒")
    val scriptTimeout: Int,
    @get:Schema(title = "执行账号")
    val account: Account,
    @get:Schema(title = "执行目标机器")
    val server: VariableServer,
    @get:Schema(title = "参数是否为敏感参数：0-不敏感，1-敏感")
    val isParamSensitive: Int,
    @get:Schema(title = "是否忽略错误：0-不忽略，1-忽略")
    val isIgnoreError: Int
)