package com.tencent.devops.quality.service.v2

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.model.quality.tables.records.TQualityIndicatorRecord
import com.tencent.devops.quality.api.v2.pojo.QualityIndicator
import com.tencent.devops.quality.api.v2.pojo.enums.IndicatorType
import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType
import com.tencent.devops.quality.api.v2.pojo.enums.QualityOperation
import com.tencent.devops.quality.api.v2.pojo.op.IndicatorData
import com.tencent.devops.quality.api.v2.pojo.op.IndicatorUpdate
import com.tencent.devops.quality.api.v2.pojo.request.IndicatorCreate
import com.tencent.devops.quality.api.v2.pojo.response.IndicatorListResponse
import com.tencent.devops.quality.api.v2.pojo.response.IndicatorStageGroup
import com.tencent.devops.quality.dao.v2.QualityIndicatorDao
import com.tencent.devops.quality.dao.v2.QualityTemplateIndicatorMapDao
import com.tencent.devops.quality.util.ElementUtils
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Base64
import kotlin.Comparator

@Service
class QualityIndicatorService @Autowired constructor(
    private val dslContext: DSLContext,
    private val indicatorDao: QualityIndicatorDao,
    private val metadataService: QualityMetadataService,
    private val templateIndicatorMapDao: QualityTemplateIndicatorMapDao
) {

    private val encoder = Base64.getEncoder()

    fun listByLevel(projectId: String): List<IndicatorStageGroup> {
        val indicators = listIndicatorByProject(projectId).filter { it.enable }.map { indicator ->
            val metadataIds = convertMetaIds(indicator.metadataIds)
            val metadata = metadataService.serviceListMetadata(metadataIds).map { QualityIndicator.Metadata(it.hashId, it.dataName, it.dataId) }
            convertRecord(indicator, metadata)
        }.toList()

        // 生成数据
        return indicators.groupBy { it.stage }.map { stage ->
            val stageGroup = stage.value.groupBy { it.elementType }.map { controlPoint ->

                // 遍历控制点，elementDetail做分隔
                var detailIndicatorMap = mutableMapOf<String /*detail*/, MutableList<QualityIndicator>>()
                controlPoint.value.forEach { indicator ->
                    indicator.elementDetail.split(",").forEach {
                        val list = detailIndicatorMap[it]
                        if (list == null) {
                            detailIndicatorMap[it] = mutableListOf(indicator)
                        } else {
                            detailIndicatorMap[it]!!.add(indicator)
                        }
                    }
                }

                // 控制点的原子类型
                val elementType = controlPoint.key

                // 根据codeccToolNameMap的key顺序排序
                if (isCodeccControlPoint(elementType)) {
                    val propertyMap = codeccToolNameMap.entries.mapIndexed { index, entry ->
                        entry.key to index
                    }.toMap()
                    detailIndicatorMap = detailIndicatorMap.toSortedMap(Comparator { o1, o2 ->
                        (propertyMap[o1] ?: Int.MAX_VALUE) - (propertyMap[o2] ?: Int.MAX_VALUE)
                    })
                }

                // 按elementDetail做分组
                val detailGroups = detailIndicatorMap.map { detailEntry ->
                    val elementDetail = detailEntry.key
                    var detailCnName = elementDetail
                    val indicatorList: List<QualityIndicator> = detailEntry.value

                    // codecc的指标要排序和中文特殊处理
                    if (isCodeccControlPoint(elementType)) {
                        detailCnName = codeccToolNameMap[elementDetail] ?: elementDetail
                    }

                    // 生成结果
                    val detailHashId = encoder.encodeToString(elementDetail.toByteArray())
                    IndicatorStageGroup.IndicatorDetailGroup(detailHashId, detailCnName, codeccToolDescMap[elementDetail]
                            ?: "", indicatorList)
                }
                IndicatorStageGroup.IndicatorControlPointGroup(encoder.encodeToString(controlPoint.key.toByteArray()),
                        elementType, ElementUtils.getElementCnName(elementType, projectId), detailGroups)
            }
            IndicatorStageGroup(encoder.encodeToString(stage.key.toByteArray()), stage.key, stageGroup)
        }
    }

    private fun isCodeccControlPoint(elementType: String): Boolean {
        return elementType == LinuxCodeCCScriptElement.classType || elementType == LinuxPaasCodeCCScriptElement.classType
    }

    fun serviceList(indicatorIds: Collection<Long>): List<QualityIndicator> {
        return indicatorDao.listByIds(dslContext, indicatorIds)?.map { indicator ->
            val metadataIds = convertMetaIds(indicator.metadataIds)
            val metadata = metadataService.serviceListMetadata(metadataIds).map { QualityIndicator.Metadata(it.hashId, it.dataName, it.dataId) }
            convertRecord(indicator, metadata)
        }?.toList() ?: listOf()
    }

    fun opList(userId: String, page: Int?, pageSize: Int?): Page<IndicatorData> {
        val dataRecords = indicatorDao.listSystem(dslContext, page, pageSize)
        val data = indicatorRecordToIndicatorData(dataRecords)
        val count = indicatorDao.countSystem(dslContext)
        return Page(page ?: 1, pageSize ?: count.toInt(), count, data)
    }

    fun opListByIds(userId: String, ids: String): List<IndicatorData> {
        val idList: Set<Long> = ids.split(",").map { it.toLongOrNull() }.filter { it != null }.toSet() as Set<Long>
        if (idList.isEmpty()) return emptyList()

        val dataRecord = indicatorDao.listByIds(dslContext, idList)
        return indicatorRecordToIndicatorData(dataRecord)
    }

    private fun indicatorRecordToIndicatorData(indicatorRecords: Result<TQualityIndicatorRecord>?): List<IndicatorData> {
        return indicatorRecords?.map {
            val metadataIds = convertMetaIds(it.metadataIds).toSet()
            val metadataList = metadataService.serviceListByIds(metadataIds)

            val sb = StringBuilder()
            var notfirst = false
            metadataList.forEach { it2 ->
                if (notfirst) sb.append(" + ")
                sb.append(it2.dataName)
                notfirst = true
            }
            val metadataNames = sb.toString()

            IndicatorData(
                    it.id, it.elementType, it.elementName, it.elementDetail, it.enName,
                    it.cnName, it.metadataIds, metadataNames, it.defaultOperation,
                    it.operationAvailable, it.threshold, it.thresholdType,
                    it.desc, it.indicatorReadOnly, it.stage, it.indicatorRange, it.type, it.tag, it.enable
            )
        } ?: listOf()
    }

    // 创建时，指标中文名与英文名不能重复
    fun opCreate(userId: String, indicatorUpdate: IndicatorUpdate): Msg {
        checkSystemIndicatorExist(indicatorUpdate.enName ?: "", indicatorUpdate.cnName ?: "")
        if (indicatorDao.create(userId, indicatorUpdate, dslContext) > 0) {
            return Msg(0, "创建成功", true)
        }
        return Msg(-1, "未知的异常，创建失败", false)
    }

    fun userDelete(userId: String, id: Long): Boolean {
        delete(userId, id)
        return true
    }

    fun opDelete(userId: String, id: Long): Boolean {
        delete(userId, id)
        return true
    }

    private fun delete(userId: String, id: Long): Boolean {
        logger.info("user($userId) delete indicator($id)")
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            templateIndicatorMapDao.deleteByIndicatorId(transactionContext, id)
            indicatorDao.delete(id, transactionContext)
        }
        return true
    }

    fun opUpdate(userId: String, id: Long, indicatorUpdate: IndicatorUpdate): Msg {
        checkSystemIndicatorExcludeExist(id, indicatorUpdate.enName ?: "", indicatorUpdate.cnName ?: "")
        logger.info("user($userId) update the indicator($id): $indicatorUpdate")
        if (indicatorDao.update(userId, id, indicatorUpdate, dslContext) > 0) {
            return Msg(0, "更新指标数据成功", true)
        }
        return Msg(-1, "未知的异常，更新失败", false)
    }

    fun userCreate(userId: String, projectId: String, indicatorCreate: IndicatorCreate): Boolean {
        checkCustomIndicatorExist(projectId, indicatorCreate.name, indicatorCreate.cnName)
        val indicatorUpdate = IndicatorUpdate(
                indicatorCreate.elementType,
                ElementUtils.getElementCnName(indicatorCreate.elementType, projectId),
                ElementUtils.getElementCnName(indicatorCreate.elementType, projectId),
                "",
                indicatorCreate.name,
                indicatorCreate.cnName,
                "",
                indicatorCreate.operation.firstOrNull()?.name,
                indicatorCreate.operation.joinToString(","),
                indicatorCreate.threshold,
                indicatorCreate.dataType.name,
                indicatorCreate.desc,
                false,
                "开发",
                projectId,
                null,
                true,
                IndicatorType.CUSTOM
        )
        indicatorDao.create(userId, indicatorUpdate, dslContext)
        return true
    }

    fun userUpdate(userId: String, projectId: String, indicatorId: String, indicatorCreate: IndicatorCreate): Boolean {
        val id = HashUtil.decodeIdToLong(indicatorId)
        checkCustomIndicatorExcludeExist(id, projectId, indicatorCreate.name, indicatorCreate.cnName)
        val indicatorUpdate = IndicatorUpdate(
                indicatorCreate.elementType,
                ElementUtils.getElementCnName(indicatorCreate.elementType, projectId),
                ElementUtils.getElementCnName(indicatorCreate.elementType, projectId),
                "",
                indicatorCreate.name,
                indicatorCreate.cnName,
                "",
                indicatorCreate.operation.firstOrNull()?.name,
                indicatorCreate.operation.joinToString(","),
                indicatorCreate.threshold,
                indicatorCreate.dataType.name,
                indicatorCreate.desc,
                false,
                "开发",
                null,
                "",
                true,
                IndicatorType.CUSTOM
        )
        logger.info("user($userId) update the indicator($id): $indicatorUpdate")
        indicatorDao.update(userId, id, indicatorUpdate, dslContext)
        return true
    }

    fun userQueryIndicatorList(projectId: String, keyword: String?): IndicatorListResponse {
        val scriptIndicators = mutableListOf<IndicatorListResponse.IndicatorListItem>()
        val systemIndicators = mutableListOf<IndicatorListResponse.IndicatorListItem>()
        val marketIndicators = mutableListOf<IndicatorListResponse.IndicatorListItem>()

        listIndicatorByProject(projectId).filter { it.enable }.groupBy { it.elementType }.forEach { elementType, indicators ->
            indicators.map { indicator ->
                val metadataIds = convertMetaIds(indicator.metadataIds)
                val metadata = metadataService.serviceListMetadata(metadataIds).map {
                    IndicatorListResponse.QualityMetadata(it.dataId, it.dataName, it.elementDetail, it.valueType, it.desc, it.extra)
                }

                val item = IndicatorListResponse.IndicatorListItem(
                        HashUtil.encodeLongId(indicator.id),
                        indicator.enName,
                        indicator.cnName,
                        indicator.elementType,
                        indicator.elementName,
                        indicator.elementDetail,
                        metadata,
                        indicator.operationAvailable.split(",").map { QualityOperation.valueOf(it) },
                        QualityDataType.valueOf(indicator.thresholdType.toUpperCase()),
                        indicator.threshold,
                        indicator.desc
                )

                when (indicator.type) {
                    IndicatorType.SYSTEM.name -> {
                        systemIndicators.add(item)
                    }
                    IndicatorType.CUSTOM.name -> {
                        scriptIndicators.add(item)
                    }
                    IndicatorType.MARKET.name -> {
                        marketIndicators.add(item)
                    }
                    else -> {
                    }
                }
            }
        }
        return IndicatorListResponse(scriptIndicators, systemIndicators, marketIndicators)
    }

    fun serviceGet(projectId: String, indicatorId: String): QualityIndicator {
        return userGet(projectId, indicatorId)
    }

    fun userGet(projectId: String, indicatorId: String): QualityIndicator {
        val record = indicatorDao.get(dslContext, HashUtil.decodeIdToLong(indicatorId))
        val metadataIds = convertMetaIds(record.metadataIds)
        val metadata = metadataService.serviceListMetadata(metadataIds).map {
            QualityIndicator.Metadata(it.hashId, it.dataName, it.dataId)
        }
        return convertRecord(record, metadata)
    }

    fun setTestIndicator(userId: String, elementType: String, indicatorUpdateList: Collection<IndicatorUpdate>): Int {
        val testIndicatorList = indicatorDao.listByElementType(dslContext, elementType, IndicatorType.MARKET)
                ?.filter { it.tag == "IN_READY_TEST" } ?: listOf()
        val testIndicatorMap = testIndicatorList.map { it.enName to it }.toMap()
        val lastIndicatorName = testIndicatorList.map { it.enName }
        val newIndicatorName = indicatorUpdateList.map { it.enName }

        // 删除这次没有的指标
        indicatorDao.delete(lastIndicatorName.minus(newIndicatorName).map { testIndicatorMap[it]!!.id }, dslContext)

        // 有则更新，没则插入
        indicatorUpdateList.forEach {
            val testIndicator = testIndicatorMap[it.enName]
            if (testIndicator != null) {
                indicatorDao.update(userId, testIndicator.id, it, dslContext)
            } else {
                indicatorDao.create(userId, it, dslContext)
            }
        }
        return indicatorUpdateList.size
    }

    // 把测试的数据刷到正式的， 有则update，没则insert，多余的删掉
    fun serviceRefreshIndicator(elementType: String, metadataMap: Map<String /* dataId */, String /* id */>): Int {
        val data = indicatorDao.listByElementType(dslContext, elementType, IndicatorType.MARKET)
        val testData = data?.filter { it.tag == "IN_READY_TEST" } ?: listOf()
        val prodData = data?.filter { it.tag != "IN_READY_TEST" } ?: listOf()
        val userId = testData.firstOrNull()?.createUser ?: ""

        // 有则update
        val deleteItemId = mutableSetOf<Long>()
        prodData.forEach PROD@{ prodItem ->
            testData.forEach TEST@{ testItem ->
                if (prodItem.enName == testItem.enName) {
                    indicatorDao.update(userId, prodItem.id, IndicatorUpdate(
                            testItem.elementType,
                            testItem.elementName,
                            testItem.elementDetail,
                            null, // 刷新不需要更新插件版本
                            testItem.enName,
                            testItem.cnName,
                            metadataMap[testItem.enName], // 插件市场注册的指标enName跟基础数据的dataId是一样的
                            testItem.defaultOperation,
                            testItem.operationAvailable,
                            testItem.threshold,
                            testItem.thresholdType,
                            testItem.desc,
                            testItem.indicatorReadOnly,
                            testItem.stage,
                            prodItem.indicatorRange, // 用线上的可见范围
                            "IN_READY_RUNNING",
                            testItem.enable,
                            IndicatorType.MARKET,
                            testItem.logPrompt
                    ), dslContext)
                    return@PROD
                }
            }

            // test没有的，多余的删掉
            deleteItemId.add(prodItem.id)
        }

        indicatorDao.delete(deleteItemId, dslContext)

        // 没则insert
        testData.forEach TEST@{ testItem ->
            prodData.forEach PROD@{ prodItem ->
                if (prodItem.enName == testItem.enName) return@TEST
            }
            indicatorDao.create(userId, IndicatorUpdate(
                    testItem.elementType,
                    testItem.elementName,
                    testItem.elementDetail,
                    testItem.atomVersion,
                    testItem.enName,
                    testItem.cnName,
                    metadataMap[testItem.enName], // 插件市场注册的指标enName跟基础数据的dataId是一样的
                    testItem.defaultOperation,
                    testItem.operationAvailable,
                    testItem.threshold,
                    testItem.thresholdType,
                    testItem.desc,
                    testItem.indicatorReadOnly,
                    testItem.stage,
                    testItem.indicatorRange,
                    "IN_READY_RUNNING",
                    testItem.enable,
                    IndicatorType.valueOf(testItem.type),
                    testItem.logPrompt
            ), dslContext)
        }

        return testData.size
    }

    fun serviceDeleteTestIndicator(elementType: String): Int {
        val data = indicatorDao.listByElementType(dslContext, elementType)
        val testData = data?.filter { it.tag == "IN_READY_TEST" } ?: listOf()
        return indicatorDao.delete(testData.map { it.id }, dslContext)
    }

    fun appendRangeByElement(elementType: String, projectIds: Collection<String>): Int {
        logger.info("append element range($elementType): ${projectIds.joinToString(",")}")
        val records = indicatorDao.listByElementType(dslContext, elementType)
                ?.filter { it.indicatorRange != "ANY" && it.tag == "IN_READY_RUNNING" } ?: listOf()
        records.forEach {
            logger.info("append element range($elementType): ${projectIds.joinToString(",")}")
            indicatorDao.appendRange(it.id, projectIds.joinToString(","), dslContext)
        }

        return records.size
    }

    fun listIndicatorByProject(projectId: String): List<TQualityIndicatorRecord> {
        val indicators = indicatorDao.listByProject(dslContext, projectId) ?: return listOf()

        // 测试项目的话，就只列出插件测试相关的指标
        val testIndicators = indicators.filter { isTestIndicator(it) }
        val testIndicatorElementTypes = testIndicators.map { it.elementType }.toSet()
        val filterIndicators = indicators.filter { !isTestIndicator(it) && !testIndicatorElementTypes.contains(it.elementType) }

        return filterIndicators.plus(testIndicators)
    }

    private fun isTestIndicator(qualityIndicator: TQualityIndicatorRecord): Boolean {
        return qualityIndicator.type == IndicatorType.MARKET.name && qualityIndicator.tag == "IN_READY_TEST"
    }

    private fun convertRecord(indicator: TQualityIndicatorRecord, metadata: List<QualityIndicator.Metadata> = listOf()): QualityIndicator {
        return QualityIndicator(
                HashUtil.encodeLongId(indicator.id),
                indicator.elementType,
                indicator.elementDetail ?: "",
                indicator.enName,
                indicator.cnName,
                indicator.stage ?: "",
                QualityOperation.valueOf(indicator.defaultOperation),
                indicator.operationAvailable.split(",").map { QualityOperation.valueOf(it) },
                indicator.threshold,
                QualityDataType.valueOf(indicator.thresholdType),
                if (indicator.tag == "TENCENTOPEN") true else indicator.indicatorReadOnly,
                indicator.type,
                indicator.tag,
                metadata,
                indicator.desc,
                indicator.logPrompt
        )
    }

    private fun convertMetaIds(metadataIds: String?): List<Long> {
        if (metadataIds.isNullOrBlank()) return emptyList()
        return metadataIds!!.split(",").map { id -> id.toLong() }
    }

    private fun checkSystemIndicatorExist(enName: String, cnName: String): Boolean {
        val indicators = indicatorDao.listByType(dslContext, null, IndicatorType.SYSTEM) ?: return false
        if (indicators.any { it.enName == enName }) throw OperationException("英文名($enName)的指标已存在")
        if (indicators.any { it.cnName == cnName }) throw OperationException("中文名($cnName)的指标已存在")
        return false
    }

    private fun checkSystemIndicatorExcludeExist(id: Long, enName: String, cnName: String): Boolean {
        val indicators = indicatorDao.listByType(dslContext, null, IndicatorType.SYSTEM) ?: return false
        val filterList = indicators.filter { it.id != id }
        if (filterList.any { it.enName == enName }) throw OperationException("英文名($enName)的指标已存在")
        if (filterList.any { it.cnName == cnName }) throw OperationException("中文名($cnName)的指标已存在")
        return false
    }

    private fun checkCustomIndicatorExist(projectId: String, enName: String, cnName: String): Boolean {
        val indicators = indicatorDao.listByType(dslContext, projectId, IndicatorType.CUSTOM) ?: return false
        if (indicators.any { it.enName == enName }) throw OperationException("英文名($enName)的指标已存在")
        if (indicators.any { it.cnName == cnName }) throw OperationException("中文名($cnName)的指标已存在")
        return false
    }

    private fun checkCustomIndicatorExcludeExist(id: Long, projectId: String, enName: String, cnName: String): Boolean {
        val indicators = indicatorDao.listByType(dslContext, projectId, IndicatorType.CUSTOM) ?: return false
        val filterList = indicators.filter { it.id != id }
        if (filterList.any { it.enName == enName }) throw OperationException("英文名($enName)的指标已存在")
        if (filterList.any { it.cnName == cnName }) throw OperationException("中文名($cnName)的指标已存在")
        return false
    }

    fun userCount(projectId: String): Long {
        return indicatorDao.count(dslContext, projectId, true)
    }

    data class Msg(val code: Int, val msg: String, val flag: Boolean)

    companion object {
        private val logger = LoggerFactory.getLogger(QualityIndicatorService::class.java)

        val codeccToolNameMap = mapOf(
                "COVERITY" to "Coverity",
                "KLOCWORK" to "Klocwork",
                "CPPLINT" to "CppLint",
                "ESLINT" to "ESLint",
                "PYLINT" to "PyLint",
                "GOML" to "Gometalinter",
                "CHECKSTYLE" to "Checkstyle",
                "STYLECOP" to "StyleCop",
                "DETEKT" to "detekt",
                "PHPCS" to "PHPCS",
                "SENSITIVE" to "敏感信息",
                "CCN" to "圈复杂度",
                "DUPC" to "重复率")

        private val codeccToolDescMap = mapOf(
                "COVERITY" to "斯坦福大学科学家研究成果，静态源代码分析领域的领导者",
                "KLOCWORK" to "业界广泛使用的商用代码检查工具，与Coverity互补",
                "CPPLINT" to "谷歌开源的C++代码风格检查工具",
                "ESLINT" to "JavaScript代码检查工具",
                "PYLINT" to "Python代码风格检查工具",
                "GOML" to "Golang静态代码分析工具",
                "CHECKSTYLE" to "Java代码风格检查工具",
                "STYLECOP" to "微软开源的C#静态代码分析工具",
                "DETEKT" to "Kotlin静态代码分析工具 ",
                "PHPCS" to "PHP代码风格检查工具",
                "SENSITIVE" to "可扫描代码中有安全风险的敏感信息",
                "CCN" to "通过计算函数的节点个数来衡量代码复杂性",
                "DUPC" to "可以检测项目中复制粘贴和重复开发相同功能等问题",
                "OCCHECK" to "OC代码风格检查工具")
    }
}