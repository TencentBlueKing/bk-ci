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

package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.dispatch.devcloud.client.WorkspaceDevCloudClient
import com.tencent.devops.dispatch.devcloud.dao.DispatchWorkspaceDao
import com.tencent.devops.dispatch.devcloud.dao.DispatchWorkspaceOpHisDao
import com.tencent.devops.dispatch.devcloud.pojo.Container
import com.tencent.devops.dispatch.devcloud.pojo.EnvStatusEnum
import com.tencent.devops.dispatch.devcloud.pojo.Environment
import com.tencent.devops.dispatch.devcloud.pojo.EnvironmentAction
import com.tencent.devops.dispatch.devcloud.pojo.EnvironmentSpec
import com.tencent.devops.dispatch.devcloud.pojo.ResourceRequirements
import com.tencent.devops.dispatch.devcloud.utils.RedisUtils
import com.tencent.devops.dispatch.kubernetes.interfaces.RemoteDevInterface
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.WorkspaceReq
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DevCloudRemoteDevService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisUtils: RedisUtils,
    private val dispatchWorkspaceDao: DispatchWorkspaceDao,
    private val dispatchWorkspaceOpHisDao: DispatchWorkspaceOpHisDao,
    private val workspaceDevCloudClient: WorkspaceDevCloudClient
) : RemoteDevInterface {
    override fun createWorkspace(userId: String, workspaceReq: WorkspaceReq): Pair<String, String> {
        val environmentOpRsp = workspaceDevCloudClient.createWorkspace(userId, Environment(
            kind = "evn/v1",
            APIVersion = "",
            spec = EnvironmentSpec(
                containers = listOf(Container(
                    image = "",
                    resource = ResourceRequirements(8, 32008)
                ))
            )
        ))

        dslContext.transaction { t ->
            val context = DSL.using(t)
            dispatchWorkspaceDao.createWorkspace(
                userId = userId,
                workspace = workspaceReq,
                environmentUid = environmentOpRsp.enviromentUid!!,
                status = EnvStatusEnum.Running,
                dslContext = context
            )

            dispatchWorkspaceOpHisDao.createWorkspaceHistory(
                dslContext = context,
                workspaceName = workspaceReq.name,
                environmentUid = environmentOpRsp.enviromentUid!!,
                operator = "admin",
                action = EnvironmentAction.CREATE
            )
        }


        return Pair("", "")
    }

    override fun startWorkspace(userId: String, workspaceName: String): Boolean {
        val environmentUid = getEnvironmentUid(workspaceName)
        workspaceDevCloudClient.operatorWorkspace(
            userId = userId,
            environmentUid = environmentUid,
            workspaceName = workspaceName,
            environmentAction = EnvironmentAction.START
        )

        // 更新db状态
        dispatchWorkspaceDao.updateWorkspaceStatus(
            workspaceName = workspaceName,
            status = EnvStatusEnum.Running,
            dslContext = dslContext
        )

        return true
    }

    override fun stopWorkspace(userId: String, workspaceName: String): Boolean {
        val environmentUid = getEnvironmentUid(workspaceName)
        workspaceDevCloudClient.operatorWorkspace(
            userId = userId,
            environmentUid = environmentUid,
            workspaceName = workspaceName,
            environmentAction = EnvironmentAction.STOP
        )

        // 更新db状态
        dispatchWorkspaceDao.updateWorkspaceStatus(
            workspaceName = workspaceName,
            status = EnvStatusEnum.Stopped,
            dslContext = dslContext
        )

        return true
    }

    override fun deleteWorkspace(userId: String, workspaceName: String): Boolean {
        val environmentUid = getEnvironmentUid(workspaceName)
        workspaceDevCloudClient.operatorWorkspace(
            userId = userId,
            environmentUid = environmentUid,
            workspaceName = workspaceName,
            environmentAction = EnvironmentAction.DELETE
        )

        // 更新db状态
        dispatchWorkspaceDao.updateWorkspaceStatus(
            workspaceName = workspaceName,
            status = EnvStatusEnum.Deleted,
            dslContext = dslContext
        )

        return true
    }

    override fun getWorkspaceUrl(userId: String, workspaceName: String): String {
        TODO("Not yet implemented")
    }

    override fun workspaceHeartbeat(userId: String, workspaceName: String): Boolean {
        redisUtils.refreshHeartbeat(userId, workspaceName)
        return true
    }

    private fun getEnvironmentUid(workspaceName: String): String {
        val workspaceRecord = dispatchWorkspaceDao.getWorkspaceInfo(workspaceName, dslContext)
        return workspaceRecord?.environmentUid ?: throw RuntimeException("No devcloud environment with $workspaceName")
    }
}
