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
import com.tencent.devops.remotedev.dispatch.kubernetes.interfaces.ServiceStartCloudInterface
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.client.WorkspaceBcsClient
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.client.WorkspaceStartCloudClient
import com.tencent.devops.remotedev.pojo.CgsResourceConfig
import com.tencent.devops.remotedev.pojo.image.StandardVmImage
import com.tencent.devops.remotedev.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.remotedev.pojo.kubernetes.TaskStatus
import com.tencent.devops.remotedev.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.remotedev.pojo.remotedev.EnvironmentResourceData
import com.tencent.devops.remotedev.pojo.remotedev.FetchWinPoolData
import com.tencent.devops.remotedev.pojo.remotedev.ResourceVmReq
import com.tencent.devops.remotedev.pojo.remotedev.ResourceVmRespData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StartCloudService @Autowired constructor(
    private val startCloudInterfaceService: StartCloudInterfaceService,
    private val workspaceBcsClient: WorkspaceBcsClient,
    private val workspaceStartCloudClient: WorkspaceStartCloudClient
) : ServiceStartCloudInterface {

    override fun createStartCloudUser(user: String, gameId: String?): Result<Boolean> {
        return Result(startCloudInterfaceService.createStartCloudUser(user, gameId))
    }

    override fun syncStartCloudResourceList(): Result<List<EnvironmentResourceData>> {
        return Result(startCloudInterfaceService.syncStartCloudResourceList())
    }

    override fun getCgsData(data: FetchWinPoolData): Result<List<EnvironmentResourceData>> {
        if (data.cgsIds.isNullOrEmpty() && data.ips.isNullOrEmpty()) {
            return Result(listOf())
        }
        return Result(startCloudInterfaceService.getCgsData(data.cgsIds, data.ips))
    }

    override fun checkCgsRunning(cgsId: String, status: EnvStatusEnum?): Result<Boolean> {
        return Result(startCloudInterfaceService.checkCgsRunning(cgsId, status))
    }

    override fun getCgsConfig(): Result<CgsResourceConfig> {
        return Result(startCloudInterfaceService.getCgsConfig())
    }

    override fun shareWorkspace(
        operator: String,
        cgsId: String,
        gameId: String?,
        receivers: List<String>
    ): Result<String> {
        return Result(startCloudInterfaceService.shareWorkspace(operator, cgsId, receivers, gameId))
    }

    override fun unShareWorkspace(operator: String, resourceId: String, receivers: List<String>): Result<Boolean> {
        return Result(startCloudInterfaceService.unShareWorkspace(operator, resourceId, receivers))
    }

    override fun getResourceVm(data: ResourceVmReq): Result<List<ResourceVmRespData>?> {
        return Result(workspaceBcsClient.startGetResourceVm(data))
    }

    override fun getWorkspaceInfoByEid(eid: String): Result<WorkspaceInfo> {
        return Result(startCloudInterfaceService.getWorkspaceInfoByEid(eid))
    }

    override fun getTaskInfoByUid(uid: String): Result<TaskStatus?> {
        return Result(startCloudInterfaceService.getTaskInfoByUid(uid))
    }

    override fun getVmStandardImages(): Result<List<StandardVmImage>?> {
        return Result(workspaceBcsClient.startGetVmStandardImages())
    }
}
