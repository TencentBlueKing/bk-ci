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

package com.tencent.devops.quality.service.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.plugin.api.ServiceCodeccElementResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.quality.api.v2.pojo.QualityHisMetadata
import com.tencent.devops.quality.api.v2.pojo.QualityIndicator
import com.tencent.devops.quality.api.v2.pojo.QualityRule
import com.tencent.devops.quality.api.v2.pojo.QualityRuleInterceptRecord
import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType
import com.tencent.devops.quality.api.v2.pojo.request.BuildCheckParams
import com.tencent.devops.quality.api.v2.pojo.response.AtomRuleResponse
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleMatchTask
import com.tencent.devops.quality.constant.codeccToolUrlPathMap
import com.tencent.devops.quality.pojo.RuleCheckResult
import com.tencent.devops.quality.pojo.RuleCheckSingleResult
import com.tencent.devops.quality.pojo.enum.NotifyType
import com.tencent.devops.quality.pojo.enum.RuleInterceptResult
import com.tencent.devops.quality.pojo.enum.RuleOperation
import com.tencent.devops.quality.service.GroupService
import com.tencent.devops.quality.util.EmailUtil
import com.tencent.devops.quality.util.RtxUtil
import com.tencent.devops.quality.util.ThresholdOperationUtil
import com.tencent.devops.quality.util.WechatUtil
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.concurrent.Executors

@Service
class QualityRuleCheckService @Autowired constructor(
    private val ruleService: QualityRuleService,
    private val qualityHisMetadataService: QualityHisMetadataService,
    private val groupService: GroupService,
    private val countService: QualityCountService,
    private val historyService: QualityHistoryService,
    private val controlPointService: QualityControlPointService,
    private val client: Client,
    private val objectMapper: ObjectMapper
) {
    private val DEFAULT_TIMEOUT_MINUTES = 15
    private val executors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    fun userGetMatchRuleList(projectId: String, pipelineId: String): List<QualityRuleMatchTask> {
        // 取出项目下包含该流水线的所有红线，再按控制点分组
        val filterRuleList = getProjectRuleList(projectId, pipelineId, null)
        return listMatchTask(filterRuleList)
    }

    fun userGetMatchTemplateList(projectId: String, templateId: String?): List<QualityRuleMatchTask> {
        if (templateId.isNullOrBlank()) return listOf()
        val ruleList = getProjectRuleList(projectId, null, templateId)
        return listMatchTask(ruleList)
    }

    fun userListAtomRule(projectId: String, pipelineId: String, atomCode: String, atomVersion: String): AtomRuleResponse {
        val filterRuleList = getProjectRuleList(projectId, pipelineId, null).filter { it.controlPoint.name == atomCode }
        val ruleList = listMatchTask(filterRuleList)
        val isControlPoint = controlPointService.isControlPoint(atomCode, atomVersion, projectId)
        return AtomRuleResponse(isControlPoint, ruleList)
    }

    fun userListTemplateAtomRule(projectId: String, templateId: String, atomCode: String, atomVersion: String): AtomRuleResponse {
        val filterRuleList = getProjectRuleList(projectId, null, templateId).filter { it.controlPoint.name == atomCode }
        val ruleList = listMatchTask(filterRuleList)
        val isControlPoint = controlPointService.isControlPoint(atomCode, atomVersion, projectId)
        return AtomRuleResponse(isControlPoint, ruleList)
    }

    private fun listMatchTask(ruleList: List<QualityRule>): List<QualityRuleMatchTask> {
        val matchTaskList = mutableListOf<QualityRuleMatchTask>()
        ruleList.groupBy { it.controlPoint.position.name }.forEach { controlPointPosName, rules ->

            // 按照控制点拦截位置再分组
            rules.groupBy { it.controlPoint.position }.forEach { position, positionRules ->
                val controlPoint = positionRules.first().controlPoint
                val taskRuleList = mutableListOf<QualityRuleMatchTask.RuleMatchRule>()
                val taskThresholdList = mutableListOf<QualityRuleMatchTask.RuleThreshold>()
                val taskAuditUserList = mutableSetOf<String>()

                positionRules.forEach {
                    // 获取规则列表
                    taskRuleList.add(QualityRuleMatchTask.RuleMatchRule(it.hashId, it.name, it.gatewayId))

                    // 获取阈值列表
                    taskThresholdList.addAll(it.indicators.map { indicator ->
                        QualityRuleMatchTask.RuleThreshold(
                                indicator.hashId,
                                indicator.cnName,
                                indicator.metadataList.map { it.name },
                                indicator.operation,
                                indicator.threshold
                        )
                    })

                    // 获取审核用户列表
                    taskAuditUserList.addAll(if (it.operation == RuleOperation.AUDIT) {
                        it.auditUserList?.toSet() ?: setOf()
                    } else {
                        setOf()
                    })
                }
                // 生成结果
                matchTaskList.add(QualityRuleMatchTask(controlPoint.name, controlPoint.cnName, position,
                        taskRuleList, taskThresholdList, taskAuditUserList))
            }
        }
        return matchTaskList
    }

    private fun getProjectRuleList(projectId: String, pipelineId: String?, templateId: String?): List<QualityRule> {
        val ruleList = ruleService.serviceListRules(projectId)
        logger.info("get project rule list for $projectId, $pipelineId, $templateId: ${ruleList.map { it.name }}")
        return ruleList.filter {
            var result = false
            if (!pipelineId.isNullOrBlank()) result = (result || it.range.contains(pipelineId))
            if (!templateId.isNullOrBlank()) result = (result || it.templateRange.contains(templateId))
            return@filter result
        }
    }

    fun check(buildCheckParams: BuildCheckParams): RuleCheckResult {
        with(buildCheckParams) {
            logger.info("start check pipeline($pipelineId) build($buildId) task(${buildCheckParams.taskId}) data($buildCheckParams)")
            val resultList = mutableListOf<RuleCheckSingleResult>()
            val ruleInterceptList = mutableListOf<Triple<QualityRule, Boolean, List<QualityRuleInterceptRecord>>>()

            // 遍历项目下所有拦截规则
            val ruleList = ruleService.serviceListRuleByPosition(projectId, buildCheckParams.position)
            val metadataList = lazyGetHisMetadata(buildId)
            logger.info("Rule metadata serviceList for build(${buildCheckParams.buildId}):\n metadataList=$metadataList")

            ruleList.filter { rule ->
                logger.info("validate whether to check rule(${rule.name}) with gatewayId(${rule.gatewayId})")
                if (rule.controlPoint.name != buildCheckParams.taskId) return@filter false
                val gatewayId = rule.gatewayId ?: ""
                if (!buildCheckParams.interceptTaskName.toLowerCase().contains(gatewayId.toLowerCase())) return@filter false

                val containsInPipeline = rule.range.contains(pipelineId)
                val containsInTemplate = rule.templateRange.contains(buildCheckParams.templateId)
                return@filter (containsInPipeline || containsInTemplate)
            }.forEach { rule ->
                logger.info("start to check rule(${rule.name})")

                val result = checkIndicator(rule, buildCheckParams, metadataList)
                val interceptRecordList = result.second
                val interceptResult = result.first
                val params = mapOf("projectId" to buildCheckParams.projectId, "pipelineId" to buildCheckParams.pipelineId)

                resultList.add(getRuleCheckSingleResult(rule.name, interceptRecordList, params))
                ruleInterceptList.add(Triple(rule, interceptResult, interceptRecordList))
            }

            // 异步后续的处理
            executors.execute { checkPostHandle(buildCheckParams, ruleInterceptList, resultList) }

            // generate result
            val failRule = ruleInterceptList.filter { !it.second }.map { it.first }
            val allPass = failRule.isEmpty()
            val allEnd = allPass || (!allPass && !failRule.any { it.operation == RuleOperation.AUDIT })
            val auditTimeOutMinutes = if (!allPass) Collections.min(failRule.map { it.auditTimeoutMinutes ?: DEFAULT_TIMEOUT_MINUTES })
                                            else DEFAULT_TIMEOUT_MINUTES
            logger.info("check result allPass($allPass) allEnd($allEnd) auditTimeoutMinutes($auditTimeOutMinutes)")
            logger.info("end check pipeline($pipelineId) build($buildId) task(${buildCheckParams.taskId})")
            return RuleCheckResult(allPass, allEnd, auditTimeOutMinutes * 60, resultList)
        }
    }

    // codecc回调数据是异步的，为空加个等待
    private fun lazyGetHisMetadata(buildId: String): List<QualityHisMetadata> {
        for (i in setOf(1, 5, 3, 7)) {
            val result = qualityHisMetadataService.serviceGetHisMetadata(buildId)
            if (result.isNotEmpty()) return result
            Thread.sleep(i * 1000L)
        }
        return listOf()
    }

    private fun checkPostHandle(
        buildCheckParams: BuildCheckParams,
        result: List<Triple<QualityRule, Boolean, List<QualityRuleInterceptRecord>>>,
        resultList: List<RuleCheckSingleResult>
    ) {
        result.forEach {
            val rule = it.first
            val ruleId = HashUtil.decodeIdToLong(rule.hashId)
            val interceptResult = it.second
            val interceptRecordList = it.third
            val createTime = LocalDateTime.now()

            with(buildCheckParams) {
                recordHistory(projectId, ruleId, pipelineId, buildId, interceptResult, interceptRecordList, createTime)
                if (!interceptResult) {
                    try {
                        if (rule.operation == RuleOperation.END) {
                            sendEndNotification(projectId, pipelineId, buildId, buildNo, createTime, interceptRecordList,
                                    rule.notifyTypeList ?: listOf(), rule.notifyGroupList ?: listOf(), rule.notifyUserList
                                    ?: listOf())
                        } else {
                            sendAuditNotification(projectId, pipelineId, buildId, buildNo, createTime, resultList, rule.auditUserList
                                    ?: listOf())
                        }
                    } catch (t: Throwable) {
                        logger.error("send notification fail", t)
                    }
                }
                countService.countIntercept(projectId, pipelineId, ruleId, interceptResult)
            }
        }
    }

    private fun checkIndicator(rule: QualityRule, buildCheckParams: BuildCheckParams, metadataList: List<QualityHisMetadata>): Pair<Boolean, MutableList<QualityRuleInterceptRecord>> {
        var allCheckResult = true
        val interceptList = mutableListOf<QualityRuleInterceptRecord>()
        val indicators = rule.indicators
        val metadataMap = metadataList.map { it.enName to it }.toMap()
        // 遍历每个指标
        indicators.forEach { indicator ->
            val thresholdType = indicator.thresholdType
            var checkResult = true

            // 脚本原子的指标特殊处理：取指标英文名 = 基础数据名
            val filterMetadataList = if (indicator.isScriptElementIndicator()) {
                metadataList.filter { indicator.enName == it.enName }.filter { it.elementType in QualityIndicator.SCRIPT_ELEMENT }
            } else {
                indicator.metadataList.map { metadataMap[it.enName] }
            }
            logger.info("Rule check indicator threshold for build(${buildCheckParams.buildId}):\n indicator=$indicator \nmetadataList=$filterMetadataList")

            // 遍历所有基础数据
            var elementDetail = ""
            val result: String? = when (thresholdType) {
            // int类型把所有基础数据累加
                QualityDataType.INT -> {
                    var result: Int? = null
                    for (it in filterMetadataList) {
                        // -1表示直接失败
                        if (DETAIL_NOT_RUN_VALUE == it?.value) {
                            result = null
                            break
                        }

                        if (it?.value != null && NumberUtils.isNumber(it.value)) {
                            val value = it.value.toInt()
                            result = (result ?: 0) + value
                            // 记录”查看详情“里面跳转的基础数据, 记录第一个
                            if (value >= 0 && elementDetail.isBlank()) elementDetail = it.detail
                        }
                    }
                    if (!ThresholdOperationUtil.valid(result?.toString(), indicator.threshold, indicator.operation)) {
                        checkResult = false
                        allCheckResult = false
                    }
                    result?.toString()
                }
            // float类型把所有基础数据累加
                QualityDataType.FLOAT -> {
                    var result: BigDecimal? = null
                    for (it in filterMetadataList) {

                        if (it?.value != null && NumberUtils.isNumber(it.value)) {
                            val value = BigDecimal(it.value)

                            // -1表示直接失败
                            if (DETAIL_NOT_RUN_FLOAT_VALUE.compareTo(value) == 0) {
                                result = null
                                break
                            }

                            result = result?.plus(BigDecimal(it.value)) ?: BigDecimal(it.value)
                            // 记录”查看详情“里面跳转的基础数据
                            if (value >= BigDecimal(0) && elementDetail.isBlank()) elementDetail = it.detail
                        }
                    }
                    if (!ThresholdOperationUtil.validDecimal(result, BigDecimal(indicator.threshold), indicator.operation)) {
                        checkResult = false
                        allCheckResult = false
                    }
                    result?.toString()
                }
            // 布尔类型把所有基础数据求与
                QualityDataType.BOOLEAN -> {
                    logger.info("is boolean...")
                    var result: Boolean? = null
                    val threshold = indicator.threshold.toBoolean()
                    logger.info("boolean threshold: $threshold")
                    for (it in filterMetadataList) {
                        logger.info("each value: ${it?.value}")
                        if (it?.value != null && (it.value.toLowerCase() == "true" || it.value.toLowerCase() == "false")) {
                            val value = it.value.toBoolean()
                            logger.info("each convert value: $value")
                            if (value != threshold) {
                                checkResult = false
                                allCheckResult = false
                                result = value
                                // 记录”查看详情“里面跳转的基础数据
                                elementDetail = it.detail
                                break
                            } else {
                                // 全通过了，也要有值
                                result = threshold
                            }
                        }
                    }

                    // 全为null，不通过
                    if (!ThresholdOperationUtil.valid(result?.toString() ?: "", indicator.threshold, indicator.operation)) {
                        checkResult = false
                        allCheckResult = false
                    }
                    result?.toString()
                }
                else -> {
                    null
                }
            }
            with(indicator) {
                interceptList.add(
                        QualityRuleInterceptRecord(hashId, cnName, elementType, operation, threshold,
                                result, rule.controlPoint.name, checkResult, elementDetail, logPrompt)
                )
            }
        }
        return Pair(allCheckResult, interceptList)
    }

    /**
     * 获取单个拦截成功信息
     */
    private fun getRuleCheckSingleResult(ruleName: String, interceptRecordList: List<QualityRuleInterceptRecord>, params: Map<String, String>): RuleCheckSingleResult {
        val messageList = interceptRecordList.map {
            val thresholdOperationName = ThresholdOperationUtil.getOperationName(it.operation)

            val sb = StringBuilder()
            if (it.pass) {
                sb.append("已通过：")
            } else {
                sb.append("已拦截：")
            }
            val nullMsg = if (it.actualValue == null) "你可能并未添加工具或打开相应规则。" else ""
            val detailMsg = getDetailMsg(it, params)
            Pair(sb.append("${it.indicatorName}当前值(${it.actualValue})，期望$thresholdOperationName${it.value}。 $nullMsg").toString(), detailMsg)
        }
        return RuleCheckSingleResult(ruleName, messageList)
    }

    private fun getDetailMsg(record: QualityRuleInterceptRecord, params: Map<String, String>): String {
        // codecc跳到独立入口页面
        return if (record.indicatorType == LinuxPaasCodeCCScriptElement.classType) {
            val projectId = params["projectId"] ?: ""
            val pipelineId = params["pipelineId"] ?: ""
            val taskId = client.get(ServiceCodeccElementResource::class).get(projectId, pipelineId).data?.taskId
            if (taskId.isNullOrBlank()) {
                logger.warn("taskId is null or blank for project($projectId) pipeline($pipelineId)")
                return ""
            }
            if (record.detail.isNullOrBlank()) {
                "<a target='_blank' href='${HomeHostUtil.innerServerHost()}/console/codecc/$projectId/task/$taskId/detail'>查看详情</a>"
            } else {
                val detail = codeccToolUrlPathMap[record.detail!!] ?: "defect/lint"
                "<a target='_blank' href='${HomeHostUtil.innerServerHost()}/console/codecc/$projectId/task/$taskId/$detail/${record.detail}/list'>查看详情</a>"
            }
        } else {
            record.logPrompt ?: ""
        }
    }

    /**
     * 记录拦截历史
     */
    private fun recordHistory(projectId: String, ruleId: Long, pipelineId: String, buildId: String, pass: Boolean, interceptRecordList: List<QualityRuleInterceptRecord>, time: LocalDateTime) {
        val interceptList = objectMapper.writeValueAsString(interceptRecordList)
        if (pass) {
            historyService.serviceCreate(projectId, ruleId, pipelineId, buildId, RuleInterceptResult.PASS.name, interceptList, time, time)
        } else {
            historyService.serviceCreate(projectId, ruleId, pipelineId, buildId, RuleInterceptResult.FAIL.name, interceptList, time, time)
            ruleService.plusInterceptTimes(ruleId)
        }
        ruleService.plusExecuteCount(ruleId)
    }

    private fun sendAuditNotification(
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildNo: String,
        createTime: LocalDateTime,
        resultList: List<RuleCheckSingleResult>,
        auditNotifyUserList: List<String>
    ) {
        val projectName = getProjectName(projectId)
        val pipelineName = getPipelineName(projectId, pipelineId)
        val url = "${HomeHostUtil.innerServerHost()}/console/pipeline/$projectId/$pipelineId/detail/$buildId"
        val time = createTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss"))

        // 获取通知用户集合
        val notifyUserSet = auditNotifyUserList.toSet()

        // 获取拦截列表
        // val interceptList = getInterceptList(interceptRecordList)

        // 推送消息
        val rtxMessage = RtxUtil.makeAuditMessage(projectName, pipelineName, buildNo, time, resultList, url, notifyUserSet)
        client.get(ServiceNotifyResource::class).sendRtxNotify(rtxMessage)
        val wechatMessage = WechatUtil.makeAuditMessage(projectName, pipelineName, buildNo, time, resultList, url, notifyUserSet)
        client.get(ServiceNotifyResource::class).sendWechatNotify(wechatMessage)
        val emailMessage = EmailUtil.makeAuditMessage(projectName, pipelineName, buildNo, time, resultList, url, notifyUserSet)
        client.get(ServiceNotifyResource::class).sendEmailNotify(emailMessage)
    }

    /**
     * 发送终止或者审核通知
     */
    private fun sendEndNotification(
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildNo: String,
        createTime: LocalDateTime,
        interceptRecordList: List<QualityRuleInterceptRecord>,
        endNotifyTypeList: List<NotifyType>,
        endNotifyGroupList: List<String>,
        endNotifyUserList: List<String>
    ) {
        val projectName = getProjectName(projectId)
        val pipelineName = getPipelineName(projectId, pipelineId)
        val url = "${HomeHostUtil.innerServerHost()}/console/pipeline/$projectId/$pipelineId/detail/$buildId"
        val time = createTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        // 获取通知用户集合
        val notifyUserSet = mutableSetOf<String>()
        val groupUsers = groupService.serviceGetUsers(endNotifyGroupList)
        notifyUserSet.addAll(groupUsers.innerUsers)
        notifyUserSet.addAll(endNotifyUserList)

        // 获取拦截列表
        val interceptList = getInterceptList(interceptRecordList)

        endNotifyTypeList.forEach {
            when (it) {
                NotifyType.RTX -> {
                    val message = RtxUtil.makeEndMessage(projectName, pipelineName, buildNo, time, interceptList, url, notifyUserSet)
                    client.get(ServiceNotifyResource::class).sendRtxNotify(message)
                }
                NotifyType.WECHAT -> {
                    val message = WechatUtil.makeEndMessage(projectName, pipelineName, buildNo, time, interceptList, url, notifyUserSet)
                    client.get(ServiceNotifyResource::class).sendWechatNotify(message)
                }
                NotifyType.EMAIL -> {
                    val message = EmailUtil.makeEndMessage(projectName, pipelineName, buildNo, time, interceptList, url, notifyUserSet)
                    client.get(ServiceNotifyResource::class).sendEmailNotify(message)
                }
            }
        }
    }

    private fun getInterceptList(interceptRecordList: List<QualityRuleInterceptRecord>): List<String> {
        return interceptRecordList.filter { !it.pass }.map {
            val oppositeOperationName = ThresholdOperationUtil.getOperationOppositeName(it.operation)
            "${it.indicatorName}当前值(${it.actualValue}) $oppositeOperationName 期望值(${it.value})"
        }
    }

    fun getAuditUserList(projectId: String, pipelineId: String, buildId: String, taskId: String): Set<String> {
        val interceptList = historyService.serviceListByBuildIdAndResult(projectId, pipelineId, buildId, RuleInterceptResult.FAIL.name)
        val ruleIdList = interceptList.map { it.ruleId }

        val auditUserList = mutableSetOf<String>()
        val ruleRecordList = ruleService.serviceListRuleByIds(projectId, ruleIdList.toSet())
        ruleRecordList.forEach {
            val auditNotifyUserList = it.auditUserList ?: listOf()
            if (it.controlPoint.name == taskId) {
                auditUserList.addAll(auditNotifyUserList)
            }
        }

        return auditUserList
    }

    private fun getProjectName(projectId: String): String {
        val project = client.get(ServiceProjectResource::class).listByProjectCode(setOf(projectId)).data?.firstOrNull()
        return project?.project_name ?: throw OperationException("ProjectId: $projectId not exist")
    }

    private fun getPipelineName(projectId: String, pipelineId: String): String {
        val map = getPipelineIdToNameMap(projectId, setOf(pipelineId))
        return map[pipelineId] ?: ""
    }

    private fun getPipelineIdToNameMap(projectId: String, pipelineIdSet: Set<String>): Map<String, String> {
        return client.get(ServicePipelineResource::class).getPipelineNameByIds(projectId, pipelineIdSet).data!!
    }

    companion object {
        private val logger = LoggerFactory.getLogger(QualityRuleCheckService::class.java)
        private const val DETAIL_NOT_RUN_VALUE = "-1"
        val DETAIL_NOT_RUN_FLOAT_VALUE = BigDecimal(-1)
    }
}