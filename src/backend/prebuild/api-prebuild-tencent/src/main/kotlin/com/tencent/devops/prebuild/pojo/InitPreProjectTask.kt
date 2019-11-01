package com.tencent.devops.prebuild.pojo

import com.tencent.devops.prebuild.pojo.enums.TaskStatus
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("初始化项目任务")
data class InitPreProjectTask(
    @ApiModelProperty("任务ID")
    val taskId: String,
    @ApiModelProperty("用户prebuild项目ID")
    val preProjectId: String,
    @ApiModelProperty("用户项目ID")
    val projectId: String,
    @ApiModelProperty("工作空间")
    val workspace: String,
    @ApiModelProperty("rsync账号")
    val account: String,
    @ApiModelProperty("password")
    var password: String,
    @ApiModelProperty("IP")
    var ip: String,
    @ApiModelProperty("taskStatus")
    var taskStatus: TaskStatus,
    @ApiModelProperty("logs")
    var logs: List<String>,
    @ApiModelProperty("userId")
    val userId: String
)
