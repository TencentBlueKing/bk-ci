package com.tencent.devops.environment.pojo.job

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("文件分发的信息")
data class FileDistributeReq(
    @ApiModelProperty(value = "源文件列表", required = true)
    val fileSourceList: List<com.tencent.devops.environment.pojo.job.FileSource>,
    @ApiModelProperty(value = "文件传输目标路径", required = true)
    val fileTargetPath: String,
    @ApiModelProperty(value = "传输模式", notes = "1：严谨模式, 2：强制模式, 默认2")
    val transferMode: Int = 2,
    @ApiModelProperty(value = "执行目标", required = true)
    val executeTarget: com.tencent.devops.environment.pojo.job.ExecuteTarget,
    @ApiModelProperty(value = "机器执行帐号用户名")
    val accountAlias: String = "user00",
    @ApiModelProperty(value = "机器执行帐号ID")
    val accountId: Long?,
    @ApiModelProperty(value = "文件分发超时时间", notes = "单位：秒，默认7200秒，取值范围1-86400。")
    val timeout: Long = 7200
)