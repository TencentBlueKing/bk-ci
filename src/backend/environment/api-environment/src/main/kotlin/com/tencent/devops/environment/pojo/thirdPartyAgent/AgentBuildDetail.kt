package com.tencent.devops.environment.pojo.thirdPartyAgent

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("第三方构建机构建任务详情")
data class AgentBuildDetail(
    @ApiModelProperty("节点 Hash ID", required = true)
    val nodeId: String,
    @ApiModelProperty("Agent Hash ID", required = true)
    val agentId: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("流水线名称", required = true)
    val pipelineName: String,
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("构建号", required = true)
    val buildNumber: Int,
    @ApiModelProperty("VM_SET_ID", required = true)
    val vmSetId: String,
    @ApiModelProperty("构建任务名称", required = true)
    val taskName: String,
    @ApiModelProperty("项目ID", required = true)
    val status: String,
    @ApiModelProperty("创建时间", required = true)
    val createdTime: Long,
    @ApiModelProperty("更新时间", required = true)
    val updatedTime: Long,
    @ApiModelProperty("工作空间", required = true)
    val workspace: String,
    @ApiModelProperty("agent任务", required = false)
    val agentTask: AgentTask?
)