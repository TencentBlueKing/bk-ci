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

package com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.dispatch.kubernetes.interfaces.ServiceWorkspaceDispatchInterface
import com.tencent.devops.remotedev.dispatch.kubernetes.service.RemoteDevService
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.expert.CreateDiskResp
import com.tencent.devops.remotedev.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.remotedev.pojo.remotedev.ExpandDiskValidateResp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StartCloudWorkspaceService @Autowired constructor(
    private val remoteDevService: RemoteDevService
) : ServiceWorkspaceDispatchInterface {

    override fun getWorkspaceUrl(
        userId: String,
        workspaceName: String,
        mountType: WorkspaceMountType
    ): Result<String?> {
        return Result(remoteDevService.getWorkspaceUrl(userId, workspaceName, mountType))
    }

    override fun getWorkspaceInfo(
        userId: String,
        workspaceName: String,
        mountType: WorkspaceMountType
    ): Result<WorkspaceInfo> {
        return Result(remoteDevService.getWorkspaceInfo(userId, workspaceName, mountType))
    }

    override fun deleteWorkspace(userId: String, workspaceName: String, bakWorkspaceName: String?): Result<Boolean> {
        remoteDevService.deleteWorkspace(workspaceName, bakWorkspaceName)
        return Result(true)
    }

    override fun expandDisk(
        workspaceName: String,
        userId: String,
        size: String,
        mountType: WorkspaceMountType
    ): Result<ExpandDiskValidateResp> {
        return Result(remoteDevService.expandDisk(workspaceName, userId, size, mountType))
    }

    override fun createDisk(
        workspaceName: String,
        userId: String,
        size: String,
        mountType: WorkspaceMountType
    ): Result<CreateDiskResp> {
        return Result(remoteDevService.createDisk(workspaceName, userId, size, mountType))
    }
}
