package com.tencent.devops.environment.pojo.tstack

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("TStack 构建节点")
data class TstackNode(
    @ApiModelProperty("TStack节点 Hash ID", required = true)
    var hashId: String,
    @ApiModelProperty("节点 Hash ID", required = false)
    var nodeHashId: String? = null,
    @ApiModelProperty("项目ID", required = true)
    var projectId: String,
    @ApiModelProperty("TStack 虚拟机ID", required = true)
    var tstackVmId: String,
    @ApiModelProperty("IP", required = true)
    var ip: String,
    @ApiModelProperty("节点名称", required = true)
    var name: String,
    @ApiModelProperty("节点类型", required = true)
    var nodeType: String = NodeType.TSTACK.name,
    @ApiModelProperty("节点状态", required = true)
    var nodeStatus: String = NodeStatus.NORMAL.name,
    @ApiModelProperty("操作系统", required = true)
    var os: String = OS.WINDOWS.name,
    @ApiModelProperty("操作系统版本", required = true)
    var osVersion: String,
    @ApiModelProperty("CPU", required = false)
    var cpu: String? = null,
    @ApiModelProperty("内存", required = false)
    var memory: String? = null,
    @ApiModelProperty("是否可用", required = true)
    var available: Boolean,
    @ApiModelProperty("创建时间", required = true)
    var createdTime: Long,
    @ApiModelProperty("更新时间", required = true)
    var updatedTime: Long
)