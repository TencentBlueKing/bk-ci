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

package com.tencent.devops.dispatch.kubernetes.startcloud.resource

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceStartCloudResource
import com.tencent.devops.remotedev.pojo.image.StandardVmImage
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.EnvironmentResourceData
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.FetchWinPoolData
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.ResourceVmReq
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.ResourceVmRespData
import com.tencent.devops.dispatch.kubernetes.startcloud.service.StartCloudInterfaceService
import com.tencent.devops.dispatch.kubernetes.startcloud.client.WorkspaceStartCloudClient
import com.tencent.devops.remotedev.pojo.CgsResourceConfig
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class StartCloudServiceResourceImpl @Autowired constructor(
    private val startCloudInterfaceService: StartCloudInterfaceService,
    private val workspaceStartCloudClient: WorkspaceStartCloudClient
) : ServiceStartCloudResource {

    override fun createStartCloudUser(user: String): Result<Boolean> {
        return Result(startCloudInterfaceService.createStartCloudUser(user))
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
        receivers: List<String>
    ): Result<String> {
        return Result(startCloudInterfaceService.shareWorkspace(operator, cgsId, receivers))
    }

    override fun unShareWorkspace(operator: String, resourceId: String, receivers: List<String>): Result<Boolean> {
        return Result(startCloudInterfaceService.unShareWorkspace(operator, resourceId, receivers))
    }

    override fun getResourceVm(data: ResourceVmReq): Result<List<ResourceVmRespData>?> {
        return Result(workspaceStartCloudClient.getResourceVm(data))
    }

    override fun getWorkspaceInfoByEid(eid: String): Result<WorkspaceInfo> {
        return Result(startCloudInterfaceService.getWorkspaceInfoByEid(eid))
    }

    override fun getTaskInfoByUid(uid: String): Result<TaskStatus?> {
        return Result(startCloudInterfaceService.getTaskInfoByUid(uid))
    }

    override fun getVmStandardImages(): Result<List<StandardVmImage>?> {
        return Result(workspaceStartCloudClient.getVmStandardImages())
    }
}
