package com.tencent.devops.environment.pojo.thirdPartyAgent

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("第三方构建集详情")
data class ThirdPartyAgentDetail(
    @ApiModelProperty("Agent Hash ID", required = true)
    val agentId: String,
    @ApiModelProperty("Node Hash ID", required = true)
    val nodeId: String,
    @ApiModelProperty("节点名称", required = true)
    val displayName: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("状态", required = true)
    val status: String,
    @ApiModelProperty("主机名", required = true)
    val hostname: String,
    @ApiModelProperty("操作系统 | LINUX MACOS WINDOWS", required = true)
    val os: String,
    @ApiModelProperty("操作系统", required = true)
    val osName: String,
    @ApiModelProperty("IP地址", required = true)
    val ip: String,
    @ApiModelProperty("导入人", required = true)
    val createdUser: String,
    @ApiModelProperty("导入时间", required = true)
    val createdTime: String,
    @ApiModelProperty("Agent版本", required = true)
    val agentVersion: String,
    @ApiModelProperty("Worker版本", required = true)
    val slaveVersion: String,
    @ApiModelProperty("agent安装路径", required = true)
    val agentInstallPath: String,
    @ApiModelProperty("最大通道数量", required = true)
    val maxParallelTaskCount: String,
    @ApiModelProperty("通道数量", required = true)
    val parallelTaskCount: String,
    @ApiModelProperty("启动用户", required = true)
    val startedUser: String,
    @ApiModelProperty("agent链接", required = true)
    val agentUrl: String,
    @ApiModelProperty("agent安装脚本", required = true)
    val agentScript: String,
    @ApiModelProperty("最新心跳时间", required = true)
    val lastHeartbeatTime: String,
    @ApiModelProperty("CPU 核数", required = true)
    val nCpus: String,
    @ApiModelProperty("内存", required = true)
    val memTotal: String,
    @ApiModelProperty("硬盘空间（最大盘）", required = true)
    val diskTotal: String,
    @ApiModelProperty("是否可以编辑", required = false)
    val canEdit: Boolean?
)