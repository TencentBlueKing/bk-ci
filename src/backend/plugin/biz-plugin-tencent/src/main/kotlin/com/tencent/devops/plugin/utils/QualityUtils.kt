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

package com.tencent.devops.plugin.utils

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.atom.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.plugin.api.ServiceCodeccElementResource
import com.tencent.devops.plugin.api.pojo.GitCommitCheckEvent
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.quality.api.v2.ServiceQualityIndicatorResource
import com.tencent.devops.quality.api.v2.ServiceQualityInterceptResource
import com.tencent.devops.quality.api.v2.pojo.enums.QualityOperation
import com.tencent.devops.quality.constant.codeccToolUrlPathMap

object QualityUtils {
    fun getQualityGitMrResult(client: Client, event: GitCommitCheckEvent): Pair<List<String>, MutableMap<String, MutableList<List<String>>>> {
        val projectId = event.projectId
        val pipelineId = event.pipelineId
        val buildId = event.buildId
        val pipelineName = client.get(ServicePipelineResource::class)
                .getPipelineNameByIds(projectId, setOf(pipelineId))
                .data?.get(pipelineId) ?: ""

        val titleData = listOf(event.status,
                DateTimeUtil.formatMilliTime(System.currentTimeMillis() - event.startTime),
                StartType.toReadableString(event.triggerType, null),
                pipelineName,
                "${HomeHostUtil.innerServerHost()}/console/pipeline/$projectId/$pipelineId/detail/$buildId"
        )

        // key：质量红线产出插件
        // value：指标、预期、结果、状态
        val resultMap = mutableMapOf<String, MutableList<List<String>>>()
        client.get(ServiceQualityInterceptResource::class)
                .listHistory(projectId, pipelineId, buildId).data?.forEach { ruleIntercept ->
            ruleIntercept.resultMsg.forEach { interceptItem ->
                val indicator = client.get(ServiceQualityIndicatorResource::class)
                        .get(projectId, interceptItem.indicatorId).data
                val indicatorElementName = indicator?.elementType ?: ""
                val elementCnName = ElementUtils.getElementCnName(indicatorElementName, projectId)
                val resultList = resultMap[elementCnName] ?: mutableListOf()
                val actualValue = if (indicatorElementName == LinuxPaasCodeCCScriptElement.classType) {
                    getActualValue(projectId, pipelineId, indicator?.elementDetail, interceptItem.actualValue ?: "null", client)
                } else {
                    interceptItem.actualValue ?: "null"
                }
                resultList.add(listOf(
                        interceptItem.indicatorName,
                        actualValue,
                        QualityOperation.convertToSymbol(interceptItem.operation) + "" + interceptItem.value,
                        interceptItem.pass.toString()
                ))
                resultMap[elementCnName] = resultList
            }
        }
        return Pair(titleData, resultMap)
    }

    // codecc要跳转到具体详情
    private fun getActualValue(projectId: String, pipelineId: String, detail: String?, value: String, client: Client): String {
        val taskId = client.get(ServiceCodeccElementResource::class).get(projectId, pipelineId).data?.taskId
        return if (detail.isNullOrBlank()) {
            "<a target='_blank' href='${HomeHostUtil.innerServerHost()}/console/codecc/$projectId/procontrol/prodesc?proj_id=$taskId&projectId=$projectId'>$value</a>"
        } else {
            val detailValue = codeccToolUrlPathMap[detail] ?: "procontrol/multidefectmanage"
            "<a target='_blank' href='${HomeHostUtil.innerServerHost()}/console/codecc/$projectId/$detailValue?proj_id=$taskId&toolName=$detail&projectId=$projectId'>$value</a>"
        }
    }
}