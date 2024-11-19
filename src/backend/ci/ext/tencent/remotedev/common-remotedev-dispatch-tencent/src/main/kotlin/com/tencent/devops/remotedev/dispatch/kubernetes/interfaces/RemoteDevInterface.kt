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

package com.tencent.devops.remotedev.dispatch.kubernetes.interfaces

import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.CreateWorkspaceRes
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.DispatchBuildTaskStatus
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.pojo.expert.CreateDiskResp
import com.tencent.devops.remotedev.pojo.kubernetes.TaskStatus
import com.tencent.devops.remotedev.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.remotedev.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.remotedev.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.remotedev.pojo.remotedev.ExpandDiskValidateResp

/**
 * 用来获取不同类型的dispatchType的service来调用相关实现
 * 远程开发相关接口
 * 注：仅在接口层相关接口使用此类，构建层使用
 */
interface RemoteDevInterface {

    /**
     * 创建远程工作空间
     */
    fun createWorkspace(userId: String, event: WorkspaceCreateEvent): CreateWorkspaceRes

    /**
     * 启动远程工作空间
     */
    fun startWorkspace(userId: String, workspaceName: String): String

    /**
     * 停止远程工作空间
     */
    fun stopWorkspace(userId: String, workspaceName: String): String

    /**
     * 重启远程工作空间
     */
    fun restartWorkspace(userId: String, workspaceName: String): String

    /**
     * 重装工作空间系统
     */
    fun rebuildWorkspace(userId: String, workspaceName: String, imageCosFile: String, formatDataDisk: Boolean?): String

    /**
     * 删除远程工作空间
     */
    fun deleteWorkspace(userId: String, event: WorkspaceOperateEvent): String

    /**
     * 制作工作空间镜像
     */
    fun makeWorkspaceImage(
        userId: String,
        workspaceName: String,
        gameId: String,
        cgsId: String,
        imageId: String
    ): String

    /**
     * 工作空间机型转换
     */
    fun upgradeWorkspaceVm(userId: String, workspaceName: String, machineType: String, pipelineId: String): String

    /**
     * 工作空间克隆
     */
    fun cloneWorkspaceVm(
        userId: String,
        workspaceName: String,
        pipelineId: String,
        machineType: String?,
        zoneId: String?,
        live: Boolean?
    ): String

    /**
     * 获取工作空间web端链接
     */
    fun getWorkspaceUrl(userId: String, workspaceName: String): String?

    /**
     * 工作空间task任务回调
     */
    fun workspaceTaskCallback(taskStatus: TaskStatus): Boolean

    /**
     * 查询工作空间状态
     */
    fun getWorkspaceInfo(userId: String, workspaceName: String): WorkspaceInfo

    /**
     * 等待任务结束
     */
    fun waitTaskFinish(
        userId: String,
        taskId: String,
        type: UpdateEventType
    ): DispatchBuildTaskStatus

    fun expandDisk(
        workspaceName: String,
        userId: String,
        size: String
    ): ExpandDiskValidateResp

    fun createDisk(
        workspaceName: String,
        userId: String,
        size: String
    ): CreateDiskResp
}
