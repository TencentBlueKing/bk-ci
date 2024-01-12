/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "NodeWithPermission-节点信息(权限)")
data class NodeWithPermission(
    @Schema(name = "环境 HashId", required = true)
    val nodeHashId: String,
    @Schema(name = "节点 Id", required = true)
    val nodeId: String,
    @Schema(name = "节点名称", required = true)
    val name: String,
    @Schema(name = "IP", required = true)
    val ip: String,
    @Schema(name = "节点状态", required = true)
    val nodeStatus: String,
    @Schema(name = "agent状态", required = true)
    val agentStatus: Boolean,
    @Schema(name = "节点类型", required = true)
    val nodeType: String,
    @Schema(name = "操作系统", required = false)
    val osName: String?,
    @Schema(name = "创建人", required = true)
    val createdUser: String,
    @Schema(name = "责任人", required = false)
    val operator: String?,
    @Schema(name = "备份责任人", required = false)
    val bakOperator: String?,
    @Schema(name = "是否可以使用", required = false)
    val canUse: Boolean?,
    @Schema(name = "是否可以编辑", required = false)
    val canEdit: Boolean?,
    @Schema(name = "是否可以删除", required = false)
    val canDelete: Boolean?,
    @Schema(name = "是否可以查看", required = false)
    val canView: Boolean? = true,
    @Schema(name = "网关地域", required = false)
    val gateway: String?,
    @Schema(name = "显示名称", required = false)
    val displayName: String?,
    @Schema(name = "创建/导入时间", required = false)
    val createTime: String?,
    @Schema(name = "最后修改时间", required = false)
    val lastModifyTime: String?,
    @Schema(name = "最后修改人", required = false)
    val lastModifyUser: String?,
    @Schema(name = "所属业务, 默认-1表示没有绑定业务")
    val bizId: Long? = -1,
    @Schema(name = "流水线Job引用数")
    val pipelineRefCount: Int? = 0,
    @Schema(name = "流水线Job引用数")
    val lastBuildTime: String? = "",
    @Schema(name = "agent hash id")
    val agentHashId: String? = ""
)
