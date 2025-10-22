/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.plugin.utils

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.quality.pojo.enums.QualityOperation
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.plugin.codecc.CodeccUtils
import com.tencent.devops.plugin.constant.PluginMessageCode.BK_CI_PIPELINE
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.service.ServiceVarResource
import com.tencent.devops.quality.api.v2.ServiceQualityIndicatorResource
import com.tencent.devops.quality.api.v2.ServiceQualityInterceptResource
import com.tencent.devops.quality.constant.DEFAULT_CODECC_URL
import com.tencent.devops.quality.constant.codeccToolUrlPathMap

@Suppress("ALL")
object QualityUtils {
    /**
     * 获取质量红线结果
     * @param client
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param buildId 流水线构建ID
     * @param eventStatus 事件状态
     * @param startTime 事件开始时间
     * @param triggerType 触发类型
     * @param insertUrl 是否插入链接地址[github 不需要插入链接]
     */
    fun getQualityGitMrResult(
        client: Client,
        projectId: String,
        pipelineId: String,
        buildId: String,
        eventStatus: String,
        startTime: Long,
        triggerType: String,
        channelCode: ChannelCode
    ): Pair<List<String>, MutableMap<String, MutableList<List<String>>>> {
        val pipelineName = client.get(ServicePipelineResource::class)
            .getPipelineNameByIds(projectId, setOf(pipelineId))
            .data?.get(pipelineId) ?: ""
        // github 不需要插入链接, 仅在插件名处插入链接，链接地址用codecc插件输出变量
        val titleData = mutableListOf(
            eventStatus,
            DateTimeUtil.formatMilliTime(System.currentTimeMillis() - startTime),
            StartType.toReadableString(
                triggerType,
                null,
                I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            ),
            pipelineName,
            if (ChannelCode.isNeedAuth(channelCode)) {
                "${HomeHostUtil.innerServerHost()}/console/pipeline/$projectId/$pipelineId/detail/$buildId"
            } else {
                ""
            },
            I18nUtil.getCodeLanMessage(BK_CI_PIPELINE)
        )

        val ruleName = mutableSetOf<String>()
        // 插件输出变量
        val reportUrl = getBuildVar(
            client = client,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            varName = CodeccUtils.BK_CI_CODECC_REPORT_URL
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

                    val elementCnName = ElementUtils.getElementCnName(indicatorElementName, projectId).let {
                        if (!ChannelCode.isNeedAuth(channelCode) && !reportUrl.isNullOrBlank()) {
                            "<a target='_blank' href='$reportUrl'>$it</a>"
                        } else {
                            it
                        }
                    }
                    val resultList = resultMap[elementCnName] ?: mutableListOf()
                    val actualValue = when {
                        CodeccUtils.isCodeccAtom(indicatorElementName) -> getActualValue(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            detail = indicator?.elementDetail,
                            value = interceptItem.actualValue ?: "null",
                            client = client,
                            channelCode = channelCode
                        )
                        else -> interceptItem.actualValue ?: "null"
                    }
                    resultList.add(
                        listOf(
                            interceptItem.indicatorName,
                            actualValue,
                            QualityOperation.convertToSymbol(interceptItem.operation) + "" + interceptItem.value,
                            interceptItem.pass.toString(), ""
                        )
                    )
                    resultMap[elementCnName] = resultList
                }
                ruleName.add(ruleIntercept.ruleName)
            }
        titleData.add(ruleName.joinToString("、"))
        return Pair(titleData, resultMap)
    }

    // codecc要跳转到具体详情
    private fun getActualValue(
        projectId: String,
        pipelineId: String,
        buildId: String,
        detail: String?,
        value: String,
        client: Client,
        channelCode: ChannelCode
    ): String {
        val taskId = getBuildVar(
            client = client,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            varName = CodeccUtils.BK_CI_CODECC_TASK_ID
        )
        return if (detail.isNullOrBlank() || detail.split(",").size > 1) {
            "<a target='_blank' href='${HomeHostUtil.innerServerHost()}/" +
                "console/codecc/$projectId/task/$taskId/detail?buildId=$buildId'>$value</a>"
        } else {
            val detailValue = codeccToolUrlPathMap[detail] ?: DEFAULT_CODECC_URL
            val fillDetailUrl = detailValue.replace("##projectId##", projectId)
                .replace("##taskId##", taskId.toString())
                .replace("##buildId##", buildId)
                .replace("##detail##", detail)
            if (ChannelCode.isNeedAuth(channelCode)) {
                "<a target='_blank' href='${HomeHostUtil.innerServerHost()}/console$fillDetailUrl'>$value</a>"
            } else {
                "<a target='_blank' href='${HomeHostUtil.innerCodeccHost()}$fillDetailUrl'>$value</a>"
            }
        }
    }

    private fun getBuildVar(
        client: Client,
        projectId: String,
        pipelineId: String,
        buildId: String,
        varName: String
    ): String? {
        val variable = client.get(ServiceVarResource::class).getBuildVar(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            varName = varName
        ).data
        val taskId = variable?.get(varName)
        return taskId
    }
}
