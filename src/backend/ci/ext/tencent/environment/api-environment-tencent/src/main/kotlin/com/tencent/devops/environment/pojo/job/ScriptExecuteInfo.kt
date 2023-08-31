package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("执行脚本的信息")
data class ScriptExecuteInfo(
    @ApiModelProperty("内容", required = true)
    val scriptContent: String,
    @ApiModelProperty("超时时间", required = true)
    val scriptTimeout: Long,
    @ApiModelProperty("执行参数", required = true)
    val scriptParam: String,
    @ApiModelProperty("执行敏感参数", required = true)
    val sensiveParam: Int,
    @ApiModelProperty("类型:1(shell脚本)、2(bat脚本)、3(perl脚本)、4(python脚本)、5(powershell脚本)", required = true)
    val scriptType: Int,
    @ApiModelProperty("环境集合", required = true)
    val envsetInfo: ScriptExecuteEnvsetInfo,
    @ApiModelProperty("机器帐号用户名", required = true)
    val userAccount: String
)