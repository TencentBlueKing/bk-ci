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
import com.tencent.devops.common.pipeline.element.PushImageToThirdRepoElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.image.api.ServiceTkePushImageResource
import com.tencent.devops.image.pojo.enums.TaskStatus
import com.tencent.devops.image.pojo.tke.TkePushImageParam
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultSuccessAtomResponse
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.util.CommonUtils
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_URL
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.apache.commons.lang3.math.NumberUtils
import org.apache.poi.util.StringUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class PushImageToThirdRepoTaskAtom @Autowired constructor(
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter
) : IAtomTask<PushImageToThirdRepoElement> {

    companion object {
        private val logger = LoggerFactory.getLogger(PushImageToThirdRepoTaskAtom::class.java)
    }

    /*
     * 腾讯云公网域名，但是内网idc可以正常访问，而且是https，证书非私有证书，需要配置host:
     * 10.101.96.92 csighub.tencentyun.com
     */
    private val tkeRepoAdd = "csighub.tencentyun.com"

    override fun getParamElement(task: PipelineBuildTask): PushImageToThirdRepoElement {
        return JsonUtil.mapTo(task.taskParams, PushImageToThirdRepoElement::class.java)
    }

    override fun execute(
        task: PipelineBuildTask,
        param: PushImageToThirdRepoElement,
        runVariables: Map<String, String>
    ): AtomResponse {

        val srcImageName = parseVariable(param.srcImageName, runVariables)
        val srcImageTag = parseVariable(param.srcImageTag, runVariables)
        val repoAddress = if (param.repoAddress.isNullOrBlank()) {
            tkeRepoAdd
        } else {
            parseVariable(param.repoAddress, runVariables)
        }
        val ticketId = parseVariable(param.ticketId, runVariables)
        val targetImageName = parseVariable(param.targetImageName, runVariables)
        val targetImageTag = parseVariable(param.targetImageTag, runVariables)
        val cmdbIdStr = parseVariable(param.cmdbId, runVariables)
        val cmdbId = if (NumberUtils.isDigits(cmdbIdStr)) cmdbIdStr.toInt() else 0
        val verifyByOa = if (param.verifyByOa == null) false else param.verifyByOa

        val projectId = task.projectId
        val buildId = task.buildId
        val pipelineId = task.pipelineId
        val userId = task.starter
        val codeRepoUrl = getCodeRepoUrl(runVariables)

        val ticketsMap = CommonUtils.getCredential(
            client = client,
            projectId = projectId,
            credentialId = ticketId,
            type = CredentialType.USERNAME_PASSWORD
        )
        val userName = ticketsMap["v1"] as String
        val password = ticketsMap["v2"] as String

        val tkePushParams = TkePushImageParam(
            userId = userId,
            srcImageName = srcImageName,
            srcImageTag = srcImageTag,
            repoAddress = repoAddress,
            userName = userName,
            password = password,
            targetImageName = targetImageName,
            targetImageTag = targetImageTag,
            projectId = projectId,
            buildId = buildId,
            pipelineId = pipelineId,
            taskId = task.taskId,
            containerId = task.containerHashId ?: "",
            codeUrl = codeRepoUrl,
            executeCount = task.executeCount,
            cmdbId = cmdbId,
            verifyOa = verifyByOa!!
        )

        buildLogPrinter.addLine(
            buildId = buildId,
            message = "开始推送镜像",
            tag = task.taskId,
            jobId = task.containerHashId,
            executeCount = task.executeCount ?: 1
        )
        var pushImageTaskResult = client.get(ServiceTkePushImageResource::class).pushImage(tkePushParams)
        loop@ while (true) {
            if (pushImageTaskResult.isNotOk() || pushImageTaskResult.data == null) {
                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = "推送镜像失败",
                    tag = task.taskId,
                    jobId = task.containerHashId,
                    executeCount = task.executeCount ?: 1
                )
                return AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorMsg = "推送镜像失败"
                )
            }
            val pushImageTask = pushImageTaskResult.data!!
            return when (pushImageTask.taskStatus) {
                TaskStatus.SUCCESS.name -> {
                    buildLogPrinter.addLine(
                        buildId = buildId,
                        message = "推送镜像成功，【<a target='_blank' href='http://csighub.oa.com/tencenthub/repo'>查看镜像</a>】",
                        tag = task.taskId,
                        jobId = task.containerHashId,
                        executeCount = task.executeCount ?: 1
                    )
                    defaultSuccessAtomResponse
                }
                TaskStatus.RUNNING.name -> {
                    Thread.sleep(5 * 1000)
                    pushImageTaskResult =
                        client.get(ServiceTkePushImageResource::class).queryUploadTask(userId, pushImageTask.taskId)
                    continue@loop
                }
                else -> {
                    buildLogPrinter.addRedLine(
                        buildId = buildId,
                        message = "推送镜像失败: ${pushImageTask.taskMessage}",
                        tag = task.taskId,
                        jobId = task.containerHashId,
                        executeCount = task.executeCount ?: 1
                    )
                    return AtomResponse(
                        buildStatus = BuildStatus.FAILED,
                        errorType = ErrorType.USER,
                        errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                        errorMsg = "推送镜像失败: ${pushImageTask.taskMessage}"
                    )
                }
            }
        }
    }

    private fun getCodeRepoUrl(runVariables: Map<String, String>): String? {
        val codeRepoUrlMap = runVariables.filter { it.key.startsWith(PIPELINE_MATERIAL_URL) }.toMap()
        if (codeRepoUrlMap.isEmpty()) {
            logger.info("No code repo url in variables")
            return ""
        }
        return StringUtil.join(codeRepoUrlMap.values.toTypedArray(), ";")
    }
}
