package com.tencent.devops.environment.pojo.job.req

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("执行脚本的信息")
data class ScriptExecuteReq(
    @ApiModelProperty(value = "脚本内容Base64")
    val scriptContent: String?,
    @ApiModelProperty(value = "脚本执行超时时间", notes = "单位：秒，默认7200秒，取值范围1-86400")
    val timeout: Long = 7200,
    @ApiModelProperty(value = "脚本执行参数")
    val scriptParam: String?,
    @ApiModelProperty(value = "是否执行敏感参数", notes = "0：不是（默认），1：是")
    val isSensiveParam: Int = 0,
    @ApiModelProperty(value = "脚本类型", notes = "1(shell脚本)、2(bat脚本)、3(perl脚本)、4(python脚本)、5(powershell脚本)")
    val scriptLanguage: Int?,
    @ApiModelProperty(value = "执行目标")
    val executeTarget: ExecuteTarget?,
    @ApiModelProperty(value = "机器执行账号别名", notes = "和accountId必须存在一个。同时存在时，accountId优先。")
    val accountAlias: String?,
    @ApiModelProperty(value = "机器执行账号ID", notes = "和accountAlias必须存在一个。同时存在时，accountId优先。")
    val accountId: Long?
)