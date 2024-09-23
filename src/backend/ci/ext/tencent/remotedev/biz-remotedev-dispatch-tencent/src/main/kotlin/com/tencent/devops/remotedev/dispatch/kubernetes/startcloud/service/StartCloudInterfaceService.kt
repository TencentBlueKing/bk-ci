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

import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.client.WorkspaceBcsClient
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.client.WorkspaceStartCloudClient
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo.EnvironmentOperate
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo.EnvironmentShare
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo.EnvironmentUnShare
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo.EnvironmentUserCreate
import com.tencent.devops.remotedev.dispatch.kubernetes.utils.WorkspaceDispatchException
import com.tencent.devops.remotedev.dispatch.kubernetes.utils.WorkspaceRedisUtils
import com.tencent.devops.remotedev.pojo.kubernetes.TaskStatus
import com.tencent.devops.remotedev.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.remotedev.pojo.remotedev.EnvironmentResourceData
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("startcloudInterfaceService")
class StartCloudInterfaceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val workspaceClient: WorkspaceStartCloudClient,
    private val workspaceBcsClient: WorkspaceBcsClient,
    private val workspaceRedisUtils: WorkspaceRedisUtils
) {
    @Value("\${startCloud.appName}")
    val contentProviderName: String = "IEG_BKCI"

    companion object {
        private val logger = LoggerFactory.getLogger(StartCloudInterfaceService::class.java)
    }

    fun createStartCloudUser(userId: String, gameId: String?): Boolean {
        kotlin.runCatching {
            workspaceClient.createUser(
                userId,
                EnvironmentUserCreate(userId, contentProviderName, checkNotNull(gameId))
            )
        }.onFailure {
            logger.warn("create user failed.|${it.message}")
            if (it is WorkspaceDispatchException) {
                throw it
            }
        }
        return true
    }

    fun shareWorkspace(
        userId: String,
        cgsId: String,
        receivers: List<String>,
        gameId: String?
    ): String {
        receivers.forEach {
            createStartCloudUser(it, gameId)
        }
        return workspaceClient.shareWorkspace(
            userId,
            EnvironmentShare(
                cgsId = cgsId,
                expireTime = 0,
                receivers = receivers,
                sharer = userId
            )
        )
    }

    fun unShareWorkspace(userId: String, resourceId: String, receivers: List<String>): Boolean {
        return workspaceClient.unShareWorkspace(
            userId,
            EnvironmentUnShare(
                resourceId = resourceId,
                receivers = receivers, unSharer = userId
            )
        )
    }

    fun getWorkspaceInfoByEid(eid: String): WorkspaceInfo {
        val workspaceStatus = workspaceBcsClient.startGetWorkspaceInfo(
            userId = "admin",
            environmentOperate = EnvironmentOperate(eid)
        )
        return WorkspaceInfo(
            status = workspaceStatus.status,
            hostIP = workspaceStatus.hostIP,
            environmentIP = workspaceStatus.environmentIP,
            clusterId = workspaceStatus.clusterId ?: "",
            namespace = workspaceStatus.namespace ?: "",
            environmentHost = workspaceStatus.environmentIP,
            ready = true,
            started = true
        )
    }

    fun getTaskInfoByUid(uid: String): TaskStatus? {
        return workspaceRedisUtils.getTaskStatus(uid)
    }

    // 同步更新云桌面资源池列表
    fun realtimeStartCloudResourceList(): List<EnvironmentResourceData> {
        val resList = mutableListOf<EnvironmentResourceData>()
        val cgs = workspaceBcsClient.startListCgs()
        cgs.forEach {
            resList.add(
                EnvironmentResourceData(
                    cgsId = it.cgsData.cgsId,
                    cgsIp = it.cgsData.cgsIp,
                    zoneId = it.cgsData.zoneId,
                    machineType = it.cgsData.machineType,
                    status = it.cgsData.status,
                    userInstanceList = it.cgsData.userInstanceList,
                    locked = it.basic?.needLock,
                    projectId = it.basic?.projectId ?: "",
                    disk = it.pvcs?.firstOrNull { pvc -> pvc.pvcClass == "ssd" }?.pvcSize,
                    hdisk = it.pvcs?.firstOrNull { pvc -> pvc.pvcClass == "hdd" }?.pvcSize,
                    imageStandard = it.basic?.imageStandard,
                    node = it.basic?.node,
                    image = it.basic?.image,
                    cpu = it.basic?.cpuCores.toString(),
                    mem = it.basic?.memoryLimit,
                    registerCgsTime = null,
                    internal = it.basic?.internal,
                    macAddress = it.basic?.macAddress,
                    vmStatus = it.basic?.status
                )
            )
        }
        logger.debug("syncStartCloudResourceList|resourceList|{}", resList)
        return resList
    }

    // 获取cgs信息
    fun getCgsData(
        cgsIds: List<String>?,
        ips: List<String>?
    ): List<EnvironmentResourceData> {
        val condition = when {
            !cgsIds.isNullOrEmpty() && !ips.isNullOrEmpty() ->
                fun(id: String, ip: String) = id in cgsIds && ip in ips

            !cgsIds.isNullOrEmpty() ->
                fun(id: String, _: String) = id in cgsIds

            !ips.isNullOrEmpty() ->
                fun(_: String, ip: String) = ip in ips

            else -> return emptyList()
        }
        logger.info("getCgsData|$cgsIds|$ips")
        return realtimeStartCloudResourceList().filter {
            condition(it.cgsId, it.cgsIp)
        }
    }
}
