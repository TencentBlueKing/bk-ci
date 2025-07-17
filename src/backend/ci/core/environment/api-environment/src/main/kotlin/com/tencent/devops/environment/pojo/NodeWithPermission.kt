/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.environment.pojo

import com.tencent.devops.environment.pojo.thirdpartyagent.AgentBuildDetail
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "NodeWithPermission-节点信息(权限)")
data class NodeWithPermission(
    @get:Schema(title = "环境 HashId", required = true)
    val nodeHashId: String,
    @get:Schema(title = "节点 Id", required = true)
    val nodeId: String,
    @get:Schema(title = "节点名称", required = true)
    val name: String,
    @get:Schema(title = "IP", required = true)
    val ip: String,
    @get:Schema(title = "节点状态", required = true)
    val nodeStatus: String,
    @get:Schema(title = "节点类型", required = true)
    val nodeType: String,
    @get:Schema(title = "操作系统", required = false)
    val osName: String?,
    @get:Schema(title = "创建人", required = true)
    val createdUser: String,
    @get:Schema(title = "责任人", required = false)
    val operator: String?,
    @get:Schema(title = "备份责任人", required = false)
    val bakOperator: String?,
    @get:Schema(title = "是否可以使用", required = false)
    val canUse: Boolean?,
    @get:Schema(title = "是否可以编辑", required = false)
    val canEdit: Boolean?,
    @get:Schema(title = "是否可以删除", required = false)
    val canDelete: Boolean?,
    @get:Schema(title = "是否可以查看", required = false)
    val canView: Boolean? = true,
    @get:Schema(title = "网关地域", required = false)
    val gateway: String?,
    @get:Schema(title = "显示名称", required = false)
    val displayName: String?,
    @get:Schema(title = "创建/导入时间", required = false)
    val createTime: String?,
    @get:Schema(title = "最后修改时间", required = false)
    val lastModifyTime: String?,
    @get:Schema(title = "最后修改人", required = false)
    val lastModifyUser: String?,
    @get:Schema(title = "所属业务, 默认-1表示没有绑定业务")
    val bizId: Long? = -1,
    @get:Schema(title = "流水线Job引用数")
    val pipelineRefCount: Int? = 0,
    @get:Schema(title = "流水线Job引用数")
    val lastBuildTime: String? = "",
    @get:Schema(title = "agent状态", required = true)
    val agentStatus: Boolean,
    @get:Schema(title = "agent版本")
    val agentVersion: String?,
    @get:Schema(title = "agent hash id")
    val agentHashId: String? = "",
    @get:Schema(title = "云区域ID")
    val cloudAreaId: Long?,
    @get:Schema(title = "操作系统类型")
    val osType: String?,
    @get:Schema(title = "hostID")
    val bkHostId: Long? = null,
    @get:Schema(title = "job任务ID")
    val taskId: Long?,
    @get:Schema(title = "主机serverId")
    val serverId: Long?,
    @get:Schema(title = "机型")
    val size: String? = null,
    @get:Schema(title = "该节点所属环境名")
    val envNames: List<String>? = null,
    @get:Schema(title = "最近构建信息")
    var latestBuildDetail: AgentBuildDetail? = null,
    @get:Schema(title = "节点标签信息")
    val tags: List<NodeTag>? = null
)
