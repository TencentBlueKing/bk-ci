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

package com.tencent.devops.dispatch.kubernetes.pojo.mq

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.remotedev.WorkspaceEvent
import com.tencent.devops.common.remotedev.MQ
import com.tencent.devops.remotedev.pojo.event.UpdateEventType

@Event(MQ.EXCHANGE_REMOTE_DEV_LISTENER_DIRECT, MQ.QUEUE_WORKSPACE_CREATE_STARTUP)
data class WorkspaceOperateEvent(
    override val userId: String,
    override val traceId: String,
    val type: UpdateEventType,
    override val workspaceName: String,
    override val delayMills: Int = 0,
    override val retryTime: Int = 0
) : WorkspaceEvent(userId, traceId, workspaceName, delayMills, retryTime)
