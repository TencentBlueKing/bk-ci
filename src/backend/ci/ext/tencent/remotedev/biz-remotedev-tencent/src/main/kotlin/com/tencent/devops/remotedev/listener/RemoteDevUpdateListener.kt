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

import com.tencent.devops.common.event.listener.Listener
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.service.projectworkspace.MakeWorkspaceImageHandler
import com.tencent.devops.remotedev.service.projectworkspace.RebuildWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.RestartWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.StartWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.StopWorkspaceHandler
import com.tencent.devops.remotedev.service.workspace.CreateControl
import com.tencent.devops.remotedev.service.workspace.DeleteControl
import com.tencent.devops.remotedev.service.workspace.SleepControl
import com.tencent.devops.remotedev.service.workspace.StartControl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Suppress("LongParameterList")
@Component
class RemoteDevUpdateListener @Autowired constructor(
    private val createControl: CreateControl,
    private val startControl: StartControl,
    private val sleepControl: SleepControl,
    private val deleteControl: DeleteControl,
    private val startWorkspaceHandler: StartWorkspaceHandler,
    private val stopWorkspaceHandler: StopWorkspaceHandler,
    private val restartWorkspaceHandler: RestartWorkspaceHandler,
    private val makeWorkspaceImageHandler: MakeWorkspaceImageHandler,
    private val rebuildWorkspaceHandler: RebuildWorkspaceHandler
) : Listener<RemoteDevUpdateEvent> {

    @Suppress("ComplexMethod")
    override fun execute(event: RemoteDevUpdateEvent) {
        logger.info("A message is received from dispatch k8s $event")
        kotlin.runCatching {
            if (event.mountType == WorkspaceMountType.START) {
                when (event.type) {
                    UpdateEventType.CREATE -> createControl.afterCreateWorkspace(event)
                    UpdateEventType.START -> startWorkspaceHandler.startWorkspaceCallback(event)
                    UpdateEventType.STOP -> stopWorkspaceHandler.stopWorkspaceCallback(event)
                    UpdateEventType.RESTART -> restartWorkspaceHandler.restartWorkspaceCallback(event)
                    UpdateEventType.DELETE -> deleteControl.afterDeleteWorkspace(event)
                    UpdateEventType.MAKE_IMAGE -> makeWorkspaceImageHandler.makeWorkspaceImageCallback(event)
                    UpdateEventType.REBUILD -> rebuildWorkspaceHandler.rebuildWorkspaceCallback(event)
                    else -> {}
                }
                return
            }
            when (event.type) {
                UpdateEventType.CREATE -> createControl.afterCreateWorkspace(event)
                UpdateEventType.START -> startControl.afterStartWorkspace(event)
                UpdateEventType.STOP -> sleepControl.afterStopWorkspace(event)
                UpdateEventType.DELETE -> deleteControl.afterDeleteWorkspace(event)
                else -> {}
            }
        }.onFailure {
            logger.warn("RemoteDevUpdateEvent call back error", it)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteDevUpdateListener::class.java)
    }
}
