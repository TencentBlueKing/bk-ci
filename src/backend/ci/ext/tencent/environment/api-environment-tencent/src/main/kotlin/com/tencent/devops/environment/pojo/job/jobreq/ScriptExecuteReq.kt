package com.tencent.devops.environment.pojo.job.jobreq

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "执行脚本的信息")
data class ScriptExecuteReq(
    @get:Schema(title = "脚本内容Base64")
    val scriptContent: String?,
    @get:Schema(title = "脚本执行超时时间", description = "单位：秒，默认7200秒，取值范围1-86400")
    val timeout: Long = 7200,
    @get:Schema(title = "脚本执行参数")
    val scriptParam: String?,
    @get:Schema(title = "是否执行敏感参数", description = "0：不是（默认），1：是")
    val isSensiveParam: Int = 0,
    @get:Schema(title = "脚本类型", description = "1(shell脚本)、2(bat脚本)、3(perl脚本)、4(python脚本)、5(powershell脚本)")
    val scriptLanguage: Int?,
    @get:Schema(title = "执行目标")
    val executeTarget: ExecuteTarget?,
    @get:Schema(title = "机器执行账号别名", description = "和accountId必须存在一个。同时存在时，accountId优先。")
    val accountAlias: String?,
    @get:Schema(title = "机器执行账号ID", description = "和accountAlias必须存在一个。同时存在时，accountId优先。")
    val accountId: Long?,
    @get:Schema(title = "自定义作业名称")
    val taskName: String?
)