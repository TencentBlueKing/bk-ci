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

package com.tencent.devops.stream.trigger.service

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.quality.pojo.enums.QualityOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.quality.api.v2.ServiceQualityIndicatorResource
import com.tencent.devops.quality.api.v2.ServiceQualityInterceptResource
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.constant.StreamMessageCode.STARTUP_CONFIG_MISSING
import com.tencent.devops.stream.trigger.actions.data.context.BuildFinishData
import com.tencent.devops.stream.util.ElementUtils
import com.tencent.devops.stream.util.StreamPipelineUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class StreamQualityService @Autowired constructor(
    private val streamGitConfig: StreamGitConfig
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamQualityService::class.java)
        private const val PIPELINE_NAME_TITLE = "Stream流水线"
    }

    fun getQualityGitMrResult(
        client: Client,
        gitProjectId: String,
        pipelineName: String,
        event: BuildFinishData,
        ruleIds: List<String>?
    ): Pair<List<String>, MutableMap<String, MutableList<List<String>>>> {
        try {
            val projectId = event.projectId
            val pipelineId = event.pipelineId
            val buildId = event.buildId

            val titleData = mutableListOf(
                event.status,
                if (event.startTime == null) {
                    "--"
                } else {
                    DateTimeUtil.formatMilliTime(
                        System.currentTimeMillis() - (event.startTime)
                    )
                },
                CodeEventType.MERGE_REQUEST.name,
                pipelineName,
                StreamPipelineUtils.genStreamV2BuildUrl(
                    homePage = streamGitConfig.streamUrl ?: throw ParamBlankException(
                        I18nUtil.getCodeLanMessage(
                            messageCode = STARTUP_CONFIG_MISSING,
                            params = arrayOf(" streamUrl")
                        )
                    ),
                    gitProjectId = gitProjectId,
                    pipelineId = pipelineId,
                    buildId = buildId
                ),
                PIPELINE_NAME_TITLE
            )

            val ruleName = mutableSetOf<String>()

            // key：质量红线产出插件
            // value：指标、预期、结果、状态
            val resultMap = mutableMapOf<String, MutableList<List<String>>>()
            client.get(ServiceQualityInterceptResource::class)
                .listRuleHistory(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    ruleIds = ruleIds
                ).data?.forEach { ruleIntercept ->
                    ruleIntercept.resultMsg.forEach { interceptItem ->
                        val indicator = client.get(ServiceQualityIndicatorResource::class)
                            .get(projectId, interceptItem.indicatorId).data
                        val indicatorElementName = indicator?.elementType ?: ""
                        val elementCnName = ElementUtils.getElementCnName(indicatorElementName, projectId)
                        val resultList = resultMap[elementCnName] ?: mutableListOf()
                        val actualValue = interceptItem.actualValue ?: ""
                        resultList.add(
                            listOf(
                                interceptItem.indicatorName,
                                actualValue,
                                QualityOperation.convertToSymbol(interceptItem.operation) + "" + interceptItem.value,
                                interceptItem.pass.toString(),
                                interceptItem.logPrompt ?: ""
                            )
                        )
                        logger.info("QUALITY|resultList is: $resultList")
                        resultMap[elementCnName] = resultList
                    }
                    ruleName.add(ruleIntercept.ruleName)
                }
            titleData.add(ruleName.joinToString("、"))
            return Pair(titleData, resultMap)
        } catch (ignore: Exception) {
            logger.warn("StreamQualityService|getQualityGitMrResult|failed=${ignore.message}")
        }
        return Pair(listOf(), mutableMapOf())
    }
}
