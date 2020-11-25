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

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BSAuthResourceApi
import com.tencent.devops.common.auth.code.VSAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.plugin.api.ServiceJinGangAppResource
import com.tencent.devops.common.pipeline.element.JinGangAppElement
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.File

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class JinGangAppTaskAtom @Autowired constructor(
    private val bkAuthResourceApi: BSAuthResourceApi,
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter,
    private val serviceCode: VSAuthServiceCode
) : IAtomTask<JinGangAppElement> {

    override fun getParamElement(task: PipelineBuildTask): JinGangAppElement {
        return JsonUtil.mapTo(task.taskParams, JinGangAppElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: JinGangAppElement, runVariables: Map<String, String>): AtomResponse {
        val srcType = parseVariable(param.srcType, runVariables)
        val files = parseVariable(param.files, runVariables)
        val runType = parseVariable(param.runType, runVariables)

        val buildId = task.buildId
        val projectId = task.projectId
        val pipelineId = task.pipelineId
        val taskId = task.taskId
        val containerId = task.containerHashId
        val userId = task.starter
        val buildNo = runVariables[PIPELINE_BUILD_NUM]?.toInt() ?: 0

        val isCustom = when (srcType) {
            "PIPELINE" -> false
            "CUSTOMIZE" -> true
            else -> return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = "unsupported srcType : $srcType"
            )
        }
        files.split(",").map { it.trim() }.forEach {
            buildLogPrinter.addLine(buildId, "jin gang start scan file: $it", taskId, containerId, task.executeCount ?: 1)
            val path = if (isCustom) "/${it.removePrefix("/")}" else "/$pipelineId/$buildId/${it.removePrefix("/")}"
            val data = client.get(ServiceJinGangAppResource::class).scanApp(userId, projectId, pipelineId, buildId, buildNo, taskId, path, isCustom, runType)
            val resourceName = File(it).name + "($buildNo)"
            val jinGangTaskId = data.data ?: throw TaskExecuteException(
                errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                errorType = ErrorType.USER,
                errorMsg = "task id is null for ($it)"
            )
            // 权限中心注册资源
            bkAuthResourceApi.createResource(userId, serviceCode, AuthResourceType.SCAN_TASK,
                    projectId, HashUtil.encodeLongId(jinGangTaskId.toLong()), resourceName)

            if (data.status != 0) {
                buildLogPrinter.addLine(buildId, "jin gang fail: $data", taskId, containerId, task.executeCount ?: 1)
            } else {
                buildLogPrinter.addLine(buildId, "jin gang success: $data", taskId, containerId, task.executeCount ?: 1)
            }
        }
        return AtomResponse(BuildStatus.SUCCEED)
    }
}
