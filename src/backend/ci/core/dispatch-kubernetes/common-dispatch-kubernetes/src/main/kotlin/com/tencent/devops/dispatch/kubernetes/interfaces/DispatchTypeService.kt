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
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildImageReq
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildStatusResp
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchJobLogResp
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchJobReq
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchTaskResp
import com.tencent.devops.dispatch.kubernetes.pojo.debug.DispatchBuilderDebugStatus
import com.tencent.devops.dispatch.kubernetes.pojo.debug.DispatchDebugOperateBuilderParams
import com.tencent.devops.dispatch.kubernetes.pojo.debug.DispatchDebugTaskStatus

/**
 * 用来获取不同类型的dispatchType的service来调用相关实现
 * 注：仅在接口层相关接口使用此类，构建层使用
 * @see com.tencent.devops.dispatch.common.interfaces.DispatchBuildTypeService
 */
interface DispatchTypeService {

    val slaveEnv: String

    /**
     * 创建一次执行的Job
     * @param userId 用户ID
     * @param jobReq 请求参数
     */
    fun createJob(
        userId: String,
        jobReq: DispatchJobReq
    ): DispatchTaskResp

    /**
     * 获取Job执行状态
     * @param jobName Job名称
     */
    fun getJobStatus(userId: String, jobName: String): DispatchBuildStatusResp

    /**
     * 获取Job日志
     * @param sinceTime 开始时间
     */
    fun getJobLogs(userId: String, jobName: String, sinceTime: Int?): DispatchJobLogResp

    /**
     * 获取Task任务状态
     * @param taskId 任务ID
     */
    fun getTaskStatus(userId: String, taskId: String): DispatchBuildStatusResp

    /**
     * 获取登录调试构建机状态详情
     */
    fun getDebugBuilderStatus(
        buildId: String,
        vmSeqId: String,
        userId: String,
        builderName: String,
        retryTime: Int = 3
    ): Result<DispatchBuilderDebugStatus>

    /**
     * 操作构建机
     * @param param 操作构建机需要的参数
     * @return 任务ID
     */
    fun operateDebugBuilder(
        buildId: String,
        vmSeqId: String,
        userId: String,
        builderName: String,
        param: DispatchDebugOperateBuilderParams
    ): String

    /**
     * 等待任务结束
     */
    fun waitDebugTaskFinish(
        userId: String,
        taskId: String
    ): DispatchDebugTaskStatus

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
