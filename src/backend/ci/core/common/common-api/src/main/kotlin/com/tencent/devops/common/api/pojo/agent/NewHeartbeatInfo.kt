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

package com.tencent.devops.common.api.pojo.agent

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "心跳信息模型")
data class NewHeartbeatInfo(
    @get:Schema(title = "主版本")
    val masterVersion: String,
    @get:Schema(title = "从属版本")
    val slaveVersion: String,
    @get:Schema(title = "主机名")
    val hostName: String,
    @get:Schema(title = "构建机模型")
    val agentIp: String,
    @get:Schema(title = "并行任务计数")
    val parallelTaskCount: Int,
    @get:Schema(title = "构建机安装路径")
    val agentInstallPath: String,
    @get:Schema(title = "启动者")
    val startedUser: String,
    @get:Schema(title = "第三方构建信息列表")
    var taskList: List<ThirdPartyBuildInfo>?,
    @get:Schema(title = "Agent属性信息")
    val props: AgentPropsInfo?,
    @get:Schema(title = "构建机id")
    var agentId: Long?,
    @get:Schema(title = "项目id")
    var projectId: String?,
    @get:Schema(title = "心跳时间戳")
    var heartbeatTime: Long?,
    @get:Schema(title = "忙碌运行中任务数量")
    var busyTaskSize: Int = 0,
    @get:Schema(title = "docker并行任务计数")
    val dockerParallelTaskCount: Int?,
    @get:Schema(title = "docker构建信息列表")
    var dockerTaskList: List<ThirdPartyDockerBuildInfo>?,
    @get:Schema(title = "忙碌运行docker中任务数量")
    var dockerBusyTaskSize: Int = 0,
    @get:Schema(title = "Agent退出的错误信息")
    val errorExitData: AgentErrorExitData?
) {
    companion object {
        fun dummyHeartbeat(projectId: String, agentId: Long): NewHeartbeatInfo {
            return NewHeartbeatInfo(
                masterVersion = "",
                slaveVersion = "",
                hostName = "",
                agentIp = "",
                parallelTaskCount = 0,
                agentInstallPath = "",
                startedUser = "",
                taskList = listOf(),
                props = AgentPropsInfo("", null, null, null),
                agentId = agentId,
                projectId = projectId,
                heartbeatTime = System.currentTimeMillis(),
                dockerParallelTaskCount = 0,
                dockerTaskList = listOf(),
                errorExitData = null
            )
        }
    }
}
