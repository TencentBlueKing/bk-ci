package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("执行脚本的信息")
data class ScriptExecuteEnvsetInfo(
    @ApiModelProperty("环境hashId集合", required = true)
    val envHashIds: List<String>,
    @ApiModelProperty("节点hashId集合", required = true)
    val nodeHashIds: List<String>,
    @ApiModelProperty("IP集合", required = true)
    val ipLists: List<ScriptExecuteIPInfo>
)