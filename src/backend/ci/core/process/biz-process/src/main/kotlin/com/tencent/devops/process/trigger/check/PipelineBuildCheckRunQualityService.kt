package com.tencent.devops.process.trigger.check

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.quality.pojo.enums.QualityOperation
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.plugin.codecc.CodeccUtils
import com.tencent.devops.plugin.codecc.CodeccUtils.BK_CI_CODECC_REPORT_URL
import com.tencent.devops.plugin.codecc.CodeccUtils.BK_CI_CODECC_TASK_ID
import com.tencent.devops.plugin.constant.PluginMessageCode
import com.tencent.devops.quality.api.v2.ServiceQualityIndicatorResource
import com.tencent.devops.quality.api.v2.ServiceQualityInterceptResource
import com.tencent.devops.quality.constant.DEFAULT_CODECC_URL
import com.tencent.devops.quality.constant.codeccToolUrlPathMap
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineBuildCheckRunQualityService @Autowired constructor(
    private val client: Client
) {
    fun getQuality(
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildStatus: String,
        startTime: Long,
        triggerType: String,
        channelCode: ChannelCode,
        pipelineName: String,
        detailUrl: String,
        variables: Map<String, String>
    ): String {
        val qualityGitMrResult = getQualityGitMrResult(
            projectId,
            pipelineId,
            buildId,
            buildStatus,
            startTime,
            triggerType,
            channelCode,
            pipelineName,
            detailUrl,
            variables
        )
        return getQualityReport(
            qualityGitMrResult.first,
            qualityGitMrResult.second
        )
    }

    /**
     * 获取质量红线结果
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param buildId 流水线构建ID
     * @param eventStatus 事件状态
     * @param startTime 事件开始时间
     * @param triggerType 触发类型
     * @param insertUrl 是否插入链接地址[github 不需要插入链接]
     */
    @SuppressWarnings("NestedBlockDepth", "LongParameterList")
    fun getQualityGitMrResult(
        projectId: String,
        pipelineId: String,
        buildId: String,
        eventStatus: String,
        startTime: Long,
        triggerType: String,
        channelCode: ChannelCode,
        pipelineName: String,
        detailUrl: String,
        variables: Map<String, String>
    ): Pair<List<String>, MutableMap<String, MutableList<List<String>>>> {
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
            detailUrl,
            I18nUtil.getCodeLanMessage(PluginMessageCode.BK_CI_PIPELINE)
        )

        val ruleName = mutableSetOf<String>()
        // 插件输出变量

        val reportUrl = variables[BK_CI_CODECC_REPORT_URL]
        // key：质量红线产出插件
        // value：指标、预期、结果、状态
        val resultMap = mutableMapOf<String, MutableList<List<String>>>()
        client.get(ServiceQualityInterceptResource::class)
                .listHistory(projectId, pipelineId, buildId).data?.forEach { ruleIntercept ->
                    ruleIntercept.resultMsg.forEach { interceptItem ->
                        val indicator = client.get(ServiceQualityIndicatorResource::class)
                                .get(projectId, interceptItem.indicatorId).data
                        val indicatorElementName = indicator?.elementType ?: ""

                        val elementCnName = getElementCnName(indicatorElementName, projectId).let {
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
                                buildId = buildId,
                                detail = indicator?.elementDetail,
                                value = interceptItem.actualValue ?: "null",
                                channelCode = channelCode,
                                variables = variables
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

    /**
     * codecc要跳转到具体详情
     */
    @SuppressWarnings("LongParameterList")
    private fun getActualValue(
        projectId: String,
        buildId: String,
        detail: String?,
        value: String,
        channelCode: ChannelCode,
        variables: Map<String, String>
    ): String {
        val taskId = variables[BK_CI_CODECC_TASK_ID]
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

    private fun getElementCnName(classType: String, projectId: String): String {
        val map = getProjectElement(projectId)

        if (CodeccUtils.isCodeccAtom(classType)) {
            return map[CodeccUtils.BK_CI_CODECC_V3_ATOM] ?: ""
        }

        return map[classType] ?: ""
    }

    private fun getProjectElement(projectId: String): Map<String/* atomCode */, String/* cnName */> {
        return client.get(ServiceMarketAtomResource::class).getProjectElements(projectId).data!!
    }

    @Suppress("ALL")
    fun getQualityReport(
        titleData: List<String>,
        resultData: MutableMap<String, MutableList<List<String>>>
    ): String {
        if (resultData.isEmpty()) {
            return ""
        }
        val triggerType = titleData[2]
        val pipelineName = titleData[3]
        val url = titleData[4]
        val pipelineNameTitle = titleData[5]
        val ruleName = titleData[6]
        // codecc开源扫描不需要展示title,只需要展示质量红线明细
        val (showTitle, pipelineLinkElement) = if (url.isBlank()) {
            Pair(false, pipelineName)
        } else {
            Pair(true, "<a href='$url' style=\"color: #03A9F4\">$pipelineName</a>")
        }
        val title = "<table><tr>" +
                "<td style=\"border:none;padding-right: 0;\">$pipelineNameTitle：</td>" +
                "<td style=\"border:none;padding-left:0;\">$pipelineLinkElement</td>" +
                "<td style=\"border:none;padding-right: 0\">触发方式：</td>" +
                "<td style=\"border:none;padding-left:0;\">$triggerType</td>" +
                "<td style=\"border:none;padding-right: 0\">质量红线：</td>" +
                "<td style=\"border:none;padding-left:0;\">$ruleName</td>" +
                "</tr></table>"
        val body = StringBuilder("")
        body.append("<table border=\"1\" cellspacing=\"0\" width=\"450\">")
        body.append("<tr>")
        body.append("<th style=\"text-align:left;\">质量红线产出插件</th>")
        body.append("<th style=\"text-align:left;\">指标</th>")
        body.append("<th style=\"text-align:left;\">结果</th>")
        body.append("<th style=\"text-align:left;\">预期</th>")
        body.append("<th style=\"text-align:left;\"></th>")
        body.append("</tr>")
        resultData.forEach { elementName, result ->
            result.forEachIndexed { index, list ->
                body.append("<tr>")
                if (index == 0) body.append("<td>$elementName</td>") else body.append("<td></td>")
                body.append("<td>${list[0]}</td>")
                body.append("<td>${list[1]}</td>")
                body.append("<td>${list[2]}</td>")
                if (list[3] == "true") {
                    body.append("<td style=\"color: #4CAF50; font-weight: bold;\">&nbsp; &radic; &nbsp;</td>")
                } else {
                    body.append("<td style=\"color: #F44336; font-weight: bold;\">&nbsp; &times; &nbsp;</td>")
                }
                body.append("</tr>")
            }
        }
        body.append("</table>")

        return if (showTitle) {
            title + body.toString()
        } else {
            body.toString()
        }
    }
}
