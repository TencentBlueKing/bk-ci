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

package com.tencent.devops.process.engine.atom.task.deploy

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.gcloud.GcloudClient
import com.tencent.devops.common.gcloud.api.pojo.CommonParam
import com.tencent.devops.common.gcloud.api.pojo.DeleteVerParam
import com.tencent.devops.common.pipeline.element.GcloudDeleteVersionElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.plugin.api.ServiceGcloudConfResource
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.util.gcloud.TicketUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class GcloudDeleteVersionTaskAtom @Autowired constructor(
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val buildLogPrinter: BuildLogPrinter
) : IAtomTask<GcloudDeleteVersionElement> {

    override fun execute(task: PipelineBuildTask, param: GcloudDeleteVersionElement, runVariables: Map<String, String>): AtomResponse {
        parseParam(param, runVariables)
        buildLogPrinter.addLine(task.buildId, "gcloud element params:\n $param", task.taskId, task.containerHashId, task.executeCount ?: 1)

        val gcloudUtil = TicketUtil(client)
        with(param) {
            val elementId = task.taskId
            buildLogPrinter.addLine(task.buildId, "正在开始更新gcloud版本配置信息，结果可以稍后前往查看：\n", elementId, task.containerHashId, task.executeCount ?: 1)
            buildLogPrinter.addLine(task.buildId, "<a target='_blank' href='http://console.gcloud.oa.com/dolphin/edit/$gameId/$productId/$versionStr'>查看详情</a>", elementId, task.containerHashId, task.executeCount ?: 1)

            val projectId = task.projectId
            val buildId = task.buildId
            val userId = task.starter

            // 获取accessId和accessKey
            val keyPair = gcloudUtil.getAccesIdAndToken(projectId, ticketId)
            val accessId = keyPair.first
            val accessKey = keyPair.second

            val commonParam = CommonParam(gameId, accessId, accessKey)
            val host = client.get(ServiceGcloudConfResource::class).getByConfigId(configId.toInt()).data
                    ?: throw TaskExecuteException(
                        errorCode = ErrorCode.USER_INPUT_INVAILD,
                        errorType = ErrorType.USER,
                        errorMsg = "unknown configId($configId)")
            val gcloudClient = GcloudClient(objectMapper, host.address, host.fileAddress)

            // step1
            val delVerParam = DeleteVerParam(userId, productId.toInt(), versionStr)
            buildLogPrinter.addLine(buildId, "删除版本的配置信息：\n$delVerParam", elementId, task.containerHashId, task.executeCount ?: 1)
            gcloudClient.deleteVersion(delVerParam, commonParam)
            buildLogPrinter.addLine(buildId, "删除版本成功：$versionStr!(gameId: $gameId, productId: $productId)", elementId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(BuildStatus.SUCCEED)
        }
    }

    private fun parseParam(param: GcloudDeleteVersionElement, runVariables: Map<String, String>) {
        param.configId = parseVariable(param.configId, runVariables)
        param.gameId = parseVariable(param.gameId, runVariables)
        param.productId = parseVariable(param.productId, runVariables)
        param.ticketId = parseVariable(param.ticketId, runVariables)
        param.versionStr = parseVariable(param.versionStr, runVariables)
    }

    override fun getParamElement(task: PipelineBuildTask): GcloudDeleteVersionElement {
        return JsonUtil.mapTo(task.taskParams, GcloudDeleteVersionElement::class.java)
    }
}
