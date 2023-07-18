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

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.Maps
import com.tencent.devops.common.api.constant.DEVELOP
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.quality.pojo.enums.QualityOperation
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.quality.tables.records.TQualityIndicatorRecord
import com.tencent.devops.plugin.codecc.CodeccUtils
import com.tencent.devops.quality.api.v2.pojo.QualityIndicator
import com.tencent.devops.quality.api.v2.pojo.enums.IndicatorType
import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType
import com.tencent.devops.quality.api.v2.pojo.op.IndicatorData
import com.tencent.devops.quality.api.v2.pojo.op.IndicatorUpdate
import com.tencent.devops.quality.api.v2.pojo.request.IndicatorCreate
import com.tencent.devops.quality.api.v2.pojo.response.IndicatorListResponse
import com.tencent.devops.quality.api.v2.pojo.response.IndicatorStageGroup
import com.tencent.devops.quality.constant.BK_CREATE_FAIL
import com.tencent.devops.quality.constant.BK_CREATE_SUCCESS
import com.tencent.devops.quality.constant.BK_METRIC_DATA_UPDATE_SUCCESS
import com.tencent.devops.quality.constant.BK_TOOL_DESC_CCN
import com.tencent.devops.quality.constant.BK_TOOL_DESC_DEFECT
import com.tencent.devops.quality.constant.BK_TOOL_DESC_DUPC
import com.tencent.devops.quality.constant.BK_TOOL_DESC_RIPS
import com.tencent.devops.quality.constant.BK_TOOL_DESC_SECURITY
import com.tencent.devops.quality.constant.BK_TOOL_DESC_SENSITIVE
import com.tencent.devops.quality.constant.BK_TOOL_DESC_STANDARD
import com.tencent.devops.quality.constant.BK_TOOL_DESC_WOODPECKER_SENSITIVE
import com.tencent.devops.quality.constant.BK_TOOL_NAME_BKCHECK_CPP
import com.tencent.devops.quality.constant.BK_TOOL_NAME_BKCHECK_OC
import com.tencent.devops.quality.constant.BK_TOOL_NAME_CCN
import com.tencent.devops.quality.constant.BK_TOOL_NAME_CHECKSTYLE
import com.tencent.devops.quality.constant.BK_TOOL_NAME_COVERITY
import com.tencent.devops.quality.constant.BK_TOOL_NAME_CPPLINT
import com.tencent.devops.quality.constant.BK_TOOL_NAME_DEFECT
import com.tencent.devops.quality.constant.BK_TOOL_NAME_DETEKT
import com.tencent.devops.quality.constant.BK_TOOL_NAME_DUPC
import com.tencent.devops.quality.constant.BK_TOOL_NAME_ESLINT
import com.tencent.devops.quality.constant.BK_TOOL_NAME_GOML
import com.tencent.devops.quality.constant.BK_TOOL_NAME_KLOCWORK
import com.tencent.devops.quality.constant.BK_TOOL_NAME_OCCHECK
import com.tencent.devops.quality.constant.BK_TOOL_NAME_PHPCS
import com.tencent.devops.quality.constant.BK_TOOL_NAME_PYLINT
import com.tencent.devops.quality.constant.BK_TOOL_NAME_SECURITY
import com.tencent.devops.quality.constant.BK_TOOL_NAME_SENSITIVE
import com.tencent.devops.quality.constant.BK_TOOL_NAME_STANDARD
import com.tencent.devops.quality.constant.BK_TOOL_NAME_STYLECOP
import com.tencent.devops.quality.constant.BK_TOOL_NAME_WOODPECKER_SENSITIVE
import com.tencent.devops.quality.constant.BK_UPDATE_FAIL
import com.tencent.devops.quality.constant.QUALITY_INDICATOR_DESC_KEY
import com.tencent.devops.quality.constant.QUALITY_INDICATOR_ELEMENT_NAME_KEY
import com.tencent.devops.quality.constant.QUALITY_INDICATOR_NAME_KEY
import com.tencent.devops.quality.constant.QualityMessageCode.QUALITY_INDICATOR_CHINESE_NAME_EXISTS
import com.tencent.devops.quality.constant.QualityMessageCode.QUALITY_INDICATOR_ENGLISH_NAME_EXISTS
import com.tencent.devops.quality.dao.v2.QualityIndicatorDao
import com.tencent.devops.quality.dao.v2.QualityTemplateIndicatorMapDao
import com.tencent.devops.quality.pojo.enum.RunElementType
import com.tencent.devops.quality.pojo.po.QualityIndicatorPO
import com.tencent.devops.quality.util.ElementUtils
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import java.io.File
import java.util.Base64
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class QualityIndicatorService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val indicatorDao: QualityIndicatorDao,
    private val metadataService: QualityMetadataService,
    private val templateIndicatorMapDao: QualityTemplateIndicatorMapDao,
    private val redisOperation: RedisOperation,
    val commonConfig: CommonConfig
) {

    private val encoder = Base64.getEncoder()

    @PostConstruct
    fun init() {
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "QUALITY_INDICATOR_INIT_LOCK",
            expiredTimeInSeconds = 60

        )
        Executors.newFixedThreadPool(1).submit {
            if (redisLock.tryLock()) {
                try {
                    logger.info("start init quality indicator")
                    val classPathResource = ClassPathResource(
                        "i18n${File.separator}indicator_${commonConfig.devopsDefaultLocaleLanguage}.json"
                    )
                    val inputStream = classPathResource.inputStream
                    val json = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val qualityIndicatorPOs = JsonUtil.to(json, object : TypeReference<List<QualityIndicatorPO>>() {})
                    indicatorDao.batchCrateQualityIndicator(dslContext, qualityIndicatorPOs)
                    logger.info("init quality indicator end")
                } catch (ignored: Throwable) {
                    logger.warn("init quality indicator fail! error:${ignored.message}")
                } finally {
                    redisLock.unlock()
                }
            }
        }
    }

    fun listByLevel(projectId: String): List<IndicatorStageGroup> {
        val codeccToolNameMap = getCodeccToolNameMap()
        val codeccToolDescMap = getCodeccToolDescMap()
        val indicatorRecords = listIndicatorByProject(projectId)
        val indicators = serviceListIndicatorRecord(indicatorRecords)

        // 生成数据
        return indicators.groupBy { it.stage }.map { stage ->
            val stageGroup = stage.value.groupBy { it.elementType }.map { controlPoint ->

                // 遍历控制点，elementDetail做分隔
                val detailIndicatorMap = mutableMapOf<String /*detail*/, MutableList<QualityIndicator>>()
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
                val detailIndicatorSortedMap = Maps.newLinkedHashMap<String /*detail*/, MutableList<QualityIndicator>>()
                if (CodeccUtils.isCodeccAtom(elementType) || CodeccUtils.isCodeccCommunityAtom(elementType)) {
                    val propertyMap = codeccToolNameMap.entries.mapIndexed { index, entry ->
                        entry.key to index
                    }.toMap()

                    // toSortedMap 在key值相等会互相覆盖，所以要分开处理
                    val originMap = detailIndicatorMap.filter { propertyMap.containsKey(it.key) }
                        .toSortedMap(Comparator { o1, o2 ->
                            propertyMap[o1]!! - propertyMap[o2]!!
                        })
                    val dynamicMap = detailIndicatorMap.filter { !propertyMap.containsKey(it.key) }
                    detailIndicatorSortedMap.putAll(originMap)
                    detailIndicatorSortedMap.putAll(dynamicMap)
                } else {
                    detailIndicatorSortedMap.putAll(detailIndicatorMap)
                }

                // 按elementDetail做分组
                val detailGroups = detailIndicatorSortedMap.map { detailEntry ->
                    val elementDetail = detailEntry.key
                    var detailCnName = elementDetail
                    val indicatorList: List<QualityIndicator> = detailEntry.value

                    // codecc的指标要排序和中文特殊处理
                    if (CodeccUtils.isCodeccAtom(elementType) || CodeccUtils.isCodeccCommunityAtom(elementType)) {
                        detailCnName = codeccToolNameMap[elementDetail] ?: elementDetail
                    }

                    // 生成结果
                    val detailHashId = encoder.encodeToString(elementDetail.toByteArray())
                    IndicatorStageGroup.IndicatorDetailGroup(detailHashId,
                        detailCnName,
                        codeccToolDescMap[elementDetail] ?: "",
                        indicatorList
                    )
                }
                IndicatorStageGroup.IndicatorControlPointGroup(
                    hashId = encoder.encodeToString(controlPoint.key.toByteArray()),
                    controlPoint = elementType,
                    controlPointName = ElementUtils.getElementCnName(elementType, projectId),
                    details = detailGroups
                )
            }
            IndicatorStageGroup(
                hashId = encoder.encodeToString(stage.key.toByteArray()),
                stage = stage.key,
                controlPoints = stageGroup
            )
        }
    }

    fun serviceList(indicatorIds: Collection<Long>): List<QualityIndicator> {
        val indicatorRecords = indicatorDao.listByIds(dslContext, indicatorIds)
        return serviceListIndicatorRecord(indicatorRecords)
    }

    fun serviceListALL(indicatorIds: Collection<Long>): List<QualityIndicator> {
        val indicatorTMap = indicatorDao.listByIds(dslContext, indicatorIds)?.map { it.id to it }?.toMap()
        return indicatorIds.map { id ->
            val indicator = indicatorTMap?.get(id) ?: throw OperationException("indicator id $id is not exist")
            val metadataIds = convertMetaIds(indicator.metadataIds)
            val metadata = metadataService.serviceListMetadata(metadataIds).map {
                QualityIndicator.Metadata(it.hashId, it.dataName, it.dataId)
            }
            convertRecord(indicator, metadata)
        }
    }

    fun serviceList(
        elementType: String,
        enNameSet: Collection<String>,
        projectId: String? = null
    ): List<QualityIndicator> {
        val tempProjectId = if (elementType == RunElementType.RUN.elementType) projectId else null
        val indicatorRecords = indicatorDao.listByElementType(
            dslContext = dslContext,
            elementType = elementType,
            type = null,
            enNameSet = enNameSet,
            projectId = tempProjectId
        )
        val prodIndicator = if (indicatorRecords?.associateBy { it.tag }?.containsKey("IN_READY_RUNNING") == true) {
            indicatorRecords?.filter { it.tag == "IN_READY_RUNNING" }.associateBy { it.enName }
        } else {
            indicatorRecords?.associateBy { it.enName }
        }
        val allIndicatorRecords = enNameSet.map {
            prodIndicator?.get(it) ?: throw OperationException("indicator id $it is not exist")
        }
        return serviceListIndicatorRecord(allIndicatorRecords)
    }

    fun serviceListByElementType(elementType: String, enNameSet: Collection<String>): List<QualityIndicator> {
        val indicatorRecords = indicatorDao.listByElementType(
            dslContext = dslContext,
            elementType = elementType,
            type = null,
            enNameSet = enNameSet
        )
        return serviceListIndicatorRecord(indicatorRecords)
    }

    fun serviceListFilterBash(elementType: String, enNameSet: Collection<String>): List<QualityIndicator> {
        return if (elementType in QualityIndicator.SCRIPT_ELEMENT) {
            listOf()
        } else {
            serviceListByElementType(elementType, enNameSet).filter { it.enable ?: false }
        }
    }

    fun opList(userId: String, page: Int?, pageSize: Int?): Page<IndicatorData> {
        val dataRecords = indicatorDao.listSystemDescByPage(dslContext, page, pageSize)
        val data = indicatorRecordToIndicatorData(dataRecords)
        val count = indicatorDao.countSystem(dslContext)
        return Page(page ?: 1, pageSize ?: count.toInt(), count, data)
    }

    fun opListByIds(userId: String, ids: String): List<IndicatorData> {
        val idList: Set<Long> = ids.split(",").mapNotNull { it.toLongOrNull() }.toSet()
        if (idList.isEmpty()) return emptyList()

        val dataRecord = indicatorDao.listByIds(dslContext, idList)
        return indicatorRecordToIndicatorData(dataRecord)
    }

    private fun indicatorRecordToIndicatorData(
        indicatorRecords: Result<TQualityIndicatorRecord>?
    ): List<IndicatorData> {
        // todo perform
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
                id = it.id,
                elementType = it.elementType,
                elementName = it.elementName,
                elementDetail = it.elementDetail,
                enName = it.enName,
                cnName = it.cnName,
                metadataIds = it.metadataIds,
                metadataNames = metadataNames,
                defaultOperation = it.defaultOperation,
                operationAvailable = it.operationAvailable,
                threshold = it.threshold,
                thresholdType = it.thresholdType,
                desc = it.desc,
                readOnly = it.indicatorReadOnly,
                stage = I18nUtil.getCodeLanMessage(messageCode = it.stage, defaultMessage = it.stage),
                range = it.indicatorRange,
                type = it.type,
                tag = it.tag,
                enable = it.enable,
                logPrompt = it.logPrompt ?: ""
            )
        } ?: listOf()
    }

    // 创建时，指标中文名与英文名不能重复
    fun opCreate(userId: String, indicatorUpdate: IndicatorUpdate): Msg {
        checkSystemIndicatorExist(indicatorUpdate.enName ?: "", indicatorUpdate.cnName ?: "")
        if (indicatorDao.create(userId, indicatorUpdate, dslContext) > 0) {
            return Msg(0, I18nUtil.getCodeLanMessage(BK_CREATE_SUCCESS), true)
        }
        return Msg(
            -1,
            I18nUtil.getCodeLanMessage(messageCode = BK_CREATE_FAIL, language = I18nUtil.getLanguage(userId)),
            false
        )
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
            return Msg(0, I18nUtil.getCodeLanMessage(BK_METRIC_DATA_UPDATE_SUCCESS), true)
        }
        return Msg(
            code = -1,
            msg = I18nUtil.getCodeLanMessage(messageCode = BK_UPDATE_FAIL),
            flag = false
        )
    }

    fun userCreate(userId: String, projectId: String, indicatorCreate: IndicatorCreate): Boolean {
        checkCustomIndicatorExist(projectId, indicatorCreate.name, indicatorCreate.cnName)
        val indicatorUpdate = getIndicatorUpdate(projectId, indicatorCreate)
        indicatorDao.create(userId, indicatorUpdate, dslContext)
        return true
    }

    fun userUpdate(userId: String, projectId: String, indicatorId: String, indicatorCreate: IndicatorCreate): Boolean {
        val id = HashUtil.decodeIdToLong(indicatorId)
        checkCustomIndicatorExcludeExist(id, projectId, indicatorCreate.name, indicatorCreate.cnName)
        val indicatorUpdate = getIndicatorUpdate(projectId, indicatorCreate)
        logger.info("user($userId) update the indicator($id): $indicatorUpdate")
        indicatorDao.update(userId = userId, id = id, indicatorUpdate = indicatorUpdate, dslContext = dslContext)
        return true
    }

    fun upsertIndicators(userId: String, projectId: String, indicatorCreateList: List<IndicatorCreate>): Boolean {
        indicatorCreateList.forEach { indicatorCreate ->
            val indicatorId = checkCustomUpsertIndicator(projectId, indicatorCreate.name)
            val indicatorUpdate = getIndicatorUpdate(projectId, indicatorCreate)
            if (indicatorId == null) {
                indicatorDao.create(
                    userId = userId,
                    indicatorUpdate = indicatorUpdate,
                    dslContext = dslContext
                )
            } else {
                indicatorDao.update(
                    userId = userId,
                    id = indicatorId,
                    indicatorUpdate = indicatorUpdate,
                    dslContext = dslContext
                )
            }
        }
        return true
    }

    fun userQueryIndicatorList(projectId: String, keyword: String?): IndicatorListResponse {
        val scriptIndicators = mutableListOf<IndicatorListResponse.IndicatorListItem>()
        val systemIndicators = mutableListOf<IndicatorListResponse.IndicatorListItem>()
        val marketIndicators = mutableListOf<IndicatorListResponse.IndicatorListItem>()
        val codeccToolNameMap = getCodeccToolNameMap()
        listIndicatorByProject(projectId).filter {
            if (keyword.isNullOrBlank()) true
            else it.cnName.contains(keyword!!)
        }.groupBy { it.elementType }.forEach { (_, indicators) ->
            indicators.map { indicator ->
                val metadataIds = convertMetaIds(indicator.metadataIds)
                // todo performance
                val metadata = metadataService.serviceListMetadata(metadataIds).map {
                    IndicatorListResponse.QualityMetadata(enName = it.dataId,
                        cnName = it.dataName,
                        detail = it.elementDetail,
                        type = it.valueType,
                        msg = it.desc,
                        extra = it.extra)
                }

                var indicatorCnName = ""
                if (CodeccUtils.isCodeccAtom(indicator.elementType) ||
                    CodeccUtils.isCodeccCommunityAtom(indicator.elementType)) {
                    indicatorCnName = codeccToolNameMap[indicator.elementDetail] ?: ""
                }

                val item = IndicatorListResponse.IndicatorListItem(
                    hashId = HashUtil.encodeLongId(indicator.id),
                    name = indicator.enName,
                    cnName = I18nUtil.getCodeLanMessage(
                        messageCode = QUALITY_INDICATOR_NAME_KEY.format(indicator.id),
                        defaultMessage = indicator.cnName
                    ),
                    elementType = indicator.elementType,
                    elementName = I18nUtil.getCodeLanMessage(
                        messageCode = QUALITY_INDICATOR_ELEMENT_NAME_KEY.format(indicator.id),
                        defaultMessage = indicator.elementName
                    ),
                    elementDetail = if (indicatorCnName.isNullOrBlank()) indicator.elementDetail else indicatorCnName,
                    metadatas = metadata,
                    availableOperation = indicator.operationAvailable.split(",").map { QualityOperation.valueOf(it) },
                    dataType = QualityDataType.valueOf(indicator.thresholdType.toUpperCase()),
                    threshold = indicator.threshold,
                    desc = I18nUtil.getCodeLanMessage(
                        messageCode = QUALITY_INDICATOR_DESC_KEY.format(indicator.id),
                        defaultMessage = indicator.desc ?: ""
                    ),
                    range = indicator.indicatorRange // 脚本指标需要加上可见范围
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

        return IndicatorListResponse(
            scriptIndicators = scriptIndicators,
            systemIndicators = systemIndicators,
            marketIndicators = marketIndicators
        )
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
        logger.info("QUALITY|setTestIndicator userId: $userId, elementType: $elementType")
        val testIndicatorList = indicatorDao.listByElementType(dslContext, elementType, IndicatorType.MARKET)
            ?.filter { isTestIndicator(it) } ?: listOf()
        val testIndicatorMap = testIndicatorList.map { it.enName to it }.toMap()
        val lastIndicatorName = testIndicatorList.map { it.enName }
        val newIndicatorName = indicatorUpdateList.map { it.enName }

        // 删除这次没有的指标
        indicatorDao.delete(lastIndicatorName.minus(newIndicatorName).map {
            testIndicatorMap.getValue(it).id
        }, dslContext)

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

    // 把测试的数据刷到正式的， 有或无都update，多余的删掉
    fun serviceRefreshIndicator(elementType: String, metadataMap: Map<String /* dataId */, String /* id */>): Int {
        logger.info("QUALITY|refreshIndicator elementType: $elementType")
        val data = indicatorDao.listByElementType(dslContext, elementType, IndicatorType.MARKET)
        val testData = data?.filter { isTestIndicator(it) } ?: listOf()
        val prodData = data?.filter { !isTestIndicator(it) } ?: listOf()
        val userId = testData.firstOrNull()?.createUser ?: ""

        // 有则update
        val deleteItemId = mutableSetOf<Long>()
        prodData.forEach PROD@{ prodItem ->
            testData.forEach TEST@{ testItem ->
                if (prodItem.enName == testItem.enName) {
                    indicatorDao.update(userId, prodItem.id, IndicatorUpdate(
                        elementType = testItem.elementType,
                        elementName = testItem.elementName,
                        elementDetail = testItem.elementDetail,
                        elementVersion = null, // 刷新不需要更新插件版本
                        enName = testItem.enName,
                        cnName = testItem.cnName,
                        metadataIds = metadataMap[testItem.enName], // 插件市场注册的指标enName跟基础数据的dataId是一样的
                        defaultOperation = testItem.defaultOperation,
                        operationAvailable = testItem.operationAvailable,
                        threshold = testItem.threshold,
                        thresholdType = testItem.thresholdType,
                        desc = testItem.desc,
                        readOnly = testItem.indicatorReadOnly,
                        stage = testItem.stage,
                        range = "",
                        tag = "IN_READY_RUNNING",
                        enable = testItem.enable,
                        type = IndicatorType.MARKET,
                        logPrompt = testItem.logPrompt
                    ), dslContext)
                    return@PROD
                }
            }

            // test没有的，多余的删掉
            deleteItemId.add(prodItem.id)
        }

        indicatorDao.delete(deleteItemId, dslContext)

        // 没也update
        testData.forEach TEST@{ testItem ->
            prodData.forEach PROD@{ prodItem ->
                if (prodItem.enName == testItem.enName) return@TEST
            }
            indicatorDao.update(userId, testItem.id, IndicatorUpdate(
                elementType = testItem.elementType,
                elementName = testItem.elementName,
                elementDetail = testItem.elementDetail,
                elementVersion = testItem.atomVersion,
                enName = testItem.enName,
                cnName = testItem.cnName,
                metadataIds = metadataMap[testItem.enName], // 插件市场注册的指标enName跟基础数据的dataId是一样的
                defaultOperation = testItem.defaultOperation,
                operationAvailable = testItem.operationAvailable,
                threshold = testItem.threshold,
                thresholdType = testItem.thresholdType,
                desc = testItem.desc,
                readOnly = testItem.indicatorReadOnly,
                stage = testItem.stage,
                range = "",
                tag = "IN_READY_RUNNING",
                enable = testItem.enable,
                type = IndicatorType.valueOf(testItem.type),
                logPrompt = testItem.logPrompt
            ), dslContext)
        }

        return testData.size
    }

    fun serviceDeleteTestIndicator(elementType: String): Int {
        logger.info("QUALITY|deleteTestIndicator elementType: $elementType")
        val data = indicatorDao.listByElementType(dslContext, elementType)
        val testData = data?.filter { isTestIndicator(it) } ?: listOf()
        return indicatorDao.delete(testData.map { it.id }, dslContext)
    }

    fun listIndicatorByProject(projectId: String): List<TQualityIndicatorRecord> {
        val projectAtomsMap = getProjectAtomCodes(projectId)
        logger.info("QUALITY|get project atoms map: $projectAtomsMap")
        val atomCodes = projectAtomsMap.map { it.key }.toSet()

        val result = mutableListOf<TQualityIndicatorRecord>()
        val indicators = indicatorDao.listAll(dslContext)
            ?.filter { atomCodes.contains(it.elementType) } ?: return listOf()

        indicators.filter { it.type != IndicatorType.CUSTOM.name || it.indicatorRange == projectId } // 过滤调非本项目的脚本插件
            .groupBy { it.elementType }
            .forEach { (type, list) ->
                val installType = projectAtomsMap[type] ?: return@forEach
                // 测试项目和测试指标不为空的话，就只列出插件测试相关的指标
                val testIndicators = list.filter { isTestIndicator(it) }
                val isTestProject = installType == StoreProjectTypeEnum.TEST.name ||
                        installType == StoreProjectTypeEnum.INIT.name
                if (isTestProject && testIndicators.isNotEmpty()) {
                    result.addAll(testIndicators)
                } else {
                    val prodIndicators = list.filter { !isTestIndicator(it) }
                    result.addAll(prodIndicators)
                }
            }
        return result.filter { it.enable }
    }

    private fun isTestIndicator(qualityIndicator: TQualityIndicatorRecord): Boolean {
        return qualityIndicator.type == IndicatorType.MARKET.name && qualityIndicator.tag == "IN_READY_TEST"
    }

    private fun convertRecord(
        indicator: TQualityIndicatorRecord,
        metadata: List<QualityIndicator.Metadata> = listOf()
    ): QualityIndicator {
        return QualityIndicator(
            hashId = HashUtil.encodeLongId(indicator.id),
            elementType = indicator.elementType,
            elementDetail = indicator.elementDetail ?: "",
            enName = indicator.enName,
            cnName = I18nUtil.getCodeLanMessage(
                messageCode = QUALITY_INDICATOR_NAME_KEY.format(indicator.id),
                defaultMessage = indicator.cnName
            ),
            stage = I18nUtil.getCodeLanMessage(
                messageCode = indicator.stage,
                defaultMessage = indicator.stage ?: ""
            ),
            operation = QualityOperation.valueOf(indicator.defaultOperation),
            operationList = indicator.operationAvailable.split(",").map { QualityOperation.valueOf(it) },
            threshold = indicator.threshold,
            thresholdType = QualityDataType.valueOf(indicator.thresholdType),
            readOnly = if (indicator.tag == "TENCENTOPEN") true else indicator.indicatorReadOnly,
            type = indicator.type,
            tag = indicator.tag,
            metadataList = metadata,
            desc = I18nUtil.getCodeLanMessage(
                messageCode = QUALITY_INDICATOR_DESC_KEY.format(indicator.id),
                defaultMessage = indicator.desc
            ),
            logPrompt = indicator.logPrompt,
            enable = indicator.enable ?: false,
            range = indicator.indicatorRange
        )
    }

    private fun convertMetaIds(metadataIds: String?): List<Long> {
        if (metadataIds.isNullOrBlank()) return emptyList()
        return metadataIds!!.split(",").map { id -> id.toLong() }
    }

    private fun checkSystemIndicatorExist(enName: String, cnName: String): Boolean {
        val indicators = indicatorDao.listByType(dslContext, IndicatorType.SYSTEM) ?: return false
        if (indicators.any { it.enName == enName }) throw OperationException(
            I18nUtil.getCodeLanMessage(
                messageCode = QUALITY_INDICATOR_ENGLISH_NAME_EXISTS,
                params = arrayOf(enName)
            )
        )
        if (indicators.any { it.cnName == cnName }) throw OperationException(
            I18nUtil.getCodeLanMessage(
                messageCode = QUALITY_INDICATOR_CHINESE_NAME_EXISTS,
                params = arrayOf(cnName)
            )
        )
        return false
    }

    private fun checkSystemIndicatorExcludeExist(id: Long, enName: String, cnName: String): Boolean {
        val indicators = indicatorDao.listByType(dslContext, IndicatorType.SYSTEM) ?: return false
        val filterList = indicators.filter { it.id != id }
        if (filterList.any { it.enName == enName }) throw OperationException(
            I18nUtil.getCodeLanMessage(
                messageCode = QUALITY_INDICATOR_ENGLISH_NAME_EXISTS,
                params = arrayOf(enName)
            )
        )
        if (filterList.any { it.cnName == cnName }) throw OperationException(
            I18nUtil.getCodeLanMessage(
                messageCode = QUALITY_INDICATOR_CHINESE_NAME_EXISTS,
                params = arrayOf(cnName)
            )
        )
        return false
    }

    private fun checkCustomIndicatorExist(projectId: String, enName: String, cnName: String): Boolean {
        val indicators = indicatorDao.listByType(dslContext, IndicatorType.CUSTOM) ?: return false
        indicators.forEach { indicator ->
            if (indicator.indicatorRange != projectId) return@forEach
            if (indicator.enName == enName) throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = QUALITY_INDICATOR_ENGLISH_NAME_EXISTS,
                    params = arrayOf(enName)
                )
            )
            if (indicator.cnName == cnName) throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = QUALITY_INDICATOR_CHINESE_NAME_EXISTS,
                    params = arrayOf(cnName)
                )
            )
        }
        return false
    }

    private fun checkCustomIndicatorExcludeExist(id: Long, projectId: String, enName: String, cnName: String): Boolean {
        val indicators = indicatorDao.listByType(dslContext, IndicatorType.CUSTOM) ?: return false
        indicators.forEach { indicator ->
            if (indicator.id == id || indicator.indicatorRange != projectId) return@forEach
            if (indicator.enName == enName) throw OperationException(I18nUtil.getCodeLanMessage(
                messageCode = QUALITY_INDICATOR_ENGLISH_NAME_EXISTS,
                params = arrayOf(enName)
            ))
            if (indicator.cnName == cnName) throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = QUALITY_INDICATOR_CHINESE_NAME_EXISTS,
                    params = arrayOf(cnName)
                )
            )
        }
        return false
    }

    private fun checkCustomUpsertIndicator(projectId: String, enName: String): Long? {
        val indicators = indicatorDao.listByType(dslContext, IndicatorType.CUSTOM) ?: return null
        indicators.forEach { indicator ->
            if (indicator.enName == enName && indicator.indicatorRange == projectId) return indicator.id
        }
        return null
    }

    private fun getIndicatorUpdate(projectId: String, indicatorCreate: IndicatorCreate): IndicatorUpdate {
        return IndicatorUpdate(
            elementType = indicatorCreate.elementType,
            elementName = ElementUtils.getElementCnName(indicatorCreate.elementType, projectId),
            elementDetail = ElementUtils.getElementCnName(indicatorCreate.elementType, projectId),
            elementVersion = "",
            enName = indicatorCreate.name,
            cnName = indicatorCreate.cnName,
            metadataIds = "",
            defaultOperation = indicatorCreate.operation.firstOrNull()?.name,
            operationAvailable = indicatorCreate.operation.joinToString(","),
            threshold = indicatorCreate.threshold,
            thresholdType = indicatorCreate.dataType.name,
            desc = indicatorCreate.desc,
            readOnly = false,
            stage = DEVELOP,
            range = projectId,
            tag = "",
            enable = true,
            type = IndicatorType.CUSTOM
        )
    }

    private fun getProjectAtomCodes(projectId: String): Map<String, String> {
        return client.get(ServiceMarketAtomResource::class).getProjectElementsInfo(projectId).data ?: mutableMapOf()
    }

    private fun serviceListIndicatorRecord(qualityIndicators: List<TQualityIndicatorRecord>?): List<QualityIndicator> {
        val metadataIds = mutableSetOf<Long>()
        qualityIndicators?.forEach { indicator ->
            val metadataId = convertMetaIds(indicator.metadataIds)
            metadataIds.addAll(metadataId)
        }
        val metadataMap = metadataService.serviceListMetadata(metadataIds).associateBy { it.hashId }
        return qualityIndicators?.map {
            val metadataIds = convertMetaIds(it.metadataIds)
            val metadataList = metadataIds.map {
                val metadata = metadataMap[HashUtil.encodeLongId(it)]
                QualityIndicator.Metadata(metadata?.hashId ?: "", metadata?.dataName ?: "", metadata?.dataId ?: "")
            }
            convertRecord(it, metadataList)
        } ?: listOf()
    }

    fun userCount(projectId: String): Long {
        return indicatorDao.count(dslContext, projectId, true)
    }

    data class Msg(val code: Int, val msg: String, val flag: Boolean)

    companion object {
        private val logger = LoggerFactory.getLogger(QualityIndicatorService::class.java)

        fun getCodeccToolNameMap() = mapOf(
            "STANDARD" to I18nUtil.getCodeLanMessage(BK_TOOL_DESC_STANDARD),
            "DEFECT" to I18nUtil.getCodeLanMessage(BK_TOOL_DESC_DEFECT),
            "SECURITY" to I18nUtil.getCodeLanMessage(BK_TOOL_DESC_SECURITY),
            "COVERITY" to "Coverity",
            "KLOCWORK" to "Klocwork",
            "RIPS" to I18nUtil.getCodeLanMessage(BK_TOOL_DESC_RIPS),
            "SENSITIVE" to I18nUtil.getCodeLanMessage(BK_TOOL_DESC_SENSITIVE),
            "WOODPECKER_SENSITIVE" to I18nUtil.getCodeLanMessage(BK_TOOL_DESC_WOODPECKER_SENSITIVE),
            "BKCHECK-CPP" to "bkcheck-cpp",
            "BKCHECK-OC" to "bkcheck-oc",
            "CHECKSTYLE" to "Checkstyle",
            "CPPLINT" to "CppLint",
            "DETEKT" to "detekt",
            "ESLINT" to "ESLint",
            "GOML" to "Gometalinter",
            "OCCHECK" to "OCCheck",
            "PHPCS" to "PHPCS",
            "PYLINT" to "PyLint",
            "STYLECOP" to "StyleCop",
            "CCN" to I18nUtil.getCodeLanMessage(BK_TOOL_DESC_CCN),
            "DUPC" to I18nUtil.getCodeLanMessage(BK_TOOL_DESC_DUPC))

        private fun getCodeccToolDescMap() = mapOf(
            "STANDARD" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_STANDARD),
            "DEFECT" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_DEFECT),
            "SECURITY" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_SECURITY),
            "CCN" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_CCN),
            "DUPC" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_DUPC),
            "COVERITY" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_COVERITY),
            "KLOCWORK" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_KLOCWORK),
            "CPPLINT" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_CPPLINT),
            "ESLINT" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_ESLINT),
            "PYLINT" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_PYLINT),
            "GOML" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_GOML),
            "CHECKSTYLE" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_CHECKSTYLE),
            "STYLECOP" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_STYLECOP),
            "DETEKT" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_DETEKT),
            "PHPCS" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_PHPCS),
            "SENSITIVE" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_SENSITIVE),
            "OCCHECK" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_OCCHECK),
            "WOODPECKER_SENSITIVE" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_WOODPECKER_SENSITIVE),
            "BKCHECK-CPP" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_BKCHECK_CPP),
            "BKCHECK-OC" to I18nUtil.getCodeLanMessage(BK_TOOL_NAME_BKCHECK_OC))
    }
}
