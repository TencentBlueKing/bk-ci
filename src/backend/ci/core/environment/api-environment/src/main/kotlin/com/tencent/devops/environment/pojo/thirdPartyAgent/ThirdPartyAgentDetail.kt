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

package com.tencent.devops.environment.pojo.thirdPartyAgent

import com.tencent.devops.common.api.pojo.agent.NewHeartbeatInfo
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
    @ApiModelProperty("docker构建机通道数量", required = true)
    val dockerParallelTaskCount: String,
    @ApiModelProperty("启动用户", required = true)
    val startedUser: String,
    @ApiModelProperty("agent链接", required = true)
    val agentUrl: String,
    @ApiModelProperty("agent安装脚本", required = true)
    val agentScript: String,
    @ApiModelProperty("最新心跳时间", required = true)
    val lastHeartbeatTime: String,
    @ApiModelProperty("CPU 核数", required = true)
    val ncpus: String, // nCpus 序列化JSON会变成 ncpus，但JSON反序列化对象时，nCpus字段不认ncpus
    @ApiModelProperty("内存", required = true)
    val memTotal: String,
    @ApiModelProperty("硬盘空间（最大盘）", required = true)
    val diskTotal: String,
    @ApiModelProperty("是否可以编辑", required = false)
    var canEdit: Boolean? = false,
    @ApiModelProperty("当前Agent版本", required = false)
    val currentAgentVersion: String? = "",
    @ApiModelProperty("当前Worker版本", required = false)
    val currentWorkerVersion: String? = "",
    @ApiModelProperty("心跳信息", required = false)
    var heartbeatInfo: NewHeartbeatInfo? = null
)
