package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("文件分发的信息")
data class FileDistributeInfoReq(
    @ApiModelProperty(value = "源文件列表", required = true)
    val fileSourceList: List<FileSource>,
    @ApiModelProperty(value = "文件传输目标路径", required = true)
    private val fileTargetPath: String,
    @ApiModelProperty(value = "目标服务器", required = true)
    val targetEnv: EnvsetInfo,
    @ApiModelProperty(value = "机器执行帐号用户名", required = true)
    val userAccount: String,
    @ApiModelProperty(value = "文件分发超时时间", notes = "单位：秒，默认7200秒，取值范围1-86400。", required = false)
    private val distributeTimeout: Long = 7200
)