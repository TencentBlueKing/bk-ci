package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("源文件帐号信息")
data class Account(
    @ApiModelProperty(value = "源执行账号别名", notes = "可从账号页面获取，与id必须存在一个，同时存在时，id优先。")
    val alias: String?,
    @ApiModelProperty(value = "源执行帐号ID", notes = "与alias必须存在一个，同时存在时，id优先。")
    val id: Long?
)