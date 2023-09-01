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

package com.tencent.devops.remotedev.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceStartCloudResource
import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DataTransferService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val workspaceDao: WorkspaceDao,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val workspaceSharedDao: WorkspaceSharedDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DataTransferService::class.java)
    }

    fun windowsWorkspaceDaoInit() {
        val allWindowsWorkspace = with(TWorkspace.T_WORKSPACE) {
            dslContext.selectFrom(this)
                .where(SYSTEM_TYPE.equal(WorkspaceSystemType.WINDOWS_GPU.name))
                .fetch()
        }
        allWindowsWorkspace.forEach {
            val count = workspaceWindowsDao.opCreate(
                dslContext = dslContext,
                workspaceName = it.name,
                winConfigId = it.winConfigId
            )
            if (count > 0 && WorkspaceStatus.values()[it.status].checkInUse()) {
                val res = kotlin.runCatching {
                    client.get(ServiceStartCloudResource::class)
                        .shareWorkspace(
                            operator = Constansts.ADMIN_NAME, workspaceName = it.name, receivers = listOf(it.creator)
                        ).data!!
                }.getOrNull()
                workspaceWindowsDao.updateWindowsResourceId(dslContext, it.name, res)

                val shareUsers = workspaceSharedDao.fetchWorkspaceSharedInfo(
                    dslContext = dslContext,
                    workspaceName = it.name
                ).filter { it.resourceId.isNullOrBlank() }.map { it.sharedUser }
                val shareRes = kotlin.runCatching {
                    client.get(ServiceStartCloudResource::class)
                        .shareWorkspace(
                            operator = Constansts.ADMIN_NAME, workspaceName = it.name, receivers = shareUsers
                        ).data!!
                }.getOrNull()
                workspaceSharedDao.updateResourceId(dslContext, it.name, shareUsers, shareRes)
            }
            Thread.sleep(20)
        }
    }
}
