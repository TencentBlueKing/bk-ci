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

package com.tencent.devops.remotedev.listener

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.listener.Listener
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.remotedev.RemoteDevDispatcher
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.remotedev.common.WorkspaceNotifyTemplateEnum
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.event.RemoteDevReminderEvent
import com.tencent.devops.remotedev.service.RemoteDevSettingService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_NOTICE_AHEAD_OF_TIME
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Suppress("LongParameterList")
@Component
class RemoteDevReminderListener @Autowired constructor(
    private val client: Client,
    private val workspaceService: WorkspaceService,
    private val redisCacheService: RedisCacheService,
    private val remoteDevSettingService: RemoteDevSettingService,
    private val dispatcher: RemoteDevDispatcher
) : Listener<RemoteDevReminderEvent> {

    override fun execute(event: RemoteDevReminderEvent) {
        logger.info("RemoteDevReminderListener $event")
        with(event) {
            kotlin.runCatching {
                val workspace = workspaceService.getWorkspaceDetail(userId, workspaceName) ?: return
                if (!workspace.status.checkRunning()) return
                val duration = remoteDevSettingService.startCloudExperienceDuration(userId)
                val limit = redisCacheService.get(REDIS_NOTICE_AHEAD_OF_TIME)?.toLong() ?: 60
                val timeLeft = duration * 60 * 60 - workspace.usageTime
                // 给予5分钟的时间误差
                if (duration * 60 * 60 - workspace.usageTime < (limit + 5) * 60) {
                    logger.info("start notify to user $userId")
                    workspaceService.dispatchWebsocketPushEvent(
                        userId = userId,
                        workspaceName = workspaceName,
                        workspaceHost = null,
                        errorMsg = null, type = WebSocketActionType.WORKSPACE_NEED_RENEWAL,
                        status = true, action = WorkspaceAction.NEED_RENEWAL,
                        systemType = workspace.systemType, workspaceMountType = workspace.workspaceMountType
                    )
                    val request = SendNotifyMessageTemplateRequest(
                        templateCode = WorkspaceNotifyTemplateEnum.REMOTEDEV_WORKSPACE_RENEWAL_TEMPLATE.templateCode,
                        receivers = mutableSetOf(userId),
                        cc = mutableSetOf(userId),
                        titleParams = null,
                        bodyParams = mapOf(
                            "userId" to userId,
                            "workspaceName" to workspaceName
                        ),
                        notifyType = mutableSetOf(NotifyType.EMAIL.name)
                    )
                    client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
                    // 延迟到预期休眠的时间窗口再进行一次判断
                    dispatcher.dispatch(
                        this.copy(delayMills = ((limit + 10) * 60).toInt() * 1000)
                    )
                } else {
                    // 体验时长延期过，计算下一次时间窗口再进行判断
                    dispatcher.dispatch(
                        this.copy(delayMills = (timeLeft - limit * 60).coerceAtLeast(60).toInt() * 1000)
                    )
                }
            }.onFailure {
                logger.warn("RemoteDevReminderListener error", it)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteDevReminderListener::class.java)
    }
}
