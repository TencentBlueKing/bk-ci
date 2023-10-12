package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("执行脚本的信息")
data class ScriptExecuteReq(
    @ApiModelProperty(value = "脚本内容Base64", required = true)
    val scriptContent: String,
    @ApiModelProperty(value = "脚本执行超时时间", notes = "单位：秒，默认7200秒，取值范围1-86400")
    val timeout: Long = 7200,
    @ApiModelProperty(value = "脚本执行参数", required = true)
    val scriptParam: String,
    @ApiModelProperty(value = "是否执行敏感参数", notes = "0：不是（默认），1：是")
    val isSensiveParam: Int = 0,
    @ApiModelProperty(
        value = "脚本类型", notes = "1(shell脚本)、2(bat脚本)、3(perl脚本)、4(python脚本)、5(powershell脚本)",
        required = true
    )
    val scriptLanguage: Int,
    @ApiModelProperty(value = "执行目标", required = true)
    val executeTarget: com.tencent.devops.environment.pojo.job.ExecuteTarget,
    @ApiModelProperty(value = "机器执行帐号用户名", required = true)
    val account: String
)