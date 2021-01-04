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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.gcloud.GcloudClient
import com.tencent.devops.common.gcloud.api.pojo.CommonParam
import com.tencent.devops.common.gcloud.api.pojo.UpdateVerParam
import com.tencent.devops.common.pipeline.element.GcloudUpdateVersionElement
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
class GcloudUpdateVersionTaskAtom @Autowired constructor(
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val buildLogPrinter: BuildLogPrinter
) : IAtomTask<GcloudUpdateVersionElement> {

    override fun execute(task: PipelineBuildTask, param: GcloudUpdateVersionElement, runVariables: Map<String, String>): AtomResponse {
        parseParam(param, runVariables)
        buildLogPrinter.addLine(task.buildId, "gcloud element params:\n $param", task.taskId, task.containerHashId, task.executeCount ?: 1)

        val ticketUtil = TicketUtil(client)
        with(param) {
            val elementId = task.taskId
            buildLogPrinter.addLine(task.buildId, "正在开始更新gcloud版本配置信息，结果可以稍后前往查看：\n", elementId, task.containerHashId, task.executeCount ?: 1)
            buildLogPrinter.addLine(task.buildId, "<a target='_blank' href='http://console.gcloud.oa.com/dolphin/edit/$gameId/$productId/$versionStr'>查看详情</a>", elementId, task.containerHashId, task.executeCount ?: 1)

            val projectId = task.projectId
            val buildId = task.buildId
            val userId = task.starter

            /*
            *
            * 版本目标用户。可选值：
            * 0 - 不可用，1 - 普通用户可用，2 - 灰度用户可用，
            * 3 - 普通用户和灰度用户都可用，4 - 审核版本，缺省 0
            *
            * */
            val availableType = if (versionType == "0") {
                if (normalUserCanUse == "1" && grayUserCanUse == "0") {
                    1
                } else if (normalUserCanUse == "0" && grayUserCanUse == "1") {
                    2
                } else if (normalUserCanUse == "1" && grayUserCanUse == "1") {
                    3
                } else {
                    0
                }
            } else {
                4
            }

            // 获取accessId和accessKey
            val ketPair = ticketUtil.getAccesIdAndToken(projectId, ticketId)
            val accessId = ketPair.first
            val accessKey = ketPair.second

            val commonParam = CommonParam(gameId, accessId, accessKey)
            val host = client.get(ServiceGcloudConfResource::class).getByConfigId(configId.toInt()).data
                    ?: throw RuntimeException("unknown configId($configId)")
            val gcloudClient = GcloudClient(objectMapper, host.address, host.fileAddress)

            // step1
            val updateVerParam = UpdateVerParam(userId, productId.toInt(), versionStr, availableType, grayRuleId, versionDes, customStr)
            buildLogPrinter.addLine(buildId, "更新的配置信息：\n$updateVerParam", elementId, task.containerHashId, task.executeCount ?: 1)

            buildLogPrinter.addLine(buildId, "updateVerParam", elementId, task.containerHashId, task.executeCount ?: 1)
            gcloudClient.updateVersion(updateVerParam, commonParam)
            buildLogPrinter.addLine(buildId, "更新gcloud配置成功!(gameId: $gameId, productId: $productId)", elementId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(BuildStatus.SUCCEED)
        }
    }

    override fun getParamElement(task: PipelineBuildTask): GcloudUpdateVersionElement {
        return JsonUtil.mapTo(task.taskParams, GcloudUpdateVersionElement::class.java)
    }

    private fun parseParam(param: GcloudUpdateVersionElement, runVariables: Map<String, String>) {
        param.configId = parseVariable(param.configId, runVariables)
        param.productId = parseVariable(param.productId, runVariables)
        param.gameId = parseVariable(param.gameId, runVariables)
        param.versionStr = parseVariable(param.versionStr, runVariables)
        param.ticketId = parseVariable(param.ticketId, runVariables)
        param.versionType = parseVariable(param.versionType, runVariables)
        param.normalUserCanUse = parseVariable(param.normalUserCanUse, runVariables)
        param.grayUserCanUse = parseVariable(param.grayUserCanUse, runVariables)
        param.grayRuleId = parseVariable(param.grayRuleId, runVariables)
        param.versionDes = parseVariable(param.versionDes, runVariables)
        param.customStr = parseVariable(param.customStr, runVariables)
    }
}
