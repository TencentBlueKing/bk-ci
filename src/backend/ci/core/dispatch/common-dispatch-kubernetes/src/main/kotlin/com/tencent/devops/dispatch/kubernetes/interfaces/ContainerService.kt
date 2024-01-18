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

package com.tencent.devops.dispatch.kubernetes.interfaces

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.dispatch.kubernetes.pojo.DispatchBuildLog
import com.tencent.devops.dispatch.kubernetes.pojo.Pool
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildImageReq
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildStatusResp
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchTaskResp
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildBuilderStatus
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildOperateBuilderParams
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.debug.DispatchBuilderDebugStatus

/**
 * 用来获取不同类型的dispatchType的container service来调用相关实现
 * 注：仅在构建相关接口使用使用此类
 */
interface ContainerService {
    // 停止构建锁
    val shutdownLockBaseKey: String

    // 平台不同的配置信息
    var cpu: Double
    var memory: String
    var disk: String
    val entrypoint: String
    val sleepEntrypoint: String

    // 平台错误帮助链接
    val helpUrl: String?

    /**
     * 启动构建时打印的相关日志
     */
    fun getLog(): DispatchBuildLog

    /**
     * 获取构建机状态
     */
    fun getBuilderStatus(
        buildId: String,
        vmSeqId: String,
        userId: String,
        builderName: String,
        retryTime: Int = 3
    ): Result<DispatchBuildBuilderStatus>

    /**
     * 操作构建机
     * @param param 操作构建机需要的参数
     * @return 任务ID
     */
    fun operateBuilder(
        buildId: String,
        vmSeqId: String,
        userId: String,
        builderName: String,
        param: DispatchBuildOperateBuilderParams
    ): String

    /**
     * 创建并启动构建机
     * @return 任务ID,构建机名称
     */
    fun createAndStartBuilder(
        dispatchMessages: DispatchMessage,
        containerPool: Pool,
        poolNo: Int,
        cpu: Double,
        mem: String,
        disk: String
    ): Pair<String, String>

    /**
     * 启动构建机
     * @return 任务ID
     */
    fun startBuilder(
        dispatchMessages: DispatchMessage,
        builderName: String,
        poolNo: Int,
        cpu: Double,
        mem: String,
        disk: String
    ): String

    /**
     * 等待任务结束
     */
    fun waitTaskFinish(
        userId: String,
        taskId: String
    ): DispatchBuildTaskStatus

    /**
     * 获取Task任务状态
     * @param taskId 任务ID
     */
    fun getTaskStatus(userId: String, taskId: String): DispatchBuildStatusResp

    /**
     * 等待构建机启动
     */
    fun waitDebugBuilderRunning(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        userId: String,
        builderName: String
    ): DispatchBuilderDebugStatus

    /**
     * 获取Websocket链接
     */
    fun getDebugWebsocketUrl(
        projectId: String,
        pipelineId: String,
        staffName: String,
        builderName: String
    ): String

    /**
     * 构建推送镜像接口
     */
    fun buildAndPushImage(
        userId: String,
        projectId: String,
        buildId: String,
        dispatchBuildImageReq: DispatchBuildImageReq
    ): DispatchTaskResp
}
