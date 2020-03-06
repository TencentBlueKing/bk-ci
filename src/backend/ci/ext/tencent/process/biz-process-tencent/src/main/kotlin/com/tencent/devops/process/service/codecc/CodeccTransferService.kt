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

package com.tencent.devops.process.service.codecc

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.util.ExecutorsUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.plugin.codecc.CodeccApi
import com.tencent.devops.plugin.codecc.pojo.coverity.ProjectLanguage
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.service.PipelineTaskService
import org.apache.commons.beanutils.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CodeccTransferService @Autowired constructor(
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineService: PipelineService,
    private val codeccApi: CodeccApi
) {
    fun transferToV2(projectId: String, pipelineIds: Set<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val futureResult = pipelineTaskService.list(projectId, pipelineIds).map {
            ExecutorsUtils.executeFixThreadFuture(Runnable {
                val pipelineId = it.key

                val codeccTask = it.value.filter { task -> task.classType == LinuxPaasCodeCCScriptElement.classType }
                if (codeccTask.isEmpty()) {
                    result[pipelineId] = "$pipelineId do not contains old codecc element"
                    return@Runnable
                }

                val newCodeccTask = it.value.filter { task -> task.taskParams["atomCode"] == "CodeccCheckAtom" }
                if (newCodeccTask.isNotEmpty()) {
                    result[pipelineId] = "$pipelineId is already contains new codecc element"
                    return@Runnable
                }

                // start to transfer
                val model = pipelineService.getPipeline("", projectId, pipelineId, ChannelCode.BS, false)
                model.stages.forEach { stage ->
                    stage.containers.forEach { container ->
                        val elementList = mutableListOf<Element>()
                        container.elements.forEach { element ->
                            if (element.getClassType() == LinuxPaasCodeCCScriptElement.classType) {
                                val newElement = getNewCodeccElement(element as LinuxPaasCodeCCScriptElement)
                                if (newElement == null) {
                                    result[pipelineId] = "get codecc new element fail"
                                    return@Runnable
                                }
                                elementList.add(newElement)
                            } else {
                                elementList.add(element)
                            }
                        }
                        container.elements = elementList
                    }
                }

                // save pipeline
                pipelineService.editPipeline(
                    userId = "",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    model = model,
                    channelCode = ChannelCode.BS,
                    checkPermission = false,
                    checkTemplate = false
                )

                result[pipelineId] = "update codecc to v1 success"
            })
        }
        futureResult.forEach { it.get() }
        return result
    }

    private fun getNewCodeccElement(oldCodeccElement: LinuxPaasCodeCCScriptElement): Element? {
        val data = getCodeccDataMap(oldCodeccElement) ?: return null
        return MarketBuildAtomElement(
            name = oldCodeccElement.name,
            id = oldCodeccElement.id,
            status = oldCodeccElement.status,
            atomCode = "CodeccCheckAtom",
            version = "1.*",
            data = mapOf("input" to data,
                "output" to mapOf<String, String>())
        )
    }

    private fun getCodeccDataMap(oldCodeccElement: LinuxPaasCodeCCScriptElement): CodeccCheckAtomParamV3? {
        // 1.基础设置tab
        val params = CodeccCheckAtomParamV3()
        params.script = oldCodeccElement.script
        params.languages = collectionToJson(oldCodeccElement.languages)
        params.asynchronous = oldCodeccElement.asynchronous
        params.path = oldCodeccElement.path
        params.pyVersion = oldCodeccElement.pyVersion
        params.goPath = oldCodeccElement.goPath
        params.projectBuildType = oldCodeccElement.projectBuildType
        params.projectBuildCommand = oldCodeccElement.projectBuildCommand
        params.needCodeContent = oldCodeccElement.needCodeContent
        params.languageRuleSetMap = collectionToJson(getNewRuleSetMap(oldCodeccElement))

        val taskInfo = codeccApi.getTaskInfo(oldCodeccElement.codeCCTaskName!!).data
            ?: return null
        // 2.通知报告tab
        with(taskInfo) {
            params.rtxReceiverType = notifyCustomInfo?.rtxReceiverType
            params.rtxReceiverList = notifyCustomInfo?.rtxReceiverList
            params.botWebhookUrl = notifyCustomInfo?.botWebhookUrl
            params.botRemindRange = notifyCustomInfo?.botRemindRange?.toString()
            params.botRemaindTools = collectionToJson(notifyCustomInfo?.botRemaindTools)
            params.botRemindSeverity = notifyCustomInfo?.botRemindSeverity?.toString()
            params.emailReceiverType = notifyCustomInfo?.emailReceiverType
            params.emailReceiverList = collectionToJson(notifyCustomInfo?.emailReceiverList)
            params.emailCCReceiverList = collectionToJson(notifyCustomInfo?.emailCCReceiverList)
            params.instantReportStatus = notifyCustomInfo?.instantReportStatus
            params.reportDate = collectionToJson(notifyCustomInfo?.reportDate)
            if (notifyCustomInfo?.reportTime != null && notifyCustomInfo?.reportMinute != null) {
                params.reportTime =
                    notifyCustomInfo?.reportTime!!.toString() + ":" + notifyCustomInfo?.reportMinute!!.toString()
            }
            params.reportTools = collectionToJson(notifyCustomInfo?.reportTools)
        }

        // 3.扫描配置tab
        val transferAuthors = codeccApi.getTransferAuthor(oldCodeccElement.codeCCTaskId!!).data
        // params.toolScanType = oldCodeccElement.scanType
        params.toolScanType = "0"
        params.newDefectJudgeFromDate = taskInfo.newDefectJudge?.fromDate
        params.transferAuthorList = collectionToJson(transferAuthors?.transferAuthorList)

        // 4.路径屏蔽tab
        val filterPaths = codeccApi.getFilterPath(oldCodeccElement.codeCCTaskId!!).data
        params.path = oldCodeccElement.path
        params.pathType = "CUSTOM"
        params.customPath = collectionToJson(filterPaths?.filterPaths)

        return params
    }

    private fun getNewRuleSetMap(oldCodeccElement: LinuxPaasCodeCCScriptElement): List<Map<String, List<String>>> {
        return oldCodeccElement.languages.map { lang ->
            val ruleName = lang.name.toUpperCase() + "_RULE"
            val ruleSetIdList = mutableListOf<String>()
            if (!oldCodeccElement.coverityToolSetId.isNullOrBlank()) ruleSetIdList.add(oldCodeccElement.coverityToolSetId!!)
            if (!oldCodeccElement.klocworkToolSetId.isNullOrBlank()) ruleSetIdList.add(oldCodeccElement.klocworkToolSetId!!)

            // cpplint	腾讯代码规范(c++)
            if (lang == ProjectLanguage.C_CPP) {
                ruleSetIdList.add("standard_cpp")
            } else {
                if (!oldCodeccElement.cpplintToolSetId.isNullOrBlank()) ruleSetIdList.add(oldCodeccElement.cpplintToolSetId!!)
            }

            if (!oldCodeccElement.eslintToolSetId.isNullOrBlank()) ruleSetIdList.add(oldCodeccElement.eslintToolSetId!!)

            // pylint	腾讯代码规范(python)
            if (lang == ProjectLanguage.PYTHON) {
                ruleSetIdList.add("standard_python")
            } else {
                if (!oldCodeccElement.pylintToolSetId.isNullOrBlank()) ruleSetIdList.add(oldCodeccElement.pylintToolSetId!!)
            }

            if (!oldCodeccElement.gometalinterToolSetId.isNullOrBlank()) ruleSetIdList.add(oldCodeccElement.gometalinterToolSetId!!)
            if (!oldCodeccElement.checkStyleToolSetId.isNullOrBlank()) ruleSetIdList.add(oldCodeccElement.checkStyleToolSetId!!)
            if (!oldCodeccElement.styleCopToolSetId.isNullOrBlank()) ruleSetIdList.add(oldCodeccElement.styleCopToolSetId!!)
            if (!oldCodeccElement.detektToolSetId.isNullOrBlank()) ruleSetIdList.add(oldCodeccElement.detektToolSetId!!)
            if (!oldCodeccElement.phpcsToolSetId.isNullOrBlank()) ruleSetIdList.add(oldCodeccElement.phpcsToolSetId!!)
            if (!oldCodeccElement.sensitiveToolSetId.isNullOrBlank()) ruleSetIdList.add(oldCodeccElement.sensitiveToolSetId!!)
            if (!oldCodeccElement.occheckToolSetId.isNullOrBlank()) ruleSetIdList.add(oldCodeccElement.occheckToolSetId!!)

            // gometalinter	腾讯代码规范(go)
            if (lang == ProjectLanguage.GOLANG) {
                ruleSetIdList.add("standard_go")
            } else {
                if (!oldCodeccElement.gociLintToolSetId.isNullOrBlank()) ruleSetIdList.add(oldCodeccElement.gociLintToolSetId!!)
            }

            if (!oldCodeccElement.woodpeckerToolSetId.isNullOrBlank()) ruleSetIdList.add(oldCodeccElement.woodpeckerToolSetId!!)
            if (!oldCodeccElement.horuspyToolSetId.isNullOrBlank()) ruleSetIdList.add(oldCodeccElement.horuspyToolSetId!!)
            if (!oldCodeccElement.pinpointToolSetId.isNullOrBlank()) ruleSetIdList.add(oldCodeccElement.pinpointToolSetId!!)

            mapOf(ruleName to ruleSetIdList.toList())
        }
    }

    class CodeccCheckAtomParamV3 {

        // 1.基础设置tab
        var script: String? = ""

        var codeCCTaskName: String? = ""
        var codeCCTaskCnName: String? = null // 暂时没用
        var codeCCTaskId: String? = null // 调用接口用到

        var languages: String? = null // [PYTHON,KOTLIN]
        var asynchronous: Boolean? = true
        var path: String? = "" // 白名单

        var pyVersion: String? = null
        var goPath: String? = null
        var projectBuildType: String? = null
        var projectBuildCommand: String? = null
        var needCodeContent: String? = null

        var languageRuleSetMap: String? = null // 规则集

        // 2.通知报告tab
        var rtxReceiverType: String? = null // rtx接收人类型：0-所有项目成员；1-接口人；2-自定义；3-无
        var rtxReceiverList: String? = null // rtx接收人列表，rtxReceiverType=2时，自定义的接收人保存在该字段
        var emailReceiverType: String? = null // 邮件收件人类型：0-所有项目成员；1-接口人；2-自定义；3-无
        var emailReceiverList: String? = null // 邮件收件人列表，当emailReceiverType=2时，自定义的收件人保存在该字段
        var emailCCReceiverList: String? = null
        var reportStatus: String? = null // 定时报告任务的状态，有效：1，暂停：2 (目前看了都是1)
        var reportDate: String? = null
        var reportTime: String? = null
        var instantReportStatus: String? = null // 即时报告状态，有效：1，暂停：2
        var reportTools: String? = null
        var botWebhookUrl: String? = null
        var botRemindSeverity: String? = null // 7-总告警数； 3-严重 + 一般告警数；1-严重告警数
        var botRemaindTools: String? = null
        var botRemindRange: String? = null // 1-新增 2-遗留

        // 3.扫描配置tab
        var toolScanType: String? = null // 对应接口的scanType, 1：增量；0：全量
        var newDefectJudgeFromDate: String? = null
        var newDefectJudgeBy: String? = null // 判定方式1：按日期；2：按构建(目前都填1)
        var transferAuthorList: String? = null

        // 4.路径屏蔽tab
        var whileScanPaths: List<String>? = listOf() // 目前暂时不用
        var pathType: String? = "" // CUSTOM - 自定义 ； DEFAULT - 系统默认（目前之用CUSTOM）
        var customPath: String? = null // 黑名单，添加后的代码路径将不会产生告警
        var filterDir: List<String>? = listOf() // 暂时不用
        var filterFile: List<String>? = listOf() // 暂时不用
    }

    private fun collectionToJson(coll: Collection<Any>?): String? {
        if (coll == null) return null
        return JsonUtil.getObjectMapper().writeValueAsString(coll)
    }
}
