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
 *
 */

package com.tencent.devops.process.trigger.mq.pacTrigger

import com.tencent.devops.common.event.dispatcher.trace.TraceEventDispatcher
import com.tencent.devops.common.event.listener.trace.BaseTraceListener
import com.tencent.devops.process.trigger.PacYamlBuildService
import com.tencent.devops.process.trigger.PacYamlResourceService
import com.tencent.devops.process.trigger.PacYamlSyncService
import com.tencent.devops.process.trigger.actions.EventActionFactory
import com.tencent.devops.process.trigger.exception.PacTriggerExceptionHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PacTriggerListener @Autowired constructor(
    traceEventDispatcher: TraceEventDispatcher,
    private val actionFactory: EventActionFactory,
    private val pacYamlResourceService: PacYamlResourceService,
    private val pacYamlSyncService: PacYamlSyncService,
    private val pacYamlBuildService: PacYamlBuildService,
    private val exceptionHandler: PacTriggerExceptionHandler
) : BaseTraceListener<BasePacYamlEvent>(
    traceEventDispatcher = traceEventDispatcher
) {
    override fun run(event: BasePacYamlEvent) {
        when (event) {
            is PacYamlEnableEvent -> {
                enablePac(projectId = event.projectId, event = event)
            }

            is PacYamlTriggerEvent -> {
                trigger(projectId = event.projectId, event = event)
            }

            else -> {
                logger.warn("PacTriggerListener|not support pac yaml event|$event")
            }
        }
    }

    private fun enablePac(projectId: String, event: PacYamlEnableEvent) {
        val action = try {
            val action = actionFactory.loadEnableEvent(
                eventStr = event.eventStr,
                actionCommonData = event.actionCommonData,
                actionContext = event.actionContext,
                actionSetting = event.actionSetting
            )
            if (action == null) {
                logger.warn("PacTriggerListener|run|$event")
                return
            }
            action
        } catch (e: Throwable) {
            logger.warn("enable pac|load|action|error", e)
            return
        }
        exceptionHandler.handle(action = action) {
            pacYamlResourceService.syncYamlPipeline(projectId = projectId, action = action)
            val ciDirId = action.data.context.ciDirId!!
            val repoHashId = action.data.setting.repoHashId
            val filePath = action.data.context.yamlFile!!.yamlPath
            pacYamlSyncService.syncSuccess(
                projectId = projectId,
                repoHashId = repoHashId,
                ciDirId = ciDirId,
                filePath = filePath
            )
        }
    }

    private fun trigger(projectId: String, event: PacYamlTriggerEvent) {
        val action = try {
            val action = actionFactory.loadByData(
                eventStr = event.eventStr,
                actionCommonData = event.actionCommonData,
                actionContext = event.actionContext,
                actionSetting = event.actionSetting
            )
            if (action == null) {
                logger.warn("PacTriggerListener|run|$event")
                return
            }
            action
        } catch (e: Throwable) {
            logger.warn("enable pac|load|action|error", e)
            return
        }
        exceptionHandler.handle(action = action) {
            pacYamlResourceService.syncYamlPipeline(projectId = projectId, action = action)
            pacYamlBuildService.start(projectId = projectId, action = action, scmType = event.scmType)
        }
    }
}
