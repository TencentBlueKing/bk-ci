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

package com.tencent.devops.common.event.pojo.pipeline

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.enums.PipelineBuildStatusBroadCastEventType
import com.tencent.devops.common.stream.constants.StreamBinding
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 构建状态的广播事件，用于通知等
 */
@Event(destination = StreamBinding.PIPELINE_BUILD_CALL_BACK_FANOUT)
data class PipelineBuildStatusBroadCastEvent(
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    override val userId: String,
    val buildId: String,
    val stageId: String? = null,
    val containerHashId: String? = null,
    val jobId: String? = null,
    val taskId: String? = null,
    val stepId: String? = null,
    val executeCount: Int?,
    val buildStatus: String?,
    val atomCode: String? = null,
    val eventTime: LocalDateTime? = LocalDateTime.now(),
    val type: PipelineBuildStatusBroadCastEventType? = null,
    val labels: Map<String, Any?>? = null,
    override var actionType: ActionType,
    override var delayMills: Int = 0
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills) {
    // 用于标记 labels 维度里的系统属性
    data class Labels(
        @Schema(title = "构建开始时间戳")
        val startTime: Long,
        @Schema(title = "构建总耗时(毫秒)")
        val duration: Long,
        @Schema(title = "排队耗时(毫秒)")
        val queueDuration: Long,
        @Schema(title = "审核耗时(毫秒)")
        val reviewDuration: Long,
        @Schema(title = "执行耗时(毫秒)")
        val executeDuration: Long,
        @Schema(title = "系统耗时(毫秒)")
        val systemDuration: Long,
        @Schema(title = "流水线名称")
        val pipelineName: String,
        @Schema(title = "阶段名称")
        val stageName: String,
        @Schema(title = "阶段Seq")
        val stageSeq: Int,
        @Schema(title = "作业名称")
        val jobName: String,
        @Schema(title = "步骤名称")
        val stepName: String,
        @Schema(title = "特殊步骤")
        val specialStep: String,
        @Schema(title = "触发方式")
        val trigger: String,
        @Schema(title = "触发用户")
        val triggerUser: String,
        @Schema(title = "Git仓库URL列表")
        val gitRepoUrls: String,
        @Schema(title = "Git仓库类型列表")
        val gitTypes: String,
        @Schema(title = "Git分支名称列表")
        val gitBranchNames: String,
        @Schema(title = "节点类型")
        val nodeType: String,
        @Schema(title = "节点名称")
        val hostName: String,
        @Schema(title = "节点操作系统类型")
        val hostOS: String,
        @Schema(title = "节点IP地址")
        val hostIp: String,
        @Schema(title = "job级别互斥锁定类型")
        val jobMutexType: String,
        @Schema(title = "job互斥锁定值")
        val mutexGroup: String,
        @Schema(title = "agent互斥锁定值")
        val agentReuseMutex: String,
        @Schema(title = "agent hash id")
        val agentId: String,
        @Schema(title = "环境hash id")
        val envHashId: String,
        @Schema(title = "环境名称")
        val envName: String,
        @Schema(title = "节点hash id")
        val nodeHashId: String,
        @Schema(title = "节点名称")
        val nodeName: String,
        @Schema(title = "镜像ID")
        val imageCode: String,
        @Schema(title = "镜像版本")
        val imageVersion: String,
        @Schema(title = "系统版本")
        val systemVersion: String,
        @Schema(title = "xcode版本")
        val xcodeVersion: String,
        @Schema(title = "docker容器名称")
        val dockerContainerName: String,
        @Schema(title = "docker镜像")
        val dockerImage: String,
        @Schema(title = "macos ip")
        val macosIp: String,
        @Schema(title = "windows ip")
        val windowsIp: String,
        @Schema(title = "失败时的错误码")
        val errorCode: Int,
        @Schema(title = "失败时的错误类型")
        val errorType: String,
        @Schema(title = "失败时的错误信息")
        val errorMessage: String,
        @Schema(title = "job层面属性")
        val dispatchType: String,
        @Schema(title = "job层面属性")
        val dispatchIdentity: String,
        @Schema(title = "job层面属性")
        val dispatchName: String
    )
}
