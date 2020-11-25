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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.element.TcmElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.plugin.api.ServiceTcmResource
import com.tencent.devops.plugin.pojo.tcm.TcmReqParam
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultSuccessAtomResponse
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.service.PipelineUserService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class TcmTaskAtom @Autowired constructor(
    private val pipelineUserService: PipelineUserService,
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter
)
    : IAtomTask<TcmElement> {
    private val logger = LoggerFactory.getLogger(TcmTaskAtom::class.java)

    override fun getParamElement(task: PipelineBuildTask): TcmElement {
        return JsonUtil.mapTo(task.taskParams, TcmElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: TcmElement, runVariables: Map<String, String>): AtomResponse {
        logger.info("Enter TcmTaskAtom Run...")
        val buildId = task.buildId
        val elementId = task.taskId

        val userId = if (param.startWithSaver == true) {
            val lastModifyUserMap = pipelineUserService.listUpdateUsers(setOf(task.pipelineId))
            lastModifyUserMap[task.pipelineId] ?: task.starter
        } else {
            task.starter
        }
        val appId = param.appId
        val tcmAppId = param.tcmAppId
        val templateId = param.templateId
        val taskName = parseVariable(param.name, runVariables) + "-" + System.currentTimeMillis()
        val workJson = if (param.workJson != null && param.workJson!!.isNotEmpty()) {
            val mapStr = parseVariable(JsonUtil.toJson(param.workJson!!), runVariables)
            JsonUtil.to<List<Map<String, String>>>(mapStr)
        } else {
            listOf()
        }

        val tcmReqParam = TcmReqParam(userId, appId, tcmAppId, templateId, taskName, workJson)
        buildLogPrinter.addLine(buildId, "tcm原子请求参数:\n ${tcmReqParam.beanToMap()}", elementId, task.containerHashId, task.executeCount ?: 1)
        return try {
            val pipelineId = task.pipelineId
            val lastUpdateUser = pipelineUserService.list(setOf(pipelineId)).firstOrNull()?.updateUser ?: ""
            client.get(ServiceTcmResource::class).startTask(tcmReqParam, buildId, lastUpdateUser)
            buildLogPrinter.addLine(buildId, "tcm原子执行成功", elementId, task.containerHashId, task.executeCount ?: 1)
            defaultSuccessAtomResponse
        } catch (e: Exception) {
            buildLogPrinter.addRedLine(buildId, "tcm原子执行失败:${e.message}", elementId, task.containerHashId, task.executeCount ?: 1)
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorMsg = "tcm原子执行失败:${e.message}"
            )
        }
    }
}
