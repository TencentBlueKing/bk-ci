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

package com.tencent.devops.quality.service.v2

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.quality.pojo.enums.QualityOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.quality.tables.records.TQualityRuleRecord
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.service.ServicePipelineTaskResource
import com.tencent.devops.process.api.template.ServicePTemplateResource
import com.tencent.devops.process.api.template.ServiceTemplateInstanceResource
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
import com.tencent.devops.process.pojo.template.OptionalTemplate
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v2.pojo.QualityControlPoint
import com.tencent.devops.quality.api.v2.pojo.QualityIndicator
import com.tencent.devops.quality.api.v2.pojo.QualityRule
import com.tencent.devops.quality.api.v2.pojo.request.CopyRuleRequest
import com.tencent.devops.quality.api.v2.pojo.request.RuleCreateRequest
import com.tencent.devops.quality.api.v2.pojo.request.RuleUpdateRequest
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleMatchTask
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleSummaryWithPermission
import com.tencent.devops.quality.api.v2.pojo.response.UserQualityRule
import com.tencent.devops.quality.constant.BK_USER_NO_OPERATE_INTERCEPT_RULE_PERMISSION
import com.tencent.devops.quality.dao.v2.QualityControlPointDao
import com.tencent.devops.quality.dao.v2.QualityRuleDao
import com.tencent.devops.quality.dao.v2.QualityRuleMapDao
import com.tencent.devops.quality.pojo.RefreshType
import com.tencent.devops.quality.pojo.RulePermission
import com.tencent.devops.quality.pojo.enum.RuleOperation
import com.tencent.devops.quality.pojo.enum.RuleRange
import com.tencent.devops.quality.service.QualityPermissionService
import com.tencent.devops.quality.util.ElementUtils
import java.time.LocalDateTime
import org.apache.commons.lang3.math.NumberUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class QualityRuleService @Autowired constructor(
    private val ruleOperationService: QualityRuleOperationService,
    private val qualityRuleDao: QualityRuleDao,
    private val ruleMapDao: QualityRuleMapDao,
    private val indicatorService: QualityIndicatorService,
    private val qualityControlPointDao: QualityControlPointDao,
    private val qualityControlPointService: QualityControlPointService,
    private val dslContext: DSLContext,
    private val client: Client,
    private val qualityCacheService: QualityCacheService,
    private val qualityPermissionService: QualityPermissionService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(QualityRuleService::class.java)
        private val RESOURCE_TYPE = AuthResourceType.QUALITY_RULE
    }

    fun hasCreatePermission(userId: String, projectId: String): Boolean {
        return qualityPermissionService.validateRulePermission(
            userId = userId,
            projectId = projectId,
            authPermission = AuthPermission.CREATE
        )
    }

    fun userCreate(userId: String, projectId: String, ruleRequest: RuleCreateRequest): String {
        val permission = AuthPermission.CREATE
        qualityPermissionService.validateRulePermission(
            userId = userId,
            projectId = projectId,
            authPermission = permission,
            message = MessageUtil.getMessageByLocale(
                BK_USER_NO_OPERATE_INTERCEPT_RULE_PERMISSION,
                I18nUtil.getLanguage(userId),
                arrayOf(permission.getI18n(I18nUtil.getLanguage(userId)))
            )
        )
        return serviceCreate(
            userId = userId,
            projectId = projectId,
            ruleRequest = ruleRequest
        )
    }

    fun serviceCreate(userId: String, projectId: String, ruleRequest: RuleCreateRequest): String {
        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)

            val ruleId = qualityRuleDao.create(
                dslContext = context,
                userId = userId,
                projectId = projectId,
                ruleRequest = ruleRequest
            )

            if (ruleRequest.operation == RuleOperation.END) {
                ruleOperationService.serviceSaveEndOperation(
                    ruleId = ruleId,
                    notifyUserList = ruleRequest.notifyUserList ?: listOf(),
                    notifyGroupList = ruleRequest.notifyGroupList?.map { HashUtil.decodeIdToLong(it) } ?: listOf(),
                    notifyTypeList = ruleRequest.notifyTypeList ?: listOf())
            } else if (ruleRequest.operation == RuleOperation.AUDIT) {
                ruleOperationService.serviceSaveAuditOperation(
                    ruleId = ruleId,
                    auditUserList = ruleRequest.auditUserList ?: listOf(),
                    auditTimeoutMinutes = ruleRequest.auditTimeoutMinutes ?: 15
                )
            }
            qualityPermissionService.createRuleResource(
                userId = userId,
                projectId = projectId,
                ruleId = ruleId,
                ruleName = ruleRequest.name
            )
            refreshRedis(projectId, ruleId)
            HashUtil.encodeLongId(ruleId)
        }
    }

    fun userUpdate(userId: String, projectId: String, ruleHashId: String, ruleRequest: RuleUpdateRequest): Boolean {
        val ruleId = HashUtil.decodeIdToLong(ruleHashId)
        logger.info("user($userId) update the rule($ruleId) in project($projectId): $ruleRequest")
        val permission = AuthPermission.EDIT
        qualityPermissionService.validateRulePermission(
            userId = userId,
            projectId = projectId,
            ruleId = ruleId,
            authPermission = permission,
            message = MessageUtil.getMessageByLocale(
                BK_USER_NO_OPERATE_INTERCEPT_RULE_PERMISSION,
                I18nUtil.getLanguage(userId),
                arrayOf(permission.getI18n(I18nUtil.getLanguage(userId)))
            )
        )
        dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            qualityRuleDao.update(context, userId, projectId, ruleId, ruleRequest)
            if (ruleRequest.operation == RuleOperation.END) {
                ruleOperationService.serviceUpdateEndOperation(
                    ruleId = ruleId,
                    notifyUserList = ruleRequest.notifyUserList ?: listOf(),
                    notifyGroupList = ruleRequest.notifyGroupList?.map { HashUtil.decodeIdToLong(it) } ?: listOf(),
                    notifyTypeList = ruleRequest.notifyTypeList ?: listOf())
            } else if (ruleRequest.operation == RuleOperation.AUDIT) {
                ruleOperationService.serviceUpdateAuditOperation(
                    ruleId = ruleId,
                    auditUserList = ruleRequest.auditUserList ?: listOf(),
                    auditTimeoutMinutes = ruleRequest.auditTimeoutMinutes ?: 15
                )
            }
            qualityPermissionService.modifyRuleResource(
                projectId = projectId,
                ruleId = ruleId,
                ruleName = ruleRequest.name
            )
            refreshRedis(projectId, ruleId)
        }
        return true
    }

    fun userUpdateEnable(userId: String, projectId: String, ruleHashId: String, enable: Boolean) {
        val ruleId = HashUtil.decodeIdToLong(ruleHashId)
        val permission = AuthPermission.ENABLE
        logger.info("user($userId) update the rule($ruleId) in project($projectId) to $enable")
        qualityPermissionService.validateRulePermission(
            userId = userId,
            projectId = projectId,
            ruleId = ruleId,
            authPermission = permission,
            message = MessageUtil.getMessageByLocale(
                BK_USER_NO_OPERATE_INTERCEPT_RULE_PERMISSION,
                I18nUtil.getLanguage(userId),
                arrayOf(permission.getI18n(I18nUtil.getLanguage(userId)))
            )
        )
        qualityRuleDao.updateEnable(dslContext = dslContext, ruleId = ruleId, enable = enable)
        refreshRedis(projectId, ruleId)
    }

    fun userDelete(userId: String, projectId: String, ruleHashId: String) {
        val ruleId = HashUtil.decodeIdToLong(ruleHashId)
        val ruleRecord = qualityRuleDao.get(dslContext, ruleId)
        val permission = AuthPermission.DELETE
        logger.info("user($userId) delete the rule($ruleId) in project($projectId)")
        qualityPermissionService.validateRulePermission(
            userId = userId,
            projectId = projectId,
            ruleId = ruleId,
            authPermission = permission,
            message = MessageUtil.getMessageByLocale(
                BK_USER_NO_OPERATE_INTERCEPT_RULE_PERMISSION,
                I18nUtil.getLanguage(userId),
                arrayOf(permission.getI18n(I18nUtil.getLanguage(userId)))
            )
        )
        qualityRuleDao.delete(dslContext, ruleId)
        qualityPermissionService.deleteRuleResource(projectId, ruleId)
        refreshDeletedRuleRedis(projectId, ruleRecord.indicatorRange, ruleRecord.pipelineTemplateRange)
    }

    fun serviceGet(ruleHashId: String): TQualityRuleRecord {
        val ruleId = HashUtil.decodeIdToLong(ruleHashId)
        return qualityRuleDao.get(dslContext, ruleId)
    }

    fun serviceListRules(projectId: String, startTime: LocalDateTime? = null): List<QualityRule> {
        val recordList = qualityRuleDao.list(dslContext, projectId, startTime)
        return batchGetRuleData(recordList ?: listOf()) ?: listOf()
    }

    fun serviceListByPipelineRange(projectId: String, pipelineId: String?): List<QualityRule> {
        val recordList = qualityRuleDao.listByPipelineRange(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        return batchGetRuleData(recordList ?: listOf()) ?: listOf()
    }

    fun serviceListByTemplateRange(projectId: String, templateId: String?): List<QualityRule> {
        val recordList = qualityRuleDao.listByTemplateRange(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId
        )
        return batchGetRuleData(recordList ?: listOf()) ?: listOf()
    }

    fun serviceListRuleByPosition(projectId: String, position: String): List<QualityRule> {
        val recordList = qualityRuleDao.listByPosition(dslContext, projectId, position)
        return batchGetRuleData(recordList ?: listOf()) ?: listOf()
    }

    fun serviceListRuleByIds(projectId: String, ruleIds: Collection<Long>): List<QualityRule> {
        val recordList = qualityRuleDao.list(dslContext, projectId, ruleIds)
        return batchGetRuleData(recordList ?: listOf()) ?: listOf()
    }

    fun userGetRule(userId: String, projectId: String, ruleHashId: String): UserQualityRule {
        val ruleId = HashUtil.decodeIdToLong(ruleHashId)
        val record = qualityRuleDao.get(dslContext, ruleId)

        // 查询规则数据
        val rule = doGetRuleData(record)

        // 过滤已删除的， 模板对应的流水线
        val pipelineIds = rule.range.toSet()
        val pipelineMap = if (pipelineIds.isNotEmpty()) {
            logger.info("get pipeline for project($projectId) ruleId($ruleId): $pipelineIds")
            client.get(ServicePipelineResource::class).getPipelineByIds(projectId, pipelineIds).data
                ?.map { it.pipelineId to it }?.toMap() ?: mapOf()
        } else {
            mapOf()
        }
        val range = pipelineMap.map { UserQualityRule.RangeItem(it.key, it.value.pipelineName) }.toMutableList()

        // 过滤已删除的模板
        val templateIds = rule.templateRange.toSet()
        val templateMap = if (templateIds.isNotEmpty()) {
            client.get(ServicePTemplateResource::class).listTemplateById(templateIds, projectId, null).data?.templates
        } else {
            mapOf()
        }
        val templateRange = templateMap?.map { UserQualityRule.RangeItem(it.key, it.value.name) } ?: listOf()

        // 获取模板对应的流水线总数
        val templatePipelineCount = client.get(ServiceTemplateInstanceResource::class)
            .countTemplateInstance(projectId, templateIds).data ?: 0

        return UserQualityRule(
            hashId = rule.hashId,
            name = rule.name,
            desc = rule.desc,
            indicators = rule.indicators,
            controlPoint = rule.controlPoint,
            range = range,
            templateRange = templateRange,
            pipelineCount = range.size + templatePipelineCount,
            operation = rule.operation,
            notifyTypeList = rule.notifyTypeList,
            notifyGroupList = rule.notifyGroupList?.map { HashUtil.encodeLongId(it.toLong()) },
            notifyUserList = rule.notifyUserList,
            auditUserList = rule.auditUserList,
            auditTimeoutMinutes = rule.auditTimeoutMinutes,
            interceptRecent = "",
            gatewayId = rule.gatewayId
        )
    }

    private fun batchGetRuleData(records: List<TQualityRuleRecord>): List<QualityRule>? {
        val qualityRuleRecordMap = records.map { it.id to it }.toMap()
        val ruleIds = qualityRuleRecordMap.keys
        val ruleMap = ruleMapDao.batchGet(dslContext, ruleIds)?.associateBy { it.ruleId }
        logger.info("QUALITY|get rule data for ruleIds: $ruleIds")
        val controlPointRecords = qualityControlPointDao.list(dslContext, records.map { it.controlPoint })
        val ruleOperationMap = ruleOperationService.serviceBatchGet(dslContext, ruleIds).map { it.ruleId to it }.toMap()
        if (ruleMap.isNullOrEmpty()) {
            return null
        }
        val resultList = mutableListOf<QualityRule>()
        records.map { record ->
            // 顺序遍历rule map生成每个指标实际的operation和threshold
            val mapRecord = ruleMap[record.id] ?: return@map
            val indicatorIds = mapRecord.indicatorIds.split(",")
                .filter { NumberUtils.isDigits(it) }.map { it.toLong() }

            // 查询控制点
            val controlPoint = qualityControlPointService.serviceGet(controlPointRecords, record.projectId)
            val dataControlPoint = QualityRule.RuleControlPoint(
                hashId = HashUtil.encodeLongId(record.id),
                name = record.controlPoint,
                cnName = ElementUtils.getElementCnName(record.controlPoint, record.projectId),
                position = ControlPointPosition.create(record.controlPointPosition),
                availablePosition = if (controlPoint?.availablePosition != null &&
                    !controlPoint.availablePosition.isNullOrBlank()) {
                    controlPoint.availablePosition.split(",").map { ControlPointPosition.create(it) }
                } else listOf()
            )
            // 查询红线通知方式
            val ruleOperation = ruleOperationMap[record.id]

            // 把指标的定义值换成实际的值
            val indicatorOperations = mapRecord.indicatorOperations.split(",")
            val indicatorThresholds = mapRecord.indicatorThresholds.split(",")
            val indicatorExtraMap = mutableMapOf<String, Pair<String, String>>()
            indicatorIds?.forEachIndexed { index, id ->
                indicatorExtraMap[HashUtil.encodeLongId(id)] = Pair(indicatorOperations[index], indicatorThresholds[index])
            }
            val dataIndicators = indicatorService.serviceList(indicatorIds).map {
                val pair = indicatorExtraMap[it.hashId]!!
                QualityIndicator(
                    hashId = it.hashId,
                    elementType = it.elementType,
                    elementDetail = it.elementDetail,
                    enName = it.enName,
                    cnName = it.cnName,
                    stage = it.stage,
                    operation = QualityOperation.valueOf(pair.first),
                    operationList = it.operationList,
                    threshold = pair.second,
                    thresholdType = it.thresholdType,
                    readOnly = it.readOnly,
                    type = it.type,
                    tag = it.tag,
                    metadataList = it.metadataList,
                    desc = it.desc,
                    logPrompt = it.logPrompt,
                    enable = it.enable,
                    range = it.range
                )
            }
            val qualityRule = QualityRule(
                hashId = HashUtil.encodeLongId(record.id),
                name = record.name,
                desc = record.desc,
                indicators = dataIndicators,
                controlPoint = dataControlPoint,
                range = if (record.indicatorRange.isNullOrBlank()) {
                    listOf()
                } else {
                    record.indicatorRange.split(",")
                },
                templateRange = if (record.pipelineTemplateRange.isNullOrBlank()) {
                    listOf()
                } else record.pipelineTemplateRange.split(","),
                operation = RuleOperation.valueOf(ruleOperation?.type ?: RuleOperation.END.name),
                notifyTypeList = if (ruleOperation == null || ruleOperation.notifyTypes.isNullOrBlank()) {
                    listOf()
                } else ruleOperation.notifyTypes.split(",").map { NotifyType.valueOf(it) },
                notifyGroupList = if (ruleOperation == null || ruleOperation.notifyGroupId.isNullOrBlank()) {
                    listOf()
                } else ruleOperation.notifyGroupId.split(","),
                notifyUserList = if (ruleOperation == null || ruleOperation.notifyUser.isNullOrBlank()) {
                    listOf()
                } else ruleOperation.notifyUser.split(","),
                auditUserList = if (ruleOperation == null || ruleOperation.auditUser.isNullOrBlank()) {
                    listOf()
                } else ruleOperation.auditUser.split(","),
                auditTimeoutMinutes = ruleOperation?.auditTimeout ?: 15,
                gatewayId = record.gatewayId,
                gateKeepers = listOf(),
                stageId = "1",
                status = null,
                taskSteps = listOf()
            )
            resultList.add(qualityRule)
        }
        return resultList
    }

    private fun doGetRuleData(record: TQualityRuleRecord): QualityRule {
        // 查询rule map
        val ruleId = record.id
        val mapRecord = ruleMapDao.get(dslContext, ruleId)
        logger.info("QUALITY|get rule data for ruleId($ruleId)")

        // 顺序遍历rule map生成每个指标实际的operation和threshold
        val indicatorIds = mapRecord.indicatorIds.split(",")
            .filter { NumberUtils.isDigits(it) }
            .map { it.toLong() }

        // 查询控制点
        val controlPointRecord = qualityControlPointService.serviceListByElementType(record.controlPoint)
        val controlPoint = qualityControlPointService.serviceGet(controlPointRecord, record.projectId)
        val dataControlPoint = QualityRule.RuleControlPoint(
            hashId = HashUtil.encodeLongId(record.id),
            name = record.controlPoint,
            cnName = ElementUtils.getElementCnName(record.controlPoint, record.projectId),
            position = ControlPointPosition.create(record.controlPointPosition),
            availablePosition = if (controlPoint?.availablePosition != null &&
                !controlPoint.availablePosition.isNullOrBlank()) {
                controlPoint.availablePosition.split(",").map { ControlPointPosition.create(it) }
            } else listOf()
        )

        // 查询指标通知
        val ruleOperation = ruleOperationService.serviceGet(dslContext, ruleId)

        // 把指标的定义值换成实际的值
        val indicatorOperations = mapRecord.indicatorOperations.split(",")
        val indicatorThresholds = mapRecord.indicatorThresholds.split(",")
        val indicatorExtraMap = mutableMapOf<String, Pair<String, String>>()
        indicatorIds.forEachIndexed { index, id ->
            indicatorExtraMap[HashUtil.encodeLongId(id)] = Pair(indicatorOperations[index], indicatorThresholds[index])
        }
        val dataIndicators = indicatorService.serviceList(indicatorIds).map {
            val pair = indicatorExtraMap[it.hashId]!!
            QualityIndicator(
                hashId = it.hashId,
                elementType = it.elementType,
                elementDetail = it.elementDetail,
                enName = it.enName,
                cnName = it.cnName,
                stage = it.stage,
                operation = QualityOperation.valueOf(pair.first),
                operationList = it.operationList,
                threshold = pair.second,
                thresholdType = it.thresholdType,
                readOnly = it.readOnly,
                type = it.type,
                tag = it.tag,
                metadataList = it.metadataList,
                desc = it.desc,
                logPrompt = it.logPrompt,
                enable = it.enable,
                range = it.range
            )
        }
        return QualityRule(
            hashId = HashUtil.encodeLongId(record.id),
            name = record.name,
            desc = record.desc,
            indicators = dataIndicators,
            controlPoint = dataControlPoint,
            range = if (record.indicatorRange.isNullOrBlank()) {
                listOf()
            } else {
                record.indicatorRange.split(",")
            },
            templateRange = if (record.pipelineTemplateRange.isNullOrBlank()) {
                listOf()
            } else record.pipelineTemplateRange.split(","),
            operation = RuleOperation.valueOf(ruleOperation.type),
            notifyTypeList = if (ruleOperation.notifyTypes.isNullOrBlank()) {
                listOf()
            } else ruleOperation.notifyTypes.split(",").map { NotifyType.valueOf(it) },
            notifyGroupList = if (ruleOperation.notifyGroupId.isNullOrBlank()) {
                listOf()
            } else ruleOperation.notifyGroupId.split(","),
            notifyUserList = if (ruleOperation.notifyUser.isNullOrBlank()) {
                listOf()
            } else ruleOperation.notifyUser.split(","),
            auditUserList = if (ruleOperation.auditUser.isNullOrBlank()) {
                listOf()
            } else ruleOperation.auditUser.split(","),
            auditTimeoutMinutes = ruleOperation.auditTimeout ?: 15,
            gatewayId = record.gatewayId,
            gateKeepers = listOf(),
            stageId = "1",
            status = null,
            taskSteps = listOf()
        )
    }

    fun listRuleDataSummary(
        userId: String,
        projectId: String,
        offset: Int,
        limit: Int
    ): Pair<Long, List<QualityRuleSummaryWithPermission>> {
        val allRulesIds = qualityRuleDao.listIds(
            dslContext = dslContext,
            projectId = projectId
        ).map { it.value1() }
        val hasListPermissionRuleIds = qualityPermissionService.filterListPermissionRules(
            userId = userId,
            projectId = projectId,
            allRulesIds = allRulesIds
        )
        if (hasListPermissionRuleIds.isEmpty())
            return Pair(0, listOf())
        val count = hasListPermissionRuleIds.size
        val finalLimit = if (limit == -1) count else limit
        val ruleRecordList = qualityRuleDao.listByIds(
            dslContext = dslContext,
            projectId = projectId,
            rulesId = hasListPermissionRuleIds,
            offset = offset,
            limit = finalLimit
        )
        val permissionMap = qualityPermissionService.filterRules(
            userId = userId,
            projectId = projectId,
            bkAuthPermissionSet = setOf(AuthPermission.EDIT, AuthPermission.DELETE, AuthPermission.ENABLE)
        )
        // 获取控制点信息
        val controlPointMap = mutableMapOf<String, QualityControlPoint>()
        qualityControlPointService.serviceList(projectId).forEach { controlPointMap[it.type] = it }

        // 获取rule的详细数据
        logger.info("serviceList rule ids for project($projectId): $hasListPermissionRuleIds")
        val ruleDetailMap = ruleMapDao.batchGet(dslContext, hasListPermissionRuleIds)?.map { it.ruleId to it }?.toMap()
            ?: mapOf()

        // 批量获取流水线信息
        val pipelineIds = mutableSetOf<String>()
        ruleRecordList?.forEach { pipelineIds.addAll(it.indicatorRange.split(",")) }
        val pipelineIdInfoMap = getPipelineIdToNameMap(projectId, pipelineIds)
        val pipelineElementsMap = client.get(ServicePipelineTaskResource::class).list(projectId, pipelineIds).data
            ?: mapOf()

        // 批量获取模板信息
        val templateIds = mutableSetOf<String>()
        ruleRecordList?.filter { !it.pipelineTemplateRange.isNullOrBlank() }?.forEach {
            templateIds.addAll(it.pipelineTemplateRange.split(","))
        }
        val srcTemplateIdMap = if (templateIds.isNotEmpty()) client.get(ServicePTemplateResource::class)
            .listTemplateById(templateIds, projectId, null).data?.templates ?: mapOf()
        else mapOf()
        val templateIdMap = mutableMapOf<String, OptionalTemplate>()
        srcTemplateIdMap.entries.forEach { templateIdMap[it.value.templateId] = it.value }

        val templatePipelineCountMap = client.get(ServiceTemplateInstanceResource::class)
            .countTemplateInstanceDetail(projectId, templateIds).data ?: mapOf()

        // 批量获取元数据信息
        var ruleMetadataSet = mutableSetOf<Long>()
        ruleRecordList?.forEach { record ->
            val ruleDetail = ruleDetailMap[record.id]
            val ruleIndicators = if (ruleDetail == null || ruleDetail.indicatorIds.isNullOrBlank()) listOf()
            else ruleDetail.indicatorIds.split(",").map { it.toLong() }
            ruleMetadataSet.addAll(ruleIndicators)
        }
        val indicatorsMap = indicatorService.serviceList(ruleMetadataSet).associateBy {
            HashUtil.decodeIdToLong(it.hashId)
        }

        val list = ruleRecordList?.map { rule ->
            // 获取所有流水线
            val ruleDetail = ruleDetailMap[rule.id]
            logger.info("serviceList rule detail ids for project($projectId): ${ruleDetail?.id}")

            val controlPoint = controlPointMap[rule.controlPoint]
            val ruleIndicators = if (ruleDetail == null || ruleDetail.indicatorIds.isNullOrBlank()) listOf()
            else ruleDetail.indicatorIds.split(",").map { it.toLong() }

            // get rule indicator map
            var indicators = mutableListOf<QualityIndicator>()
            ruleIndicators.forEach {
                if (indicatorsMap.containsKey(it)) {
                    indicators.add(indicatorsMap[it]!!)
                }
            }
            logger.info(
                "serviceList rule indicator ids for project($projectId): ${indicators.map { it.enName }}"
            )
            val indicatorOperations = ruleDetail?.indicatorOperations?.split(",") ?: listOf()
            val indicatorThresholds = ruleDetail?.indicatorThresholds?.split(",") ?: listOf()
            val ruleIndicatorMap = ruleIndicators.mapIndexed { index, id ->
                id to Pair(indicatorOperations[index], indicatorThresholds[index])
            }.toMap()

            // 获取结果各字段数据
            val pipelineSummary = getPipelineLackSummary(
                projectId = projectId,
                rule = rule,
                pipelineIdInfoMap = pipelineIdInfoMap,
                indicators = indicators,
                controlPoint = controlPoint,
                pipelineElementsMap = pipelineElementsMap
            )
            val templateSummary = getTemplateLackSummary(projectId, rule, indicators, controlPoint, templateIdMap)
            val summaryIndicatorList = getSummaryIndicatorList(indicators, ruleIndicatorMap)
            val ruleSummaryControlPoint =
                QualityRuleSummaryWithPermission.RuleSummaryControlPoint(
                    hashId = controlPoint?.hashId ?: "",
                    name = controlPoint?.type ?: "",
                    cnName = controlPoint?.name ?: ""
                )
            val pipelineCount = rule.indicatorRange.split(",").filter { pipelineIdInfoMap.containsKey(it) }.size
            val ruleTemplateIds = if (rule.pipelineTemplateRange.isNullOrBlank()) {
                listOf()
            } else rule.pipelineTemplateRange.split(",")
            val templatePipelineCount = templatePipelineCountMap.filter { it.key in ruleTemplateIds }.values.sum()
            val rulePermission = getRulePermission(permissionMap, rule)

            // 最后生成结果
            QualityRuleSummaryWithPermission(
                ruleHashId = HashUtil.encodeLongId(rule.id),
                name = rule.name,
                controlPoint = ruleSummaryControlPoint,
                indicatorList = summaryIndicatorList,
                range = RuleRange.PART_BY_NAME,
                rangeSummary = templateSummary.plus(pipelineSummary),
                pipelineCount = pipelineCount + templatePipelineCount,
                pipelineExecuteCount = rule.executeCount,
                interceptTimes = rule.interceptTimes,
                enable = rule.enable,
                permissions = rulePermission,
                gatewayId = rule.gatewayId
            )
        } ?: listOf()
        return Pair(count.toLong(), list)
    }

    private fun getRulePermission(
        permissionMap: Map<AuthPermission, List<Long>>,
        rule: TQualityRuleRecord
    ): RulePermission {
        val canEditList = permissionMap[AuthPermission.EDIT]!!
        val canDeleteList = permissionMap[AuthPermission.DELETE]!!
        val canEnableList = permissionMap[AuthPermission.ENABLE]!!

        val canEdit = canEditList.contains(rule.id)
        val canDelete = canDeleteList.contains(rule.id)
        val canEnable = canEnableList.contains(rule.id)
        return RulePermission(canEdit, canDelete, canEnable)
    }

    // 生成Indicator汇总结果
    private fun getSummaryIndicatorList(
        indicators: List<QualityIndicator>,
        ruleIndicatorMap: Map<Long, Pair<String, String>>
    ): List<QualityRuleSummaryWithPermission.RuleSummaryIndicator> {
        return indicators.map {
            val pair = ruleIndicatorMap[HashUtil.decodeIdToLong(it.hashId)]
            QualityRuleSummaryWithPermission.RuleSummaryIndicator(
                it.hashId,
                it.enName,
                it.cnName,
                pair?.first ?: "",
                pair?.second ?: ""
            )
        }
    }

    // 获取rule里面的流水线，相对indicators和控制点，还缺哪些指标
    private fun getPipelineLackSummary(
        projectId: String,
        rule: TQualityRuleRecord,
        pipelineIdInfoMap: Map<String, SimplePipeline>,
        indicators: List<QualityIndicator>,
        controlPoint: QualityControlPoint?,
        pipelineElementsMap: Map<String, List<PipelineModelTask>>
    ): List<QualityRuleSummaryWithPermission.RuleRangeSummary> {

        val pipelineIds = rule.indicatorRange.split(",")
        val indicatorElement = indicators.map { it.elementType }.toSet()
        return pipelineIdInfoMap.filter { it.key in pipelineIds }.map { map ->
            val pipelineId = map.key
            val info = map.value
            val pipelineElement = pipelineElementsMap[pipelineId] ?: listOf()
            val pipelineElementCodes = pipelineElement.map { it.atomCode }
            val lackElements = indicatorElement.minus(pipelineElementCodes).toMutableSet()
            if (controlPoint != null && !pipelineElementCodes.contains(controlPoint.type)) {
                lackElements.add(controlPoint.type)
            }
            QualityRuleSummaryWithPermission.RuleRangeSummary(
                id = info.pipelineId,
                name = info.pipelineName,
                type = "PIPELINE",
                lackElements = lackElements.map { ElementUtils.getElementCnName(it, projectId) }
            )
        }
    }

    // 获取rule里面的模板，相对indicators和控制点，还缺哪些指标
    private fun getTemplateLackSummary(
        projectId: String,
        rule: TQualityRuleRecord,
        indicators: List<QualityIndicator>,
        controlPoint: QualityControlPoint?,
        templateIdMap: Map<String, OptionalTemplate>
    ): List<QualityRuleSummaryWithPermission.RuleRangeSummary> {

        val templateIds = if (rule.pipelineTemplateRange.isNullOrBlank()) listOf()
        else rule.pipelineTemplateRange.split(",")
        val indicatorElement = indicators.map { it.elementType }.toSet()
        return templateIdMap.filter { templateIds.contains(it.key) }.map { map ->
            val template = map.value
            val templateElements = mutableListOf<Element>()
            template.stages.map { stage ->
                stage.containers.map { container -> templateElements.addAll(container.elements) }
            }
            val templateElementCodes = templateElements.map { it.getAtomCode() }.toSet()
            val lackElements = indicatorElement.minus(templateElementCodes).toMutableSet()
            if (controlPoint != null && !templateElementCodes.contains(controlPoint.type)) {
                lackElements.add(controlPoint.type)
            }
            QualityRuleSummaryWithPermission.RuleRangeSummary(
                id = template.templateId,
                name = template.name,
                type = "TEMPLATE",
                lackElements = lackElements.map { ElementUtils.getElementCnName(it, projectId) }
            )
        }
    }

    fun plusInterceptTimes(ruleId: Long) {
        qualityRuleDao.plusInterceptTimes(dslContext, ruleId)
    }

    fun plusExecuteCount(ruleId: Long) {
        qualityRuleDao.plusExecuteCount(dslContext, ruleId)
    }

    private fun getPipelineIdToNameMap(projectId: String, pipelineIdSet: Set<String>): Map<String, SimplePipeline> {
        val pipelineList = client.get(ServicePipelineResource::class).getPipelineByIds(projectId, pipelineIdSet).data!!
        return pipelineList.map { it.pipelineId to it }.toMap()
    }

    fun count(projectId: String): Long {
        return qualityRuleDao.count(dslContext, projectId)
    }

    fun copyRule(request: CopyRuleRequest): List<String> {
        logger.info("copy rule request: $request")
        val ruleList = qualityRuleDao.list(dslContext, request.sourceProjectId) ?: return listOf()
        val filterRuleList = ruleList.filter {
            !it.pipelineTemplateRange.isNullOrBlank() &&
                it.pipelineTemplateRange.split(",").contains(request.sourceTemplateId)
        }
        logger.info("start to copy rule list size: ${filterRuleList.size}")
        return filterRuleList.map { rule ->
            logger.info("start to copy rule : ${rule.name}")
            val ruleData = doGetRuleData(rule)

            val createRequest = RuleCreateRequest(
                name = ruleData.name,
                desc = ruleData.desc,
                indicatorIds = ruleData.indicators.map {
                    RuleCreateRequest.CreateRequestIndicator(it.hashId, it.operation.name, it.threshold)
                }, // indicatorIds
                controlPoint = ruleData.controlPoint.name,
                controlPointPosition = ruleData.controlPoint.position.name,
                range = listOf(),
                templateRange = listOf(request.targetTemplateId),
                operation = ruleData.operation,
                notifyTypeList = listOf(),
                notifyGroupList = listOf(),
                notifyUserList = listOf(),
                auditUserList = listOf(),
                auditTimeoutMinutes = ruleData.auditTimeoutMinutes,
                gatewayId = ruleData.gatewayId
            )
            serviceCreate(request.userId, request.targetProjectId, createRequest)
        }
    }

    fun listMatchTask(ruleList: List<QualityRule>): List<QualityRuleMatchTask> {
        val matchTaskList = mutableListOf<QualityRuleMatchTask>()
        ruleList.groupBy { it.controlPoint.position.name }.forEach { (_, rules) ->

            // 按照控制点拦截位置再分组
            rules.groupBy { it.controlPoint.position }.forEach { (position, positionRules) ->
                val controlPoint = positionRules.first().controlPoint
                val taskRuleList = mutableListOf<QualityRuleMatchTask.RuleMatchRule>()
                val taskThresholdList = mutableListOf<QualityRuleMatchTask.RuleThreshold>()
                val taskAuditUserList = mutableSetOf<String>()

                positionRules.forEach { rule ->
                    // 获取规则列表
                    taskRuleList.add(QualityRuleMatchTask.RuleMatchRule(rule.hashId, rule.name, rule.gatewayId))

                    // 获取阈值列表
                    taskThresholdList.addAll(rule.indicators.map { indicator ->
                        QualityRuleMatchTask.RuleThreshold(
                            indicator.hashId,
                            indicator.cnName,
                            indicator.metadataList.map { it.name },
                            indicator.operation,
                            indicator.threshold
                        )
                    })

                    // 获取审核用户列表
                    taskAuditUserList.addAll(
                        if (rule.operation == RuleOperation.AUDIT) {
                            rule.auditUserList?.toSet() ?: setOf()
                        } else {
                            setOf()
                        }
                    )
                }
                // 生成结果
                matchTaskList.add(
                    QualityRuleMatchTask(
                        controlPoint.name, controlPoint.cnName, position,
                        taskRuleList, taskThresholdList, taskAuditUserList
                    )
                )
            }
        }
        return matchTaskList
    }

    fun getProjectRuleList(projectId: String, pipelineId: String?, templateId: String?): List<QualityRule> {
        val watcher = Watcher(id = "QUALITY|getProjectRuleList|$projectId|$pipelineId|$templateId")
        var ruleList: List<QualityRule>
        try {
            watcher.start("ruleList")
            ruleList = when {
                pipelineId != null -> {
                    serviceListByPipelineRange(projectId, pipelineId)
                }
                templateId != null -> {
                    serviceListByTemplateRange(projectId, templateId)
                }
                else -> {
                    serviceListRules(projectId)
                }
            }
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher = watcher, warnThreshold = 500, errorThreshold = 3000)
        }
        return ruleList
    }

    private fun refreshRedis(projectId: String, ruleId: Long?) {
        if (ruleId == null) {
            return
        }
        val ruleRecord = qualityRuleDao.getById(dslContext, ruleId) ?: return
        val pipelineStr = ruleRecord.indicatorRange
        val templateStr = ruleRecord.pipelineTemplateRange
        logger.info("refreshRedis $projectId| $ruleId| $pipelineStr| $templateStr")
        if (!pipelineStr.isNullOrBlank()) {
            val pipelineList = pipelineStr.split(",")
            pipelineList.forEach { pipelineId ->
                val filterRuleList = getProjectRuleList(projectId, pipelineId, null)
                val ruleList = listMatchTask(filterRuleList)
                qualityCacheService.refreshCache(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    templateId = null,
                    ruleTasks = ruleList,
                    type = RefreshType.OPERATE
                )
                logger.info("refreshRedis pipeline $projectId|$pipelineId| $ruleId | $ruleList")
            }
        }
        if (!templateStr.isNullOrBlank()) {
            val templateList = templateStr.split(",")
            templateList.forEach { templateId ->
                val filterRuleList = getProjectRuleList(projectId, null, templateId)
                val ruleList = listMatchTask(filterRuleList)
                qualityCacheService.refreshCache(
                    projectId = projectId,
                    pipelineId = null,
                    templateId = templateId,
                    ruleTasks = ruleList,
                    type = RefreshType.OPERATE
                )
                logger.info("refreshRedis template $projectId|$templateId| $ruleId| $ruleList")
            }
        }
    }

    private fun refreshDeletedRuleRedis(projectId: String, pipelineRange: String, templateRange: String) {
        logger.info("refreshRedis $projectId| $pipelineRange| $templateRange")
        if (!pipelineRange.isNullOrBlank()) {
            val pipelineList = pipelineRange.split(",")
            pipelineList.forEach { pipelineId ->
                val filterRuleList = getProjectRuleList(projectId, pipelineId, null)
                val ruleList = listMatchTask(filterRuleList)
                qualityCacheService.refreshCache(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    templateId = null,
                    ruleTasks = ruleList,
                    type = RefreshType.OPERATE
                )
                logger.info("refreshRedis pipeline $projectId|$pipelineId | $ruleList")
            }
        }
        if (!templateRange.isNullOrBlank()) {
            val templateList = templateRange.split(",")
            templateList.forEach { templateId ->
                val filterRuleList = getProjectRuleList(projectId, null, templateId)
                val ruleList = listMatchTask(filterRuleList)
                qualityCacheService.refreshCache(
                    projectId = projectId,
                    pipelineId = null,
                    templateId = templateId,
                    ruleTasks = ruleList,
                    type = RefreshType.OPERATE
                )
                logger.info("refreshRedis template $projectId|$templateId | $ruleList")
            }
        }
    }
}
