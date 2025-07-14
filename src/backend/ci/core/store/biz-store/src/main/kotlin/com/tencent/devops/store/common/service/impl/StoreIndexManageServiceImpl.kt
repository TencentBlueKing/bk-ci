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

package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_INVALID_PARAM_
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.TStoreIndexBaseInfo
import com.tencent.devops.model.store.tables.TStoreIndexLevelInfo
import com.tencent.devops.model.store.tables.TStoreIndexResult
import com.tencent.devops.model.store.tables.records.TStoreIndexBaseInfoRecord
import com.tencent.devops.model.store.tables.records.TStoreIndexElementDetailRecord
import com.tencent.devops.model.store.tables.records.TStoreIndexLevelInfoRecord
import com.tencent.devops.model.store.tables.records.TStoreIndexResultRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.store.common.dao.StoreIndexManageInfoDao
import com.tencent.devops.store.common.dao.StorePipelineRelDao
import com.tencent.devops.store.common.dao.StoreProjectRelDao
import com.tencent.devops.store.common.service.StoreIndexManageService
import com.tencent.devops.store.common.service.StoreIndexPipelineService
import com.tencent.devops.store.common.service.action.StoreDecorateFactory
import com.tencent.devops.store.pojo.common.BK_ATOM_QUALITY_INDEX
import com.tencent.devops.store.pojo.common.BK_ATOM_SLA
import com.tencent.devops.store.pojo.common.BK_ATOM_SLA_INDEX
import com.tencent.devops.store.pojo.common.BK_CODE_QUALITY
import com.tencent.devops.store.pojo.common.BK_COMPLIANCE_RATE
import com.tencent.devops.store.pojo.common.BK_NOT_UP_TO_PAR
import com.tencent.devops.store.pojo.common.BK_NO_FAIL_DATA
import com.tencent.devops.store.pojo.common.BK_STORE_TRUSTWORTHY_INDEX
import com.tencent.devops.store.pojo.common.BK_STORE_TRUSTWORTHY_TIPS
import com.tencent.devops.store.pojo.common.BK_UP_TO_PAR
import com.tencent.devops.store.pojo.common.STORE_CODE
import com.tencent.devops.store.pojo.common.enums.IndexExecuteTimeTypeEnum
import com.tencent.devops.store.pojo.common.enums.IndexOperationTypeEnum
import com.tencent.devops.store.pojo.common.enums.StorePipelineBusTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.index.CreateIndexComputeDetailRequest
import com.tencent.devops.store.pojo.common.index.StoreIndexBaseInfo
import com.tencent.devops.store.pojo.common.index.StoreIndexCreateRequest
import com.tencent.devops.store.pojo.common.index.StoreIndexInfo
import com.tencent.devops.store.pojo.common.index.StoreIndexPipelineInitRequest
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreIndexManageServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeIndexPipelineService: StoreIndexPipelineService,
    private val storePipelineRelDao: StorePipelineRelDao,
    private val storeIndexManageInfoDao: StoreIndexManageInfoDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val client: Client
) : StoreIndexManageService {

    override fun add(userId: String, storeIndexCreateRequest: StoreIndexCreateRequest): Result<Boolean> {
        val indexCode = storeIndexCreateRequest.indexCode
        // 验证指标代码是否已存在
        val validateResult = validateAddStoreIndexCreateReq(storeIndexCreateRequest)
        if (validateResult != null) {
            logger.info("the validateResult is :$validateResult")
            return validateResult
        }
        val storeIndexBaseInfoId = UUIDUtil.generate()
        val tStoreIndexBaseInfoRecord = TStoreIndexBaseInfoRecord()
        tStoreIndexBaseInfoRecord.id = storeIndexBaseInfoId
        tStoreIndexBaseInfoRecord.storeType = storeIndexCreateRequest.storeType.type.toByte()
        tStoreIndexBaseInfoRecord.indexCode = indexCode
        tStoreIndexBaseInfoRecord.indexName = storeIndexCreateRequest.indexName
        tStoreIndexBaseInfoRecord.description = storeIndexCreateRequest.description
        tStoreIndexBaseInfoRecord.operationType = storeIndexCreateRequest.operationType.name
        tStoreIndexBaseInfoRecord.executeTimeType = storeIndexCreateRequest.executeTimeType.name
        tStoreIndexBaseInfoRecord.weight = storeIndexCreateRequest.weight
        tStoreIndexBaseInfoRecord.creator = userId
        tStoreIndexBaseInfoRecord.modifier = userId
        tStoreIndexBaseInfoRecord.createTime = LocalDateTime.now()
        tStoreIndexBaseInfoRecord.updateTime = LocalDateTime.now()
        // 创建指标等级
        val indexLevelInfoRecords = storeIndexCreateRequest.levelInfos.map {
            val tStoreIndexLevelInfo = TStoreIndexLevelInfoRecord()
            tStoreIndexLevelInfo.id = UUIDUtil.generate()
            tStoreIndexLevelInfo.levelName = it.levelName
            tStoreIndexLevelInfo.iconUrl = it.iconUrl
            tStoreIndexLevelInfo.indexId = storeIndexBaseInfoId
            tStoreIndexLevelInfo.creator = userId
            tStoreIndexLevelInfo.modifier = userId
            tStoreIndexLevelInfo.createTime = LocalDateTime.now()
            tStoreIndexLevelInfo.updateTime = LocalDateTime.now()
            tStoreIndexLevelInfo
        }
        storeIndexManageInfoDao.batchCreateStoreIndexLevelInfo(dslContext, indexLevelInfoRecords)
        // 如果运算类型为插件则需要初始化流水线
        if (storeIndexCreateRequest.operationType == IndexOperationTypeEnum.ATOM &&
            !storeIndexCreateRequest.atomCode.isNullOrBlank()
        ) {
            tStoreIndexBaseInfoRecord.atomCode = storeIndexCreateRequest.atomCode
            storeIndexManageInfoDao.createStoreIndexBaseInfo(dslContext, tStoreIndexBaseInfoRecord)
            storeIndexPipelineService.initStoreIndexPipeline(
                userId = userId,
                storeIndexPipelineInitRequest = StoreIndexPipelineInitRequest(
                    indexCode = indexCode,
                    atomCode = storeIndexCreateRequest.atomCode!!,
                    executeTimeType = storeIndexCreateRequest.executeTimeType,
                    storeType = storeIndexCreateRequest.storeType
                )
            )
        } else {
            storeIndexManageInfoDao.createStoreIndexBaseInfo(dslContext, tStoreIndexBaseInfoRecord)
        }
        return Result(true)
    }

    override fun delete(userId: String, indexId: String): Result<Boolean> {
        val indexBaseInfo =
            storeIndexManageInfoDao.getStoreIndexBaseInfoById(dslContext, indexId) ?: return Result(false)
        val atomCode = indexBaseInfo.atomCode
        // 如果运算类型为插件则需要初始化流水线
        if (indexBaseInfo.operationType == IndexOperationTypeEnum.ATOM.name) {
            val storePipelineRelRecord = storePipelineRelDao.getStorePipelineRel(
                dslContext = dslContext,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM,
                busType = StorePipelineBusTypeEnum.INDEX
            )
            if (storePipelineRelRecord != null) {
                val pipelineId = storePipelineRelRecord.pipelineId
                // 查询插件对应的初始化项目
                val initProjectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
                    dslContext = dslContext,
                    storeCode = atomCode,
                    storeType = StoreTypeEnum.ATOM.type.toByte()
                ) ?: ""
                val pipelineBuildInfo = client.get(ServiceBuildResource::class).getPipelineLatestBuildByIds(
                    initProjectCode,
                    listOf(pipelineId)
                ).data?.get(storePipelineRelRecord.pipelineId)
                pipelineBuildInfo?.let {
                    if (it.status == BuildStatus.PREPARE_ENV.name ||
                        it.status == BuildStatus.RUNNING.name
                    ) {
                        client.get(ServiceBuildResource::class).manualShutdown(
                            userId = userId,
                            projectId = initProjectCode,
                            pipelineId = pipelineId,
                            buildId = it.buildId,
                            channelCode = ChannelCode.AM
                        )
                    }
                }
            }
        }

        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeIndexManageInfoDao.deleteTStoreIndexLevelInfo(context, indexId)
            storeIndexManageInfoDao.deleteTStoreIndexBaseInfo(context, indexId)
            storeIndexManageInfoDao.deleteStoreIndexResulById(context, indexId)
            storeIndexManageInfoDao.deleteStoreIndexElementById(context, indexId)
        }
        return Result(true)
    }

    override fun list(userId: String, keyWords: String?, page: Int, pageSize: Int): Page<StoreIndexBaseInfo> {
        val count = storeIndexManageInfoDao.count(dslContext, keyWords)
        val records = storeIndexManageInfoDao.list(dslContext, keyWords, page, pageSize)
        return Page(
            count = count,
            page = page,
            pageSize = pageSize,
            records = records.map {
                StoreIndexBaseInfo(
                    id = it.id,
                    indexCode = it.indexCode,
                    indexName = it.indexName,
                    description = it.description,
                    operationType = IndexOperationTypeEnum.valueOf(it.operationType),
                    atomCode = it.atomCode,
                    atomVersion = it.atomVersion,
                    finishTaskNum = it.finishTaskNum,
                    totalTaskNum = it.totalTaskNum,
                    executeTimeType = IndexExecuteTimeTypeEnum.valueOf(it.executeTimeType),
                    storeType = StoreTypeEnum.getStoreTypeObj(it.storeType.toInt())!!,
                    weight = it.weight,
                    creator = it.creator,
                    createTime = it.createTime,
                    modifier = it.modifier,
                    updateTime = it.updateTime
                )
            }
        )
    }

    override fun getStoreIndexInfosByStoreCodes(
        storeType: StoreTypeEnum,
        storeCodes: List<String>
    ): Map<String, List<StoreIndexInfo>> {
        val storeIndexInfosMap = mutableMapOf<String, List<StoreIndexInfo>>()
        val storeIndexInfosRecords =
            storeIndexManageInfoDao.getStoreIndexInfosByStoreCodes(dslContext, storeType, storeCodes)
        storeIndexInfosRecords.forEach {
            val storeCode = it[STORE_CODE] as String
            val storeIndexInfos =
                storeIndexInfosMap[storeCode]?.toMutableList() ?: emptyList<StoreIndexInfo>().toMutableList()
            val tStoreIndexResult = TStoreIndexResult.T_STORE_INDEX_RESULT
            val tStoreIndexBaseInfo = TStoreIndexBaseInfo.T_STORE_INDEX_BASE_INFO
            val tStoreIndexLevelInfo = TStoreIndexLevelInfo.T_STORE_INDEX_LEVEL_INFO
            val iconUrl = it[tStoreIndexLevelInfo.ICON_URL]
            storeIndexInfos.add(
                StoreIndexInfo(
                    indexCode = it[tStoreIndexResult.INDEX_CODE],
                    indexName = it[tStoreIndexBaseInfo.INDEX_NAME],
                    iconUrl = iconUrl?.let {
                        StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(iconUrl) as? String
                    } ?: "",
                    description = it[tStoreIndexBaseInfo.DESCRIPTION],
                    indexLevelName = I18nUtil.getCodeLanMessage(
                        messageCode = it[tStoreIndexLevelInfo.LEVEL_NAME],
                        language = I18nUtil.getDefaultLocaleLanguage(),
                        defaultMessage = it[tStoreIndexLevelInfo.LEVEL_NAME]
                    ),
                    hover = handleHover(
                        storeCode = storeCode,
                        indexCode = it[tStoreIndexResult.INDEX_CODE],
                        storeType = storeType
                    )
                )
            )
            storeIndexInfosMap[storeCode] = storeIndexInfos
        }

        return storeIndexInfosMap
    }

    fun handleHover(storeCode: String, indexCode: String, storeType: StoreTypeEnum): String {
        val elementDeilResult = storeIndexManageInfoDao.getElementDeilByStoreCode(
            dslContext = dslContext,
            storeCode = storeCode,
            indexCode = indexCode,
            storeType = storeType
        )
        if (elementDeilResult.isEmpty()) return ""
        return when (indexCode) {
            BK_ATOM_QUALITY_INDEX -> buildQualityIndexHover(elementDeilResult)
            BK_ATOM_SLA_INDEX -> buildSlaIndexHover(elementDeilResult)
            BK_STORE_TRUSTWORTHY_INDEX -> {
                buildTrustworthyIndexHover(elementDeilResult)
            }

            else -> ""
        }
    }

    private fun buildQualityIndexHover(elementDeilResult: List<TStoreIndexElementDetailRecord>): String {
        val complianceRateValue =
            elementDeilResult.find { it.elementCode == BK_COMPLIANCE_RATE }?.elementValue?.toDoubleOrNull()
        val codeQualityValue =
            elementDeilResult.find { it.elementCode == BK_CODE_QUALITY }?.elementValue?.toDoubleOrNull() ?: 0.0

        val complianceRateTips = complianceRateValue?.let {
            val resultCode = if (it >= 99.9) BK_UP_TO_PAR else BK_NOT_UP_TO_PAR
            "$it%(${I18nUtil.getCodeLanMessage(resultCode)})"
        } ?: I18nUtil.getCodeLanMessage(BK_NO_FAIL_DATA)

        val qualityIndexResult = I18nUtil.getCodeLanMessage(
            if (codeQualityValue >= 100) BK_UP_TO_PAR else BK_NOT_UP_TO_PAR
        )

        return """
        <span style="line-height: 18px">
            <span>${I18nUtil.getCodeLanMessage(BK_COMPLIANCE_RATE)}：$complianceRateTips</span>
            <br>
            <span>${I18nUtil.getCodeLanMessage(BK_CODE_QUALITY)}：$codeQualityValue（$qualityIndexResult）</span>
        </span>
    """.trimIndent()
    }

    private fun buildSlaIndexHover(elementDeilResult: List<TStoreIndexElementDetailRecord>): String {
        val slaValue = elementDeilResult.find { it.elementCode == BK_ATOM_SLA_INDEX }?.elementValue?.toDoubleOrNull()

        val slaTips = slaValue?.let {
            val resultCode = if (it >= 99.9) BK_UP_TO_PAR else BK_NOT_UP_TO_PAR
            "$it%(${I18nUtil.getCodeLanMessage(resultCode)})"
        }

        return """
        <span style="line-height: 18px">
            <span>${I18nUtil.getCodeLanMessage(BK_ATOM_SLA)}：$slaTips</span>
        </span>
    """.trimIndent()
    }

    private fun buildTrustworthyIndexHover(elementDeilResult: List<TStoreIndexElementDetailRecord>): String {
        val trustworthyValue = elementDeilResult.find { it.elementCode == BK_STORE_TRUSTWORTHY_INDEX }?.elementValue
        trustworthyValue?.let {
            return """
        <span style="line-height: 18px">
            <span>${I18nUtil.getCodeLanMessage(BK_STORE_TRUSTWORTHY_TIPS)}</span>
        </span>
        """.trimIndent()
        }
        return ""
    }

    override fun getStoreIndexInfosByStoreCode(
        storeType: StoreTypeEnum,
        storeCode: String
    ): List<StoreIndexInfo> {
        return getStoreIndexInfosByStoreCodes(storeType, listOf(storeCode))[storeCode] ?: emptyList()
    }

    override fun createIndexComputeDetail(
        userId: String,
        createIndexComputeDetailRequest: CreateIndexComputeDetailRequest
    ): Result<Boolean> {
        val indexId = storeIndexManageInfoDao.getStoreIndexBaseInfo(
            dslContext = dslContext,
            storeType = createIndexComputeDetailRequest.storeType,
            indexCode = createIndexComputeDetailRequest.indexCode
        ) ?: throw ErrorCodeException(
            errorCode = ERROR_INVALID_PARAM_,
            params = arrayOf("indexCode: ${createIndexComputeDetailRequest.indexCode}")
        )
        val levelId = storeIndexManageInfoDao.getStoreIndexLevelInfo(
            dslContext,
            indexId,
            createIndexComputeDetailRequest.levelName
        )?.id
        val tStoreIndexResultRecord = TStoreIndexResultRecord().apply {
            this.id = UUIDUtil.generate()
            this.indexId = indexId
            this.indexCode = createIndexComputeDetailRequest.indexCode
            this.storeCode = createIndexComputeDetailRequest.storeCode
            this.storeType = createIndexComputeDetailRequest.storeType.type.toByte()
            this.iconTips = createIndexComputeDetailRequest.iconTips
            this.levelId = levelId
            this.creator = userId
            this.modifier = userId
            this.updateTime = LocalDateTime.now()
            this.createTime = LocalDateTime.now()
        }
        storeIndexManageInfoDao.batchCreateStoreIndexResult(dslContext, listOf(tStoreIndexResultRecord))
        val tStoreIndexElementDetailRecords = createIndexComputeDetailRequest.elementInfos.map {
            TStoreIndexElementDetailRecord().apply {
                this.id = UUIDUtil.generate()
                this.storeCode = createIndexComputeDetailRequest.storeCode
                this.storeType = createIndexComputeDetailRequest.storeType.type.toByte()
                this.indexId = indexId
                this.indexCode = createIndexComputeDetailRequest.indexCode
                this.elementCode = it.elementCode
                this.elementName = it.elementName
                this.elementValue = it.elementValue
                this.remark = it.remark
                this.creator = userId
                this.modifier = userId
                this.updateTime = LocalDateTime.now()
                this.createTime = LocalDateTime.now()
            }
        }
        storeIndexManageInfoDao.batchCreateElementDetail(dslContext, tStoreIndexElementDetailRecords)
        return Result(true)
    }

    override fun deleteStoreIndexResultByStoreCode(
        userId: String,
        indexCode: String,
        storeType: StoreTypeEnum,
        storeCodes: List<String>
    ): Result<Boolean> {
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            storeIndexManageInfoDao.deleteStoreIndexElementDetailByStoreCode(
                dslContext = context,
                indexCode = indexCode,
                storeCodes = storeCodes,
                storeType = storeType
            )
            storeIndexManageInfoDao.deleteStoreIndexResultByStoreCode(
                dslContext = context,
                indexCode = indexCode,
                storeCodes = storeCodes,
                storeType = storeType
            )
        }
        return Result(true)
    }

    override fun updateTrustworthyIndexInfo(
        userId: String,
        deptCode: String,
        storeType: StoreTypeEnum,
        storeCodes: List<String>
    ): Result<Boolean> {
        val oldStoreCodes =
            storeIndexManageInfoDao.getStoreCodeByElementName(dslContext, TRUSTWORTHY_INDEX_CODE, deptCode)
        val intersects = storeCodes.intersect(oldStoreCodes.toSet())
        val delStoreCodes: List<String>
        val newStoreCodes = if (intersects.isNotEmpty()) {
            delStoreCodes = oldStoreCodes.subtract(intersects).toList()
            storeCodes.subtract(intersects)
        } else {
            delStoreCodes = oldStoreCodes
            storeCodes
        }
        if (delStoreCodes.isNotEmpty()) {
            deleteStoreIndexResultByStoreCode(
                userId = userId,
                indexCode = TRUSTWORTHY_INDEX_CODE,
                storeType = storeType,
                storeCodes = delStoreCodes
            )
        }
        if (newStoreCodes.isEmpty()) return Result(true)
        val indexId = storeIndexManageInfoDao.getStoreIndexBaseInfo(
            dslContext = dslContext,
            storeType = storeType,
            indexCode = TRUSTWORTHY_INDEX_CODE
        ) ?: throw ErrorCodeException(
            errorCode = ERROR_INVALID_PARAM_,
            params = arrayOf("indexCode: $TRUSTWORTHY_INDEX_CODE")
        )
        val levelId = storeIndexManageInfoDao.getStoreIndexLevelInfo(
            dslContext = dslContext,
            indexId = indexId,
            levelName = TRUSTWORTHY_INDEX_LEVEL_NAME
        )?.id
        val tStoreIndexResultRecords = mutableListOf<TStoreIndexResultRecord>()
        val tStoreIndexElementDetailRecords = mutableListOf<TStoreIndexElementDetailRecord>()
        newStoreCodes.forEach {
            val tStoreIndexResultRecord = TStoreIndexResultRecord().apply {
                this.id = UUIDUtil.generate()
                this.indexId = indexId
                this.indexCode = TRUSTWORTHY_INDEX_CODE
                this.storeCode = it
                this.storeType = storeType.type.toByte()
                this.iconTips = I18nUtil.getCodeLanMessage(TRUSTWORTHY_INDEX_LEVEL_NAME)
                this.levelId = levelId
                this.creator = userId
                this.modifier = userId
                this.updateTime = LocalDateTime.now()
                this.createTime = LocalDateTime.now()
            }
            tStoreIndexResultRecords.add(tStoreIndexResultRecord)
            val tStoreIndexElementDetailRecord = TStoreIndexElementDetailRecord().apply {
                this.id = UUIDUtil.generate()
                this.storeCode = it
                this.storeType = storeType.type.toByte()
                this.indexId = indexId
                this.indexCode = TRUSTWORTHY_INDEX_CODE
                this.elementCode = TRUSTWORTHY_INDEX_CODE
                this.elementName = TRUSTWORTHY_INDEX_CODE
                this.elementValue = "Certified"
                this.remark = null
                this.creator = userId
                this.modifier = userId
                this.updateTime = LocalDateTime.now()
                this.createTime = LocalDateTime.now()
            }
            tStoreIndexElementDetailRecords.add(tStoreIndexElementDetailRecord)
        }
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            storeIndexManageInfoDao.batchCreateStoreIndexResult(context, tStoreIndexResultRecords)
            storeIndexManageInfoDao.batchCreateElementDetail(context, tStoreIndexElementDetailRecords)
        }
        return Result(true)
    }

    private fun validateAddStoreIndexCreateReq(
        storeIndexCreateRequest: StoreIndexCreateRequest
    ): Result<Boolean>? {
        val indexCode = storeIndexCreateRequest.indexCode
        // 判断指标代码是否存在
        val codeCount = storeIndexManageInfoDao.getStoreIndexBaseInfoByCode(
            dslContext,
            storeIndexCreateRequest.storeType,
            indexCode
        )
        if (codeCount > 0) {
            // 抛出错误提示
            return I18nUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(indexCode)
            )
        }
        val indexName = storeIndexCreateRequest.indexName
        // 判断指标名称是否存在
        val nameCount = storeIndexManageInfoDao.getStoreIndexBaseInfoByName(
            dslContext,
            storeIndexCreateRequest.storeType,
            indexName
        )
        if (nameCount > 0) {
            // 抛出错误提示
            return I18nUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(indexName)
            )
        }
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StoreIndexManageServiceImpl::class.java)
        private const val TRUSTWORTHY_INDEX_CODE = "storeTrustworthyIndex"
        private const val TRUSTWORTHY_INDEX_LEVEL_NAME = "verifiedComponents"
    }
}
