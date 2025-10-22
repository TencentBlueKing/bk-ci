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

package com.tencent.devops.environment.pojo.thirdpartyagent

import com.tencent.devops.common.api.pojo.agent.NewHeartbeatInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "第三方构建集详情")
data class ThirdPartyAgentDetail(
    @get:Schema(title = "Agent Hash ID", required = true)
    val agentId: String,
    @get:Schema(title = "Node Hash ID", required = true)
    val nodeId: String,
    @get:Schema(title = "节点名称", required = true)
    val displayName: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "状态", required = true)
    val status: String,
    @get:Schema(title = "主机名", required = true)
    val hostname: String,
    @get:Schema(title = "操作系统 | LINUX MACOS WINDOWS", required = true)
    val os: String,
    @get:Schema(title = "操作系统", required = true)
    val osName: String,
    @get:Schema(title = "IP地址", required = true)
    val ip: String,
    @get:Schema(title = "导入人", required = true)
    val createdUser: String,
    @get:Schema(title = "导入时间", required = true)
    val createdTime: String,
    @get:Schema(title = "Agent版本", required = true)
    val agentVersion: String,
    @get:Schema(title = "Worker版本", required = true)
    val slaveVersion: String,
    @get:Schema(title = "agent安装路径", required = true)
    val agentInstallPath: String,
    @Deprecated(message = "已废弃，使用 parallelTaskCount")
    @get:Schema(title = "已废弃，使用 parallelTaskCount", required = true)
    val maxParallelTaskCount: String,
    @get:Schema(title = "最大构建并发数", required = true)
    val parallelTaskCount: String,
    @get:Schema(title = "docker构建机通道数量", required = true)
    val dockerParallelTaskCount: String,
    @get:Schema(title = "启动用户", required = true)
    val startedUser: String,
    @get:Schema(title = "agent链接", required = true)
    val agentUrl: String,
    @get:Schema(title = "agent安装脚本", required = true)
    val agentScript: String,
    @get:Schema(title = "最新心跳时间", required = true)
    val lastHeartbeatTime: String,
    @get:Schema(title = "CPU 核数", required = true)
    val ncpus: String, // nCpus 序列化JSON会变成 ncpus，但JSON反序列化对象时，nCpus字段不认ncpus
    @get:Schema(title = "内存", required = true)
    val memTotal: String,
    @get:Schema(title = "硬盘空间（最大盘）", required = true)
    val diskTotal: String,
    @get:Schema(title = "是否可以编辑", required = false)
    var canEdit: Boolean? = false,
    @get:Schema(title = "当前Agent版本", required = false)
    val currentAgentVersion: String? = "",
    @get:Schema(title = "当前Worker版本", required = false)
    val currentWorkerVersion: String? = "",
    @get:Schema(title = "心跳信息", required = false)
    var heartbeatInfo: NewHeartbeatInfo? = null,
    @get:Schema(title = "错误退出信息", required = false)
    val exitErrorMsg: String? = null
)
