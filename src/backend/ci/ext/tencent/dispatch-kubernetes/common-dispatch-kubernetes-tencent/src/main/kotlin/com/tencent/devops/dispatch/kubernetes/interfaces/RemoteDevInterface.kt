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

import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceCreateEvent

/**
 * 用来获取不同类型的dispatchType的service来调用相关实现
 * 远程开发相关接口
 * 注：仅在接口层相关接口使用此类，构建层使用
 */
interface RemoteDevInterface {

    /**
     * 创建远程工作空间
     */
    fun createWorkspace(userId: String, event: WorkspaceCreateEvent): Pair<String, String>

    /**
     * 启动远程工作空间
     */
    fun startWorkspace(userId: String, workspaceName: String): Boolean

    /**
     * 停止远程工作空间
     */
    fun stopWorkspace(userId: String, workspaceName: String): Boolean

    /**
     * 删除远程工作空间
     */
    fun deleteWorkspace(userId: String, workspaceName: String): Boolean

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
}
