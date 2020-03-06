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
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.plugin.codecc.CodeccApi
import com.tencent.devops.plugin.codecc.pojo.coverity.ProjectLanguage
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.service.PipelineTaskService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CodeccTransferService @Autowired constructor(
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineService: PipelineService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val codeccApi: CodeccApi
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CodeccTransferService::class.java)
    }

    fun transferToV2(projectId: String, pipelineIds: Set<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        pipelineTaskService.list(projectId, pipelineIds).map {
            val resultMsg = try {
                doTransfer(projectId, it)
            } catch (e: Exception) {
                logger.error("transfer pipeline ${it.key} fail", e)
                e.message ?: "unexpected error occur"
            }
            result[it.key] = resultMsg
        }
        return result
    }

    fun doTransfer(projectId: String, it: Map.Entry<String, List<PipelineModelTask>>): String {
        val pipelineId = it.key

        val codeccTask = it.value.filter { task -> task.classType == LinuxPaasCodeCCScriptElement.classType }
        if (codeccTask.isEmpty()) {
            return "$pipelineId do not contains old codecc element"
        }

        val newCodeccTask = it.value.filter { task -> task.taskParams["atomCode"] == "CodeccCheckAtom" }
        if (newCodeccTask.isNotEmpty()) {
            return "$pipelineId is already contains new codecc element"
        }

        // start to transfer
        val model = pipelineRepositoryService.getModel(pipelineId)!!
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(pipelineId)
        logger.info("get pipeline info for pipeline: $pipelineId, $pipelineInfo")
        model.stages.forEach { stage ->
            stage.containers.forEach { container ->
                val elementList = mutableListOf<Element>()
                container.elements.forEach { element ->
                    if (element.getClassType() == LinuxPaasCodeCCScriptElement.classType) {
                        logger.info("get new codecc element for pipeline: $pipelineId")
                        val newElement = getNewCodeccElement(element as LinuxPaasCodeCCScriptElement)
                            ?: return "get codecc new element fail"
                        elementList.add(newElement)
                    } else {
                        elementList.add(element)
                    }
                }
                container.elements = elementList
            }
        }

        // save pipeline
        logger.info("edit pipeline: $pipelineId")
        pipelineService.editPipeline(
            userId = pipelineInfo?.lastModifyUser ?: "",
            projectId = projectId,
            pipelineId = pipelineId,
            model = model,
            channelCode = ChannelCode.BS,
            checkPermission = false,
            checkTemplate = false
        )

        return "update codecc to v1 success"
    }

    private fun getNewCodeccElement(oldCodeccElement: LinuxPaasCodeCCScriptElement): Element? {
        val data = getCodeccDataMap(oldCodeccElement) ?: return null
        return MarketBuildAtomElement(
            name = oldCodeccElement.name,
            id = oldCodeccElement.id,
            status = oldCodeccElement.status,
            atomCode = "CodeccCheckAtom",
            version = "1.*",
            data = mapOf(
                "input" to data,
                "output" to mapOf<String, String>()
            )
        )
    }

    private fun getCodeccDataMap(oldCodeccElement: LinuxPaasCodeCCScriptElement): CodeccCheckAtomParamV3? {
        // 1.基础设置tab
        val params = CodeccCheckAtomParamV3()
        params.script = oldCodeccElement.script
        params.scriptType = oldCodeccElement.scriptType
        params.languages = oldCodeccElement.languages
        params.asynchronous = oldCodeccElement.asynchronous
        params.path = oldCodeccElement.path
        params.pyVersion = oldCodeccElement.pyVersion
        params.goPath = oldCodeccElement.goPath
        params.projectBuildType = oldCodeccElement.projectBuildType
        params.projectBuildCommand = oldCodeccElement.projectBuildCommand
        params.needCodeContent = oldCodeccElement.needCodeContent
        params.tools = oldCodeccElement.tools

        val ruleSetMap = getNewRuleSetMap(oldCodeccElement)
        params.languageRuleSetMap = ruleSetMap

        val taskInfo = codeccApi.getTaskInfo(oldCodeccElement.codeCCTaskName!!).data
            ?: return null
        // 2.通知报告tab
        with(taskInfo) {
            params.rtxReceiverType = notifyCustomInfo?.rtxReceiverType
            params.rtxReceiverList = notifyCustomInfo?.rtxReceiverList
            params.botWebhookUrl = notifyCustomInfo?.botWebhookUrl
            params.botRemindRange = notifyCustomInfo?.botRemindRange?.toString()
            params.botRemaindTools = notifyCustomInfo?.botRemaindTools
            params.botRemindSeverity = notifyCustomInfo?.botRemindSeverity?.toString()
            params.emailReceiverType = notifyCustomInfo?.emailReceiverType
            params.emailReceiverList = notifyCustomInfo?.emailReceiverList
            params.emailCCReceiverList = notifyCustomInfo?.emailCCReceiverList
            params.instantReportStatus = notifyCustomInfo?.instantReportStatus
            params.reportDate = notifyCustomInfo?.reportDate
            if (notifyCustomInfo?.reportTime != null && notifyCustomInfo?.reportMinute != null) {
                params.reportTime =
                    notifyCustomInfo?.reportTime!!.toString() + ":" + notifyCustomInfo?.reportMinute!!.toString()
            }
            params.reportTools = notifyCustomInfo?.reportTools
        }

        // 3.扫描配置tab
        val transferAuthors = codeccApi.getTransferAuthor(oldCodeccElement.codeCCTaskId!!).data
        // params.toolScanType = oldCodeccElement.scanType
        params.toolScanType = "0"
        params.newDefectJudgeFromDate = taskInfo.newDefectJudge?.fromDate
        params.transferAuthorList = transferAuthors?.transferAuthorList

        // 4.路径屏蔽tab
        val filterPaths = codeccApi.getFilterPath(oldCodeccElement.codeCCTaskId!!).data
        params.path = oldCodeccElement.path
        params.pathType = "CUSTOM"
        params.customPath = filterPaths?.filterPaths

        params.cppRule = ruleSetMap["C_CPP_RULE"]
        params.javaRule = ruleSetMap["JAVA_RULE"]
        params.jsRule = ruleSetMap["JS_RULE"]
        params.csharpRule = ruleSetMap["C_SHARP_RULE"]
        params.phpRule = ruleSetMap["PHP_RULE"]
        params.ocRule = ruleSetMap["OC_RULE"]
        params.pythonRule = ruleSetMap["PYTHON_RULE"]
        params.golangRule = ruleSetMap["GOLANG_RULE"]
        params.swiftRule = ruleSetMap["SWIFT_RULE"]
        params.rubyRule = ruleSetMap["RUBY_RULE"]
        params.typeScriptRule = ruleSetMap["TYPESCRIPT_RULE"]
        params.kotlinRule = ruleSetMap["KOTLIN_RULE"]
        params.othersRule = ruleSetMap["OTHERS_RULE"]

        logger.info("get new codecc params for pipeline: $params")

        return params
    }

    private fun getNewRuleSetMap(oldCodeccElement: LinuxPaasCodeCCScriptElement): Map<String, List<String>> {
        return oldCodeccElement.languages.map { lang ->
            val ruleName = lang.name.toUpperCase() + "_RULE"
            val ruleSetIdList = mutableListOf<String>()

            oldCodeccElement.tools?.forEach { tool ->
                val newRuleId = newRuleIdMap[lang.name + "_" + tool]
                    ?: throw RuntimeException("no new rule id found for ${lang.name}, $tool")
                ruleSetIdList.add(newRuleId)
            }
            ruleName to ruleSetIdList.toList()
        }.toMap()
    }

    //  [ "COVERITY", "KLOCWORK", "PINPOINT", "CPPLINT", "CHECKSTYLE", "ESLINT", "STYLECOP", "PHPCS", "PYLINT", "GOML", "DETEKT", "OCCHECK", "SENSITIVE", "HORUSPY", "WOODPECKER_SENSITIVE", "RIPS", "CCN", "DUPC" ]
    private val newRuleIdMap = mapOf(
        ProjectLanguage.C_CPP.name + "_" + "COVERITY" to "codecc_default_coverity_cpp",
        ProjectLanguage.C_CPP.name + "_" + "CPPLINT" to "standard_cpp",
        ProjectLanguage.C_CPP.name + "_" + "CCN" to "codecc_default_ccn_cpp",
        ProjectLanguage.C_CPP.name + "_" + "DUPC" to "codecc_default_dupc_cpp",
        ProjectLanguage.C_CPP.name + "_" + "SENSITIVE" to "ieg_sensitive_cpp",
        ProjectLanguage.C_CPP.name + "_" + "WOODPECKER_SENSITIVE" to "woodpecker_cpp",

        ProjectLanguage.PYTHON.name + "_" + "PYLINT" to "standard_python",
        ProjectLanguage.PYTHON.name + "_" + "CCN" to "codecc_default_ccn_python",
        ProjectLanguage.PYTHON.name + "_" + "DUPC" to "codecc_default_dupc_python",
        ProjectLanguage.PYTHON.name + "_" + "SENSITIVE" to "ieg_sensitive_python",
        ProjectLanguage.PYTHON.name + "_" + "WOODPECKER_SENSITIVE" to "woodpecker_python",

        ProjectLanguage.GOLANG.name + "_" + "COVERITY" to "codecc_default_coverity_cpp",
        ProjectLanguage.GOLANG.name + "_" + "GOML" to "standard_go",
        ProjectLanguage.GOLANG.name + "_" + "CCN" to "codecc_default_ccn_go",
        ProjectLanguage.GOLANG.name + "_" + "DUPC" to "codecc_default_dupc_go",
        ProjectLanguage.PYTHON.name + "_" + "SENSITIVE" to "ieg_sensitive_go",
        ProjectLanguage.GOLANG.name + "_" + "WOODPECKER_SENSITIVE" to "woodpecker_go"
    )

    class CodeccCheckAtomParamV3 {

        // 1.基础设置tab
        var script: String? = ""
        var scriptType: BuildScriptType? = BuildScriptType.SHELL
        var codeCCTaskName: String? = ""
        var codeCCTaskCnName: String? = null // 暂时没用
        var codeCCTaskId: String? = null // 调用接口用到

        var languages: List<ProjectLanguage>? = null // [PYTHON,KOTLIN]
        var asynchronous: Boolean? = true
        var path: String? = "" // 白名单

        var pyVersion: String? = null
        var goPath: String? = null
        var projectBuildType: String? = null
        var projectBuildCommand: String? = null
        var needCodeContent: String? = null

        var languageRuleSetMap: Map<String, List<String>>? = mapOf() // 规则集

        // 2.通知报告tab
        var rtxReceiverType: String? = null // rtx接收人类型：0-所有项目成员；1-接口人；2-自定义；3-无
        var rtxReceiverList: Set<String>? = null // rtx接收人列表，rtxReceiverType=2时，自定义的接收人保存在该字段
        var emailReceiverType: String? = null // 邮件收件人类型：0-所有项目成员；1-接口人；2-自定义；3-无
        var emailReceiverList: Set<String>? = null // 邮件收件人列表，当emailReceiverType=2时，自定义的收件人保存在该字段
        var emailCCReceiverList: Set<String>? = null
        var reportStatus: String? = null // 定时报告任务的状态，有效：1，暂停：2 (目前看了都是1)
        var reportDate: Set<Int>? = null
        var reportTime: String? = null
        var instantReportStatus: String? = null // 即时报告状态，有效：1，暂停：2
        var reportTools: Set<String>? = null
        var botWebhookUrl: String? = null
        var botRemindSeverity: String? = null // 7-总告警数； 3-严重 + 一般告警数；1-严重告警数
        var botRemaindTools: Set<String>? = null
        var botRemindRange: String? = null // 1-新增 2-遗留

        // 3.扫描配置tab
        var toolScanType: String? = null // 对应接口的scanType, 1：增量；0：全量
        var newDefectJudgeFromDate: String? = null
        var newDefectJudgeBy: String? = null // 判定方式1：按日期；2：按构建(目前都填1)
        var transferAuthorList: List<CodeccApi.TransferAuthorPair>? = null

        // 4.路径屏蔽tab
        var whileScanPaths: List<String>? = listOf() // 目前暂时不用
        var pathType: String? = "" // CUSTOM - 自定义 ； DEFAULT - 系统默认（目前之用CUSTOM）
        var customPath: List<String>? = null // 黑名单，添加后的代码路径将不会产生告警
        var filterDir: List<String>? = listOf() // 暂时不用
        var filterFile: List<String>? = listOf() // 暂时不用

        // 前端显示参数
        var tools: List<String>? = null
        @JsonProperty("C_CPP_RULE")
        var cppRule: List<String>? = null
        @JsonProperty("JAVA_RULE")
        var javaRule: List<String>? = null
        @JsonProperty("JS_RULE")
        var jsRule: List<String>? = null
        @JsonProperty("C_SHARP_RULE")
        var csharpRule: List<String>? = null
        @JsonProperty("PHP_RULE")
        var phpRule: List<String>? = null
        @JsonProperty("OC_RULE")
        var ocRule: List<String>? = null
        @JsonProperty("PYTHON_RULE")
        var pythonRule: List<String>? = null
        @JsonProperty("GOLANG_RULE")
        var golangRule: List<String>? = null
        @JsonProperty("SWIFT_RULE")
        var swiftRule: List<String>? = null
        @JsonProperty("RUBY_RULE")
        var rubyRule: List<String>? = null
        @JsonProperty("TYPESCRIPT_RULE")
        var typeScriptRule: List<String>? = null
        @JsonProperty("KOTLIN_RULE")
        var kotlinRule: List<String>? = null
        @JsonProperty("OTHERS_RULE")
        var othersRule: List<String>? = null
    }
}
