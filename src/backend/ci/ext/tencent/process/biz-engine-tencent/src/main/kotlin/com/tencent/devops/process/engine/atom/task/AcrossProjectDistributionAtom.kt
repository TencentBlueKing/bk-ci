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

package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.element.AcrossProjectDistributionElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.BK_SUCCESSFULLY_DISTRIBUTED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_SUCCESSFULLY_FAILED
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class AcrossProjectDistributionAtom @Autowired constructor(
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter
) :
    IAtomTask<AcrossProjectDistributionElement> {

    override fun getParamElement(task: PipelineBuildTask): AcrossProjectDistributionElement {
        return JsonUtil.mapTo(task.taskParams, AcrossProjectDistributionElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: AcrossProjectDistributionElement, runVariables: Map<String, String>): AtomResponse {
        val pipelineId = task.pipelineId
        val projectId = task.projectId
        val buildId = task.buildId
//        val param: AcrossProjectDistributionElement = JsonUtil.mapTo(task.taskParams)
        val path = parseVariable(param.path, runVariables) // parseVariable(getTaskParams("path", task), task)
        val customized = param.customized // parseVariable(getTaskParams("customized", task), task)
        val targetProjectId = parseVariable(param.targetProjectId, runVariables) // getTaskParams("targetProjectId", task), task)
        val targetPath = if (param.targetPath.isNotEmpty()) {
            parseVariable(param.targetPath, runVariables)
        } else {
            "/"
        }

        val artifactoryType = if (customized) ArtifactoryType.CUSTOM_DIR else ArtifactoryType.PIPELINE
        val relativePath = if (customized) path else "/$pipelineId/$buildId/${path.removePrefix("/")}"

        logger.info("[$buildId]|param=$param")

        val result =
            client.get(ServiceArtifactoryResource::class).acrossProjectCopy(
                task.starter, projectId, artifactoryType, relativePath, targetProjectId, targetPath
            )

        return if (result.isOk()) {
            buildLogPrinter.addLine(buildId,
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_SUCCESSFULLY_DISTRIBUTED,
                    params = arrayOf(result.data.toString())
                ), task.taskId, task.containerHashId, task.executeCount ?: 1)
            AtomResponse(BuildStatus.SUCCEED)
        } else {
            buildLogPrinter.addRedLine(buildId,
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_SUCCESSFULLY_FAILED
                ) + "$result", task.taskId, task.containerHashId, task.executeCount ?: 1)
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorMsg = I18nUtil.getCodeLanMessage(
                    messageCode = BK_SUCCESSFULLY_FAILED
                ) + "$result"
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AcrossProjectDistributionAtom::class.java)
    }
}
