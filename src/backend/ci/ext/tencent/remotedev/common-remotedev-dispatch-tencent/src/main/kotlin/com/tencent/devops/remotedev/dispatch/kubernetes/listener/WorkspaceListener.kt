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

package com.tencent.devops.remotedev.dispatch.kubernetes.listener

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.remotedev.dispatch.kubernetes.service.RebuildOptions
import com.tencent.devops.remotedev.dispatch.kubernetes.service.RemoteDevService
import com.tencent.devops.remotedev.dispatch.kubernetes.service.WorkspaceOperateCommonObject
import com.tencent.devops.remotedev.dispatch.kubernetes.service.factory.RemoteDevServiceFactory
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.remotedev.pojo.mq.WorkspaceOperateEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Suppress("ALL")
class WorkspaceListener @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val remoteDevService: RemoteDevService,
    private val remoteDevServiceFactory: RemoteDevServiceFactory
) {

    @BkTimed
    fun handleWorkspaceCreateSyn(event: WorkspaceCreateEvent) {
        try {
            logger.info("Start to handle workspace create ($event)")
            remoteDevService.createWorkspaceSyn(
                event = event
            )
        } catch (t: Throwable) {
            logger.error("Handle workspace create error.", t)
        }
    }

    @BkTimed
    fun handleWorkspaceOperateSyn(event: WorkspaceOperateEvent) {
        try {
            logger.info("Start to handle workspace operate ($event)")
            when (event.type) {
                UpdateEventType.START -> {
                    remoteDevServiceFactory.loadRemoteDevService(event.mountType)
                        .startWorkspace(event.userId, event.workspaceName)
                }

                UpdateEventType.STOP -> {
                    remoteDevServiceFactory.loadRemoteDevService(event.mountType)
                        .stopWorkspace(event.userId, event.workspaceName)
                }

                UpdateEventType.DELETE -> {
                    remoteDevServiceFactory.loadRemoteDevService(event.mountType)
                        .deleteWorkspace(event.userId, event)
                }

                UpdateEventType.RESTART -> {
                    remoteDevServiceFactory.loadRemoteDevService(event.mountType)
                        .restartWorkspace(event.userId, event.workspaceName)
                }

                UpdateEventType.REBUILD -> {
                    val taskUid = remoteDevServiceFactory.loadRemoteDevService(event.mountType).rebuildWorkspace(
                        userId = event.userId,
                        workspaceName = event.workspaceName,
                        imageCosFile = event.imageCosFile ?: "",
                        formatDataDisk = event.formatDataDisk
                    )
                    if (event.rebuildRemoveOwner == true) {
                        WorkspaceOperateCommonObject.saveRebuildOptions(
                            redisOperation, taskUid, RebuildOptions(true)
                        )
                        logger.debug("rebuildWorkspace|saveRebuildOptions|$taskUid")
                    }
                }

                UpdateEventType.UPGRADE -> {
                    // 需要生成一个新的 pipelineId 进行操作
                    val orderId = "${event.projectId}_${event.projectId}_${UUIDUtil.generate().takeLast(16)}"
                    remoteDevServiceFactory.loadRemoteDevService(event.mountType).upgradeWorkspaceVm(
                        userId = event.userId,
                        workspaceName = event.workspaceName,
                        machineType = event.machineType!!,
                        pipelineId = orderId
                    )
                }

                UpdateEventType.CLONE -> {
                    // 需要生成一个新的 pipelineId 进行操作
                    val orderId = "${event.projectId}_${event.projectId}_${UUIDUtil.generate().takeLast(16)}"
                    remoteDevServiceFactory.loadRemoteDevService(event.mountType).cloneWorkspaceVm(
                        userId = event.userId,
                        workspaceName = event.workspaceName,
                        pipelineId = orderId,
                        machineType = event.machineType,
                        zoneId = event.zoneId,
                        live = event.live
                    )
                }
                else -> {
                }
            }
        } catch (e: Exception) {
            logger.error("Fail to handle workspace operate ($event)", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceListener::class.java)
    }
}
