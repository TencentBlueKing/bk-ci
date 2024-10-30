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

package com.tencent.devops.remotedev.pojo.mq

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.remotedev.RemoteDevMQ
import com.tencent.devops.common.remotedev.WorkspaceEvent
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.remotedev.Devfile
import io.swagger.v3.oas.annotations.media.Schema

@Event(RemoteDevMQ.WORKSPACE_CREATE_STARTUP)
data class WorkspaceCreateEvent(
    override val userId: String,
    override val traceId: String,
    override val workspaceName: String,
    @get:Schema(title = "dev file 详情")
    val devFile: Devfile,
    val mountType: WorkspaceMountType = WorkspaceMountType.START,
    @get:Schema(title = "工作空间归属")
    val ownerType: WorkspaceOwnerType? = WorkspaceOwnerType.PERSONAL,
    @get:Schema(title = "projectId")
    val projectId: String? = null,
    @get:Schema(title = "appName")
    val appName: String?,
    @get:Schema(title = "gameId")
    val gameId: Long?,
    override var delayMills: Int = 0,
    override var retryTime: Int = 0
) : WorkspaceEvent(userId, traceId, workspaceName, delayMills, retryTime)
