package com.tencent.devops.quality.service.v2

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.QualityAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.model.quality.tables.records.TQualityRuleRecord
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.service.ServicePipelineTaskResource
import com.tencent.devops.process.api.template.ServiceTemplateInstanceResource
import com.tencent.devops.process.api.template.ServiceTemplateResource
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
import com.tencent.devops.process.pojo.template.OptionalTemplate
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v2.pojo.QualityControlPoint
import com.tencent.devops.quality.api.v2.pojo.QualityIndicator
import com.tencent.devops.quality.api.v2.pojo.QualityRule
import com.tencent.devops.quality.api.v2.pojo.enums.QualityOperation
import com.tencent.devops.quality.api.v2.pojo.request.CopyRuleRequest
import com.tencent.devops.quality.api.v2.pojo.request.RuleCreateRequest
import com.tencent.devops.quality.api.v2.pojo.request.RuleUpdateRequest
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleSummaryWithPermission
import com.tencent.devops.quality.api.v2.pojo.response.UserQualityRule
import com.tencent.devops.quality.dao.v2.QualityRuleDao
import com.tencent.devops.quality.dao.v2.QualityRuleMapDao
import com.tencent.devops.quality.pojo.RulePermission
import com.tencent.devops.quality.pojo.enum.NotifyType
import com.tencent.devops.quality.pojo.enum.RuleOperation
import com.tencent.devops.quality.pojo.enum.RuleRange
import com.tencent.devops.quality.util.ElementUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class QualityRuleService @Autowired constructor(
    private val ruleOperationService: QualityRuleOperationService,
    private val qualityRuleDao: QualityRuleDao,
    private val ruleMapDao: QualityRuleMapDao,
    private val indicatorService: QualityIndicatorService,
    private val qualityControlPointService: QualityControlPointService,
    private val dslContext: DSLContext,
    private val client: Client,
    private val bkAuthPermissionApi: AuthPermissionApi,
    private val bkAuthResourceApi: AuthResourceApi,
    private val serviceCode: QualityAuthServiceCode
) {

    companion object {
        private val logger = LoggerFactory.getLogger(QualityRuleService::class.java)
        private val RESOURCE_TYPE = AuthResourceType.QUALITY_RULE
    }

    fun hasCreatePermission(userId: String, projectId: String): Boolean {
        return validatePermission(userId, projectId, AuthPermission.CREATE)
    }

    fun userCreate(userId: String, projectId: String, ruleRequest: RuleCreateRequest): String {
        validatePermission(userId, projectId, AuthPermission.CREATE, "用户在项目($projectId)下没有创建拦截规则权限")
        return serviceCreate(userId, projectId, ruleRequest)
    }

    fun serviceCreate(userId: String, projectId: String, ruleRequest: RuleCreateRequest): String {
        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)

            val ruleId = qualityRuleDao.create(context, userId, projectId, ruleRequest)

            if (ruleRequest.operation == RuleOperation.END) {
                ruleOperationService.serviceSaveEndOperation(
                        ruleId,
                        ruleRequest.notifyUserList ?: listOf(),
                        ruleRequest.notifyGroupList?.map { HashUtil.decodeIdToLong(it) } ?: listOf(),
                        ruleRequest.notifyTypeList ?: listOf())
            } else if (ruleRequest.operation == RuleOperation.AUDIT) {
                ruleOperationService.serviceSaveAuditOperation(
                        ruleId,
                        ruleRequest.auditUserList ?: listOf(),
                        ruleRequest.auditTimeoutMinutes ?: 15
                )
            }
            createResource(userId, projectId, ruleId, ruleRequest.name)
            HashUtil.encodeLongId(ruleId)
        }
    }

    fun userUpdate(userId: String, projectId: String, ruleHashId: String, ruleRequest: RuleUpdateRequest): Boolean {
        val ruleId = HashUtil.decodeIdToLong(ruleHashId)
        logger.info("user($userId) update the rule($ruleId) in project($projectId): $ruleRequest")
        validatePermission(userId, projectId, ruleId, AuthPermission.EDIT, "用户在项目($projectId)下没拦截规则($ruleHashId)的编辑权限")
        dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            qualityRuleDao.update(context, userId, projectId, ruleId, ruleRequest)
            if (ruleRequest.operation == RuleOperation.END) {
                ruleOperationService.serviceUpdateEndOperation(
                        ruleId,
                        ruleRequest.notifyUserList ?: listOf(),
                        ruleRequest.notifyGroupList?.map { HashUtil.decodeIdToLong(it) } ?: listOf(),
                        ruleRequest.notifyTypeList ?: listOf())
            } else if (ruleRequest.operation == RuleOperation.AUDIT) {
                ruleOperationService.serviceUpdateAuditOperation(
                        ruleId,
                        ruleRequest.auditUserList ?: listOf(),
                        ruleRequest.auditTimeoutMinutes ?: 15
                )
            }
            modifyResource(projectId, ruleId, ruleRequest.name)
        }
        return true
    }

    fun userUpdateEnable(userId: String, projectId: String, ruleHashId: String, enable: Boolean) {
        val ruleId = HashUtil.decodeIdToLong(ruleHashId)
        logger.info("user($userId) update the rule($ruleId) in project($projectId) to $enable")
        validatePermission(userId, projectId, ruleId, AuthPermission.ENABLE, "用户在项目($projectId)下没拦截规则($ruleHashId)的停用/启用权限")
        qualityRuleDao.updateEnable(dslContext, ruleId, enable)
    }

    fun userDelete(userId: String, projectId: String, ruleHashId: String) {
        val ruleId = HashUtil.decodeIdToLong(ruleHashId)
        logger.info("user($userId) delete the rule($ruleId) in project($projectId)")
        validatePermission(userId, projectId, ruleId, AuthPermission.ENABLE, "用户在项目($projectId)下没拦截规则($ruleHashId)的停用/启用权限")
        qualityRuleDao.delete(dslContext, ruleId)
        deleteResource(projectId, ruleId)
    }

    fun serviceGet(ruleHashId: String): TQualityRuleRecord {
        val ruleId = HashUtil.decodeIdToLong(ruleHashId)
        return qualityRuleDao.get(dslContext, ruleId)
    }

    fun serviceListRules(projectId: String, startTime: LocalDateTime? = null): List<QualityRule> {
        return qualityRuleDao.list(dslContext, projectId, startTime)?.map {
            doGetRuleData(it)
        } ?: listOf()
    }

    fun serviceListRuleByPosition(projectId: String, position: String): List<QualityRule> {
        return qualityRuleDao.listByPosition(dslContext, projectId, position)?.map {
            doGetRuleData(it)
        } ?: listOf()
    }

    fun serviceListRuleByIds(projectId: String, ruleIds: Collection<Long>): List<QualityRule> {
        return qualityRuleDao.list(dslContext, projectId, ruleIds)?.map {
            doGetRuleData(it)
        } ?: listOf()
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
            client.get(ServiceTemplateResource::class).listTemplateById(templateIds, null).data?.templates
        } else {
            mapOf()
        }
        val templateRange = templateMap?.map { UserQualityRule.RangeItem(it.key, it.value.name) } ?: listOf()

        // 获取模板对应的流水线总数
        val templatePipelineCount = client.get(ServiceTemplateInstanceResource::class).countTemplateInstance(projectId, templateIds).data ?: 0

        return UserQualityRule(
                rule.hashId,
                rule.name,
                rule.desc,
                rule.indicators,
                rule.controlPoint,
                range,
                templateRange,
                range.size + templatePipelineCount,
                rule.operation,
                rule.notifyTypeList,
                rule.notifyGroupList?.map { HashUtil.encodeLongId(it.toLong()) },
                rule.notifyUserList,
                rule.auditUserList,
                rule.auditTimeoutMinutes,
                "",
                rule.gatewayId
        )
    }

    private fun doGetRuleData(record: TQualityRuleRecord): QualityRule {
        // 查询rule map
        val ruleId = record.id
        val mapRecord = ruleMapDao.get(dslContext, ruleId)
        logger.info("get rule data for ruleId($ruleId): $mapRecord")

        // 顺序遍历rule map生成每个指标实际的operation和threshold
        val indicatorIds = mapRecord.indicatorIds.split(",").map { it.toLong() }

        // 查询控制点
        val controlPoint = qualityControlPointService.serviceGet(record.controlPoint)
        val dataControlPoint = QualityRule.RuleControlPoint(
                HashUtil.encodeLongId(record.id),
                record.controlPoint,
                ElementUtils.getElementCnName(record.controlPoint, record.projectId),
                ControlPointPosition(record.controlPointPosition),
                if (controlPoint?.availablePosition != null && !controlPoint.availablePosition.isNullOrBlank())
                    controlPoint.availablePosition.split(",").map { ControlPointPosition(it) }
                else listOf()
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
                    it.hashId,
                    it.elementType,
                    it.elementDetail,
                    it.enName,
                    it.cnName,
                    it.stage,
                    QualityOperation.valueOf(pair.first),
                    it.operationList,
                    pair.second,
                    it.thresholdType,
                    it.readOnly,
                    it.type,
                    it.tag,
                    it.metadataList,
                    it.desc,
                    it.logPrompt
            )
        }
        return QualityRule(
                HashUtil.encodeLongId(record.id),
                record.name,
                record.desc,
                dataIndicators,
                dataControlPoint,
                if (record.indicatorRange.isNullOrBlank()) listOf() else record.indicatorRange.split(","),
                if (record.pipelineTemplateRange.isNullOrBlank()) listOf() else record.pipelineTemplateRange.split(","),
                RuleOperation.valueOf(ruleOperation.type),
                if (ruleOperation.notifyTypes.isNullOrBlank()) listOf() else ruleOperation.notifyTypes.split(",").map { NotifyType.valueOf(it) },
                if (ruleOperation.notifyGroupId.isNullOrBlank()) listOf() else ruleOperation.notifyGroupId.split(","),
                if (ruleOperation.notifyUser.isNullOrBlank()) listOf() else ruleOperation.notifyUser.split(","),
                if (ruleOperation.auditUser.isNullOrBlank()) listOf() else ruleOperation.auditUser.split(","),
                ruleOperation.auditTimeout ?: 15,
                record.gatewayId
        )
    }

    fun listRuleDataSummary(userId: String, projectId: String, offset: Int, limit: Int): Pair<Long, List<QualityRuleSummaryWithPermission>> {
        val count = qualityRuleDao.count(dslContext, projectId)
        val finalLimit = if (limit == -1) count.toInt() else limit
        val ruleRecordList = qualityRuleDao.list(dslContext, projectId, offset, finalLimit)
        val permissionMap = filterRules(userId, projectId, setOf(AuthPermission.EDIT, AuthPermission.DELETE, AuthPermission.ENABLE))
        // 获取控制点信息
        val controlPointMap = mutableMapOf<String, QualityControlPoint>()
        qualityControlPointService.serviceList(projectId).forEach { controlPointMap[it.type] = it }

        // 获取rule的详细数据
        val ruleIds = ruleRecordList?.map { it.id } ?: listOf()
        logger.info("serviceList rule ids for project($projectId): $ruleIds")
        val ruleDetailMap = ruleMapDao.batchGet(dslContext, ruleIds)?.map { it.ruleId to it }?.toMap() ?: mapOf()

        // 批量获取流水线信息
        val pipelineIds = mutableSetOf<String>()
        ruleRecordList?.forEach { pipelineIds.addAll(it.indicatorRange.split(",")) }
        val pipelineIdInfoMap = getPipelineIdToNameMap(projectId, pipelineIds)
        val pipelineElementsMap = client.get(ServicePipelineTaskResource::class).list(projectId, pipelineIds).data
                ?: mapOf()

        // 批量获取模板信息
        val templateIds = mutableSetOf<String>()
        ruleRecordList?.filter { !it.pipelineTemplateRange.isNullOrBlank() }?.forEach { templateIds.addAll(it.pipelineTemplateRange.split(",")) }
        val templateIdMap = if (templateIds.isNotEmpty()) client.get(ServiceTemplateResource::class)
                .listTemplateById(templateIds, null).data?.templates ?: mapOf()
        else mapOf()
        val templatePipelineCountMap = client.get(ServiceTemplateInstanceResource::class)
                .countTemplateInstanceDetail(projectId, templateIds).data ?: mapOf()

        val list = ruleRecordList?.map { rule ->
            // 获取所有流水线
            val ruleDetail = ruleDetailMap[rule.id]
            logger.info("serviceList rule detail ids for project($projectId): $ruleDetail")

            val controlPoint = controlPointMap[rule.controlPoint]
            val ruleIndicators = if (ruleDetail == null || ruleDetail.indicatorIds.isNullOrBlank()) listOf()
            else ruleDetail.indicatorIds.split(",").map { it.toLong() }

            // get rule indicator map
            val indicators = indicatorService.serviceList(ruleIndicators)
            logger.info("serviceList rule indicator ids for project($projectId): ${indicators.map { it.enName }}")
            val indicatorOperations = ruleDetail?.indicatorOperations?.split(",") ?: listOf()
            val indicatorThresholds = ruleDetail?.indicatorThresholds?.split(",") ?: listOf()
            val ruleIndicatorMap = ruleIndicators.mapIndexed { index, id ->
                id to Pair(indicatorOperations[index], indicatorThresholds[index])
            }.toMap()

            // 获取结果各字段数据
            val pipelineSummary = getPipelineLackSummary(projectId, rule, pipelineIdInfoMap, indicators, controlPoint, pipelineElementsMap)
            val templateSummary = getTemplateLackSummary(projectId, rule, indicators, controlPoint, templateIdMap)
            val summaryIndicatorList = getSummaryIndicatorList(indicators, ruleIndicatorMap)
            val ruleSummaryControlPoint =
                    QualityRuleSummaryWithPermission.RuleSummaryControlPoint(
                            controlPoint?.hashId ?: "",
                            controlPoint?.type ?: "",
                            controlPoint?.name ?: "")
            val pipelineCount = rule.indicatorRange.split(",").filter { pipelineIdInfoMap.containsKey(it) }.size
            val ruleTemplateIds = if (rule.pipelineTemplateRange.isNullOrBlank()) listOf() else rule.pipelineTemplateRange.split(",")
            val templatePipelineCount = templatePipelineCountMap.filter { it.key in ruleTemplateIds }.values.sum()
            val rulePermission = getRulePermission(permissionMap, rule)

            // 最后生成结果
            QualityRuleSummaryWithPermission(
                    HashUtil.encodeLongId(rule.id),
                    rule.name,
                    ruleSummaryControlPoint,
                    summaryIndicatorList,
                    RuleRange.PART_BY_NAME,
                    templateSummary.plus(pipelineSummary),
                    pipelineCount + templatePipelineCount,
                    rule.executeCount,
                    rule.interceptTimes,
                    rule.enable,
                    rulePermission
            )
        } ?: listOf()
        return Pair(count, list)
    }

    private fun getRulePermission(permissionMap: Map<AuthPermission, List<Long>>, rule: TQualityRuleRecord): RulePermission {
        val canEditList = permissionMap[AuthPermission.EDIT]!!
        val canDeleteList = permissionMap[AuthPermission.DELETE]!!
        val canEnableList = permissionMap[AuthPermission.ENABLE]!!

        val canEdit = canEditList.contains(rule.id)
        val canDelete = canDeleteList.contains(rule.id)
        val canEnable = canEnableList.contains(rule.id)
        return RulePermission(canEdit, canDelete, canEnable)
    }

    // 生成Indicator汇总结果
    private fun getSummaryIndicatorList(indicators: List<QualityIndicator>, ruleIndicatorMap: Map<Long, Pair<String, String>>): List<QualityRuleSummaryWithPermission.RuleSummaryIndicator> {
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
        return pipelineIdInfoMap.filter { it.key in pipelineIds }.map {
            val pipelineId = it.key
            val info = it.value
            val pipelineElement = pipelineElementsMap[pipelineId] ?: listOf()
            val pipelineElementCodes = pipelineElement.map { it.classType }
            val lackElements = indicatorElement.minus(pipelineElementCodes).toMutableSet()
            if (controlPoint != null && !pipelineElementCodes.contains(controlPoint.type)) lackElements.add(controlPoint.type)
            QualityRuleSummaryWithPermission.RuleRangeSummary(info.pipelineId, info.pipelineName, "PIPELINE",
                    lackElements.map { ElementUtils.getElementCnName(it, projectId) })
        }
    }

    // 获取rule里面的模板，相对indicators和控制点，还缺哪些指标
    private fun getTemplateLackSummary(projectId: String, rule: TQualityRuleRecord, indicators: List<QualityIndicator>, controlPoint: QualityControlPoint?, templateIdMap: Map<String, OptionalTemplate>): List<QualityRuleSummaryWithPermission.RuleRangeSummary> {

        val templateIds = if (rule.pipelineTemplateRange.isNullOrBlank()) listOf()
                            else rule.pipelineTemplateRange.split(",")
        val indicatorElement = indicators.map { it.elementType }.toSet()
        return templateIdMap.filter { templateIds.contains(it.key) }.map {
            val template = it.value
            val templateElements = mutableListOf<Element>()
            template.stages.map { it.containers.map { templateElements.addAll(it.elements) } }
            val templateElementCodes = templateElements.map { it.getAtomCode() }.toSet()
            val lackElements = indicatorElement.minus(templateElementCodes).toMutableSet()
            if (controlPoint != null && !templateElementCodes.contains(controlPoint.type)) lackElements.add(controlPoint.type)
            QualityRuleSummaryWithPermission.RuleRangeSummary(template.templateId, template.name, "TEMPLATE",
                    lackElements.map { ElementUtils.getElementCnName(it, projectId) })
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

    private fun validatePermission(userId: String, projectId: String, authPermission: AuthPermission): Boolean {
        return bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, RESOURCE_TYPE, projectId, authPermission)
    }

    private fun validatePermission(userId: String, projectId: String, authPermission: AuthPermission, message: String) {
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, RESOURCE_TYPE, projectId, "*", authPermission)) {
            logger.error(message)
            throw PermissionForbiddenException(message)
        }
    }

    private fun validatePermission(userId: String, projectId: String, ruleId: Long, authPermission: AuthPermission, message: String) {
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, RESOURCE_TYPE, projectId, HashUtil.encodeLongId(ruleId), authPermission)) {
            logger.error(message)
            throw PermissionForbiddenException(message)
        }
    }

    private fun createResource(userId: String, projectId: String, ruleId: Long, ruleName: String) {
        bkAuthResourceApi.createResource(userId, serviceCode, RESOURCE_TYPE, projectId, HashUtil.encodeLongId(ruleId), ruleName)
    }

    private fun modifyResource(projectId: String, ruleId: Long, ruleName: String) {
        bkAuthResourceApi.modifyResource(serviceCode, RESOURCE_TYPE, projectId, HashUtil.encodeLongId(ruleId), ruleName)
    }

    private fun deleteResource(projectId: String, ruleId: Long) {
        bkAuthResourceApi.deleteResource(serviceCode, RESOURCE_TYPE, projectId, HashUtil.encodeLongId(ruleId))
    }

    private fun filterRules(userId: String, projectId: String, authPermissionSet: Set<AuthPermission>): Map<AuthPermission, List<Long>> {
        val permissionResourceMap = bkAuthPermissionApi.getUserResourcesByPermissions(userId, serviceCode, RESOURCE_TYPE, projectId, authPermissionSet, null)
        val permissionRuleMap = mutableMapOf<AuthPermission, List<Long>>()
        permissionResourceMap.forEach { permission, list ->
            permissionRuleMap[permission] = list.map { HashUtil.decodeIdToLong(it) }
        }
        return permissionRuleMap
    }

    fun count(projectId: String): Long {
        return qualityRuleDao.count(dslContext, projectId)
    }

    fun copyRule(request: CopyRuleRequest): List<String> {
        logger.info("copy rule request: $request")
        val ruleList = qualityRuleDao.list(dslContext, request.sourceProjectId) ?: return listOf()
        val filterRuleList = ruleList.filter { !it.pipelineTemplateRange.isNullOrBlank() && it.pipelineTemplateRange.split(",").contains(request.sourceTemplateId) }
        logger.info("start to copy rule list size: ${filterRuleList.size}")
        return filterRuleList.map {
            logger.info("start to copy rule : ${it.name}")
            val ruleData = doGetRuleData(it)

            val createRequest = RuleCreateRequest(ruleData.name,
                    ruleData.desc,
                    ruleData.indicators.map { RuleCreateRequest.CreateRequestIndicator(it.hashId, it.operation.name, it.threshold) }, // indicatorIds
                    ruleData.controlPoint.name,
                    ruleData.controlPoint.position.name,
                    listOf(),
                    listOf(request.targetTemplateId),
                    ruleData.operation,
                    listOf(),
                    listOf(),
                    listOf(),
                    listOf(),
                    ruleData.auditTimeoutMinutes,
                    ruleData.gatewayId
            )
            serviceCreate(request.userId, request.targetProjectId, createRequest)
        }
    }
}
