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

package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.DEPLOY
import com.tencent.devops.common.api.constant.DEVELOP
import com.tencent.devops.common.api.constant.SECURITY
import com.tencent.devops.common.api.constant.TEST
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.quality.api.v2.ServiceQualityControlPointMarketResource
import com.tencent.devops.quality.api.v2.ServiceQualityIndicatorMarketResource
import com.tencent.devops.quality.api.v2.ServiceQualityMetadataMarketResource
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition.Companion.AFTER_POSITION
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition.Companion.BEFORE_POSITION
import com.tencent.devops.quality.api.v2.pojo.QualityControlPoint
import com.tencent.devops.quality.api.v2.pojo.enums.IndicatorType
import com.tencent.devops.quality.api.v2.pojo.op.IndicatorUpdate
import com.tencent.devops.quality.api.v2.pojo.op.QualityMetaData
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.AtomLabelRelDao
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.atom.MarketAtomEnvInfoDao
import com.tencent.devops.store.dao.atom.MarketAtomFeatureDao
import com.tencent.devops.store.dao.atom.MarketAtomVersionLogDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreReleaseDao
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.atom.AtomFeatureRequest
import com.tencent.devops.store.pojo.atom.AtomOfflineReq
import com.tencent.devops.store.pojo.atom.GetAtomConfigResult
import com.tencent.devops.store.pojo.atom.GetAtomQualityConfigResult
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.atom.UpdateAtomInfo
import com.tencent.devops.store.pojo.atom.enums.AtomPackageSourceTypeEnum
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.QUALITY_JSON_NAME
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.StoreProcessInfo
import com.tencent.devops.store.pojo.common.StoreReleaseCreateRequest
import com.tencent.devops.store.pojo.common.TASK_JSON_NAME
import com.tencent.devops.store.pojo.common.UN_RELEASE
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomNotifyService
import com.tencent.devops.store.service.atom.AtomQualityService
import com.tencent.devops.store.service.atom.AtomReleaseService
import com.tencent.devops.store.service.atom.MarketAtomArchiveService
import com.tencent.devops.store.service.atom.MarketAtomCommonService
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.websocket.StoreWebsocketService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.util.StringUtils
import java.time.LocalDateTime

abstract class AtomReleaseServiceImpl @Autowired constructor() : AtomReleaseService {

    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var atomDao: AtomDao
    @Autowired
    lateinit var marketAtomDao: MarketAtomDao
    @Autowired
    lateinit var storeProjectRelDao: StoreProjectRelDao
    @Autowired
    lateinit var marketAtomEnvInfoDao: MarketAtomEnvInfoDao
    @Autowired
    lateinit var storeMemberDao: StoreMemberDao
    @Autowired
    lateinit var marketAtomVersionLogDao: MarketAtomVersionLogDao
    @Autowired
    lateinit var marketAtomFeatureDao: MarketAtomFeatureDao
    @Autowired
    lateinit var atomLabelRelDao: AtomLabelRelDao
    @Autowired
    lateinit var storeReleaseDao: StoreReleaseDao
    @Autowired
    lateinit var atomNotifyService: AtomNotifyService
    @Autowired
    lateinit var atomQualityService: AtomQualityService
    @Autowired
    lateinit var marketAtomCommonService: MarketAtomCommonService
    @Autowired
    lateinit var marketAtomArchiveService: MarketAtomArchiveService
    @Autowired
    lateinit var storeCommonService: StoreCommonService
    @Autowired
    lateinit var redisOperation: RedisOperation
    @Autowired
    lateinit var client: Client
    @Autowired
    lateinit var storeWebsocketService: StoreWebsocketService

    companion object {
        private val logger = LoggerFactory.getLogger(AtomReleaseServiceImpl::class.java)
    }

    @Value("\${store.atomDetailBaseUrl}")
    protected lateinit var atomDetailBaseUrl: String

    private fun validateAddMarketAtomReq(
        userId: String,
        marketAtomCreateRequest: MarketAtomCreateRequest
    ): Result<Boolean> {
        logger.info("the validateAddMarketAtomReq userId is :$userId,marketAtomCreateRequest is :$marketAtomCreateRequest")
        val atomCode = marketAtomCreateRequest.atomCode
        // 判断插件代码是否存在
        val codeCount = atomDao.countByCode(dslContext, atomCode)
        if (codeCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(atomCode),
                false
            )
        }
        val atomName = marketAtomCreateRequest.name
        // 判断插件名称是否存在
        val nameCount = atomDao.countByName(dslContext, atomName)
        if (nameCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(atomName),
                false
            )
        }
        return Result(true)
    }

    override fun addMarketAtom(
        userId: String,
        marketAtomCreateRequest: MarketAtomCreateRequest
    ): Result<Boolean> {
        logger.info("addMarketAtom userId is :$userId,marketAtomCreateRequest is :$marketAtomCreateRequest")
        val atomCode = marketAtomCreateRequest.atomCode
        val validateResult = validateAddMarketAtomReq(userId, marketAtomCreateRequest)
        logger.info("the validateResult is :$validateResult")
        if (validateResult.isNotOk()) {
            return validateResult
        }
        val handleAtomPackageResult = handleAtomPackage(marketAtomCreateRequest, userId, atomCode)
        logger.info("the handleAtomPackageResult is :$handleAtomPackageResult")
        if (handleAtomPackageResult.isNotOk()) {
            return Result(handleAtomPackageResult.status, handleAtomPackageResult.message, null)
        }
        val handleAtomPackageMap = handleAtomPackageResult.data
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val id = UUIDUtil.generate()
            // 添加插件基本信息
            marketAtomDao.addMarketAtom(
                dslContext = context,
                userId = userId,
                id = id,
                repositoryHashId = handleAtomPackageMap?.get("repositoryHashId") ?: "",
                codeSrc = handleAtomPackageMap?.get("codeSrc") ?: "",
                docsLink = atomDetailBaseUrl + atomCode,
                marketAtomCreateRequest = marketAtomCreateRequest
            )
            // 添加插件与项目关联关系，type为0代表新增插件时关联的初始化项目
            storeProjectRelDao.addStoreProjectRel(
                dslContext = context,
                userId = userId,
                storeCode = atomCode,
                projectCode = marketAtomCreateRequest.projectCode,
                type = StoreProjectTypeEnum.INIT.type.toByte(),
                storeType = StoreTypeEnum.ATOM.type.toByte()
            )
            storeProjectRelDao.addStoreProjectRel(
                dslContext = context,
                userId = userId,
                storeCode = atomCode,
                projectCode = marketAtomCreateRequest.projectCode,
                type = StoreProjectTypeEnum.TEST.type.toByte(),
                storeType = StoreTypeEnum.ATOM.type.toByte()
            )
            val atomEnvRequest = AtomEnvRequest(
                userId = userId,
                pkgPath = "",
                language = marketAtomCreateRequest.language,
                minVersion = null,
                target = "",
                shaContent = null,
                preCmd = null
            )
            marketAtomEnvInfoDao.addMarketAtomEnvInfo(context, id, atomEnvRequest) // 添加流水线插件执行环境信息
            // 默认给新建插件的人赋予管理员权限
            storeMemberDao.addStoreMember(
                dslContext = context,
                userId = userId,
                storeCode = atomCode,
                userName = userId,
                type = StoreMemberTypeEnum.ADMIN.type.toByte(),
                storeType = StoreTypeEnum.ATOM.type.toByte()
            )
            // 添加插件特性信息
            marketAtomFeatureDao.addAtomFeature(
                dslContext = context,
                userId = userId,
                atomFeatureRequest = AtomFeatureRequest(
                    atomCode = atomCode,
                    recommendFlag = true,
                    yamlFlag = false
                )
            )
        }
        return Result(true)
    }

    abstract fun handleAtomPackage(
        marketAtomCreateRequest: MarketAtomCreateRequest,
        userId: String,
        atomCode: String
    ): Result<Map<String, String>?>

    abstract fun getAtomPackageSourceType(atomCode: String): AtomPackageSourceTypeEnum

    @Suppress("UNCHECKED_CAST")
    override fun updateMarketAtom(
        userId: String,
        projectCode: String,
        marketAtomUpdateRequest: MarketAtomUpdateRequest
    ): Result<String?> {
        logger.info("updateMarketAtom userId is :$userId,marketAtomUpdateRequest is :$marketAtomUpdateRequest")
        val atomCode = marketAtomUpdateRequest.atomCode
        val atomPackageSourceType = getAtomPackageSourceType(atomCode)
        logger.info("updateMarketAtom atomPackageSourceType is :$atomPackageSourceType")
        val version = marketAtomUpdateRequest.version
        if (atomPackageSourceType == AtomPackageSourceTypeEnum.UPLOAD) {
            // 校验可执行包sha摘要内容是否有效
            val packageShaContent = redisOperation.get("$projectCode:$atomCode:$version:packageShaContent")
            if (marketAtomUpdateRequest.packageShaContent != packageShaContent) {
                return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_UPLOAD_PACKAGE_INVALID)
            }
        }
        // 判断插件是不是首次创建版本
        val atomRecords = marketAtomDao.getAtomsByAtomCode(dslContext, atomCode)
        logger.info("the atomRecords is :$atomRecords")
        if (null == atomRecords || atomRecords.size < 1) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(atomCode))
        }
        // 判断更新的插件名称是否重复
        if (validateAtomNameIsExist(
                marketAtomUpdateRequest.name,
                atomRecords
            )
        ) return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PARAMETER_IS_EXIST,
            arrayOf(marketAtomUpdateRequest.name)
        )
        val atomRecord = atomRecords[0]
        // 校验前端传的版本号是否正确
        val releaseType = marketAtomUpdateRequest.releaseType
        val osList = marketAtomUpdateRequest.os
        val validateAtomVersionResult =
            marketAtomCommonService.validateAtomVersion(
                atomRecord = atomRecord,
                releaseType = releaseType,
                osList = osList,
                version = version
            )
        logger.info("validateAtomVersionResult is :$validateAtomVersionResult")
        if (validateAtomVersionResult.isNotOk()) {
            return Result(status = validateAtomVersionResult.status, message = validateAtomVersionResult.message ?: "")
        }
        val validateResult = validateUpdateMarketAtomReq(userId, marketAtomUpdateRequest, atomRecord)
        logger.info("validateUpdateMarketAtomReq validateResult is :$validateResult")
        if (validateResult.isNotOk()) {
            return Result(validateResult.status, validateResult.message, null)
        }
        var atomId = UUIDUtil.generate()
        val getAtomConfResult = getAtomConfig(
            atomPackageSourceType = atomPackageSourceType,
            projectCode = projectCode,
            atomCode = atomCode,
            atomVersion = version,
            repositoryHashId = atomRecord.repositoryHashId,
            userId = userId
        )
        if (getAtomConfResult.errorCode != "0") {
            return MessageCodeUtil.generateResponseDataObject(
                getAtomConfResult.errorCode,
                getAtomConfResult.errorParams
            )
        }

        // 解析quality.json
        val getAtomQualityResult = getAtomQualityConfig(
            projectCode = projectCode,
            atomCode = atomCode,
            atomName = marketAtomUpdateRequest.name,
            atomVersion = version,
            repositoryHashId = atomRecord.repositoryHashId,
            userId = userId
        )
        logger.info("update market atom, getAtomQualityResult: $getAtomQualityResult")
        if (getAtomQualityResult.errorCode == StoreMessageCode.USER_REPOSITORY_PULL_QUALITY_JSON_FILE_FAIL) {
            logger.info("quality.json not found , skip...")
        } else if (getAtomQualityResult.errorCode != "0") {
            return MessageCodeUtil.generateResponseDataObject(
                getAtomQualityResult.errorCode,
                getAtomQualityResult.errorParams
            )
        }

        val taskDataMap = getAtomConfResult.taskDataMap
        val atomEnvRequest = getAtomConfResult.atomEnvRequest ?: return MessageCodeUtil.generateResponseDataObject(
            StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_NULL, arrayOf("execution")
        )

        val propsMap = mutableMapOf<String, Any?>()
        propsMap["inputGroups"] = taskDataMap?.get("inputGroups")
        propsMap["input"] = taskDataMap?.get("input")
        propsMap["output"] = taskDataMap?.get("output")

        val classType = if (marketAtomUpdateRequest.os.isEmpty()) "marketBuildLess" else "marketBuild"
        val logoUrl = marketAtomUpdateRequest.logoUrl
        marketAtomUpdateRequest.os.sort() // 给操作系统排序
        val atomStatus =
            if (atomPackageSourceType == AtomPackageSourceTypeEnum.REPO) AtomStatusEnum.COMMITTING else AtomStatusEnum.TESTING
        val cancelFlag = atomRecord.atomStatus == AtomStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val props = JsonUtil.toJson(propsMap)
            if (StringUtils.isEmpty(atomRecord.version) || (cancelFlag && releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE)) {
                // 首次创建版本或者取消发布后不变更版本号重新上架，则在该版本的记录上做更新操作
                atomId = atomRecord.id
                val finalReleaseType = if (releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE) {
                    val atomVersion = marketAtomVersionLogDao.getAtomVersion(context, atomId)
                    atomVersion.releaseType
                } else {
                    releaseType.releaseType.toByte()
                }
                updateMarketAtom(
                    context = context,
                    userId = userId,
                    atomId = atomId,
                    atomStatus = atomStatus,
                    classType = classType,
                    props = props,
                    releaseType = finalReleaseType,
                    marketAtomUpdateRequest = marketAtomUpdateRequest,
                    atomEnvRequest = atomEnvRequest
                )
            } else {
                // 升级插件
                upgradeMarketAtom(
                    marketAtomUpdateRequest = marketAtomUpdateRequest,
                    context = context,
                    userId = userId,
                    atomId = atomId,
                    atomStatus = atomStatus,
                    classType = classType,
                    props = props,
                    atomEnvRequest = atomEnvRequest,
                    atomRecord = atomRecord
                )
            }
            // 更新标签信息
            val labelIdList = marketAtomUpdateRequest.labelIdList
            if (null != labelIdList) {
                atomLabelRelDao.deleteByAtomId(context, atomId)
                if (labelIdList.isNotEmpty())
                    atomLabelRelDao.batchAdd(context, userId, atomId, labelIdList)
            }
            asyncHandleUpdateAtom(context, atomId, userId)
        }
        return Result(atomId)
    }

    /**
     * 校验升级插件参数
     */
    abstract fun validateUpdateMarketAtomReq(
        userId: String,
        marketAtomUpdateRequest: MarketAtomUpdateRequest,
        atomRecord: TAtomRecord
    ): Result<Boolean>

    /**
     * 异步处理上架插件信息
     */
    abstract fun asyncHandleUpdateAtom(
        context: DSLContext,
        atomId: String,
        userId: String
    )

    private fun updateMarketAtom(
        context: DSLContext,
        userId: String,
        atomId: String,
        atomStatus: AtomStatusEnum,
        classType: String,
        props: String,
        releaseType: Byte,
        marketAtomUpdateRequest: MarketAtomUpdateRequest,
        atomEnvRequest: AtomEnvRequest
    ) {
        marketAtomDao.updateMarketAtom(
            dslContext = context,
            userId = userId,
            id = atomId,
            atomStatus = atomStatus,
            classType = classType,
            props = props,
            marketAtomUpdateRequest = marketAtomUpdateRequest
        )
        marketAtomVersionLogDao.addMarketAtomVersion(
            dslContext = context,
            userId = userId,
            atomId = atomId,
            releaseType = releaseType,
            versionContent = marketAtomUpdateRequest.versionContent
        )
        marketAtomEnvInfoDao.updateMarketAtomEnvInfo(context, atomId, atomEnvRequest)
        // 通过websocket推送状态变更消息
        storeWebsocketService.sendWebsocketMessage(userId, atomId)
    }

    @Suppress("UNCHECKED_CAST")
    protected fun getAtomQualityConfig(
        projectCode: String,
        atomCode: String,
        atomName: String,
        atomVersion: String,
        repositoryHashId: String,
        userId: String
    ): GetAtomQualityConfigResult {
        try {
            val qualityJsonStr = getFileStr(
                projectCode = projectCode,
                atomCode = atomCode,
                atomVersion = atomVersion,
                repositoryHashId = repositoryHashId,
                fileName = QUALITY_JSON_NAME
            )
            logger.info("the quality json str is :$qualityJsonStr")
            return if (!qualityJsonStr.isNullOrBlank()) {
                val qualityDataMap = JsonUtil.toMap(qualityJsonStr!!)
                val indicators = qualityDataMap["indicators"] as Map<String, Any>
                val stageCode = qualityDataMap["stage"] as String
                val stage = when (stageCode) {
                    "DEVELOP" -> MessageCodeUtil.getCodeLanMessage(DEVELOP)
                    "TEST" -> MessageCodeUtil.getCodeLanMessage(TEST)
                    "DEPLOY" -> MessageCodeUtil.getCodeLanMessage(DEPLOY)
                    "SECURITY" -> MessageCodeUtil.getCodeLanMessage(SECURITY)
                    else -> throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                        params = arrayOf(stageCode)
                    )
                }

                // 先注册基础数据
                val metadataResultMap = registerMetadata(
                    userId = userId,
                    atomCode = atomCode,
                    atomName = atomName,
                    indicators = indicators
                )

                // 再注册指标
                registerIndicator(
                    userId = userId,
                    projectId = projectCode,
                    atomCode = atomCode,
                    atomName = atomName,
                    atomVersion = atomVersion,
                    stage = stage,
                    metadataResultMap = metadataResultMap,
                    indicators = indicators
                )

                // 最后注册控制点
                registerControlPoint(
                    userId = userId,
                    atomCode = atomCode,
                    atomName = atomName,
                    atomVersion = atomVersion,
                    stage = stage,
                    projectId = projectCode
                )

                GetAtomQualityConfigResult("0", arrayOf(""))
            } else {
                try {
                    client.get(ServiceQualityIndicatorMarketResource::class).deleteTestIndicator(atomCode)
                    client.get(ServiceQualityMetadataMarketResource::class).deleteTestMetadata(atomCode)
                    client.get(ServiceQualityControlPointMarketResource::class).deleteTestControlPoint(atomCode)
                } catch (e: Exception) {
                    logger.error("clear atom:$atomCode test quality data error", e)
                }

                GetAtomQualityConfigResult(
                    StoreMessageCode.USER_REPOSITORY_PULL_QUALITY_JSON_FILE_FAIL,
                    arrayOf(QUALITY_JSON_NAME)
                )
            }
        } catch (e: Exception) {
            logger.error("getFileContent error is :$e", e)
            return GetAtomQualityConfigResult(
                StoreMessageCode.USER_ATOM_QUALITY_CONF_INVALID,
                arrayOf(QUALITY_JSON_NAME)
            )
        }
    }

    /**
     * 获取文件信息
     */
    abstract fun getFileStr(
        projectCode: String,
        atomCode: String,
        atomVersion: String,
        repositoryHashId: String,
        fileName: String
    ): String?

    private fun registerControlPoint(
        userId: String,
        atomCode: String,
        atomName: String,
        atomVersion: String,
        stage: String,
        projectId: String
    ) {
        client.get(ServiceQualityControlPointMarketResource::class).setTestControlPoint(
            userId, QualityControlPoint(
                "",
                atomCode,
                atomName,
                stage,
                listOf(ControlPointPosition(BEFORE_POSITION), ControlPointPosition(AFTER_POSITION)),
                ControlPointPosition(BEFORE_POSITION),
                true,
                atomVersion,
                projectId
            )
        )
    }

    private fun registerIndicator(
        userId: String,
        projectId: String,
        atomCode: String,
        atomName: String,
        atomVersion: String,
        stage: String,
        metadataResultMap: Map<String, Long>,
        indicators: Map<String, Any>
    ) {
        val indicatorsList = indicators.map {
            val map = it.value as Map<String, Any>
            val type = map["type"] as String?
            val valueType = map["valueType"] as String?
            IndicatorUpdate(
                elementType = atomCode,
                elementName = atomName,
                elementDetail = if (type.isNullOrBlank()) atomCode else type,
                elementVersion = atomVersion,
                enName = it.key,
                cnName = map["label"] as String,
                metadataIds = metadataResultMap[it.key]?.toString(),
                defaultOperation = map["defaultOp"] as String,
                operationAvailable = map["availableOp"] as String,
                threshold = map["threshold"] as String,
                thresholdType = if (valueType.isNullOrBlank()) "INT" else valueType,
                desc = map["desc"] as String?,
                readOnly = map["readOnly"] as Boolean? ?: false,
                stage = stage,
                range = projectId,
                tag = "IN_READY_TEST",
                enable = true,
                type = IndicatorType.MARKET,
                logPrompt = map["logPrompt"] as? String ?: ""
            )
        }
        client.get(ServiceQualityIndicatorMarketResource::class).setTestIndicator(userId, atomCode, indicatorsList)
    }

    private fun registerMetadata(
        userId: String,
        atomCode: String,
        atomName: String,
        indicators: Map<String, Any>
    ): Map<String, Long> {
        val metadataList = indicators.map {
            val map = it.value as Map<String, Any>
            val type = map["type"] as String?
            QualityMetaData(
                id = -1L,
                dataId = it.key,
                dataName = map["label"] as String,
                elementType = atomCode,
                elementName = atomName,
                elementDetail = if (type.isNullOrBlank()) atomCode else type,
                valueType = map["valueType"] as String? ?: "INT",
                desc = map["desc"] as String? ?: "",
                extra = "IN_READY_TEST" // 标注是正在测试中的
            )
        }
        return client.get(ServiceQualityMetadataMarketResource::class).setTestMetadata(
            userId,
            atomCode,
            metadataList
        ).data ?: mapOf()
    }

    @Suppress("UNCHECKED_CAST")
    protected fun getAtomConfig(
        atomPackageSourceType: AtomPackageSourceTypeEnum,
        projectCode: String,
        atomCode: String,
        atomVersion: String,
        repositoryHashId: String,
        userId: String
    ): GetAtomConfigResult {
        // 拉取task.json配置文件校验其合法性
        try {
            val taskJsonStr = getFileStr(
                projectCode = projectCode,
                atomCode = atomCode,
                atomVersion = atomVersion,
                repositoryHashId = repositoryHashId,
                fileName = TASK_JSON_NAME
            )
            logger.info("the taskJsonStr is :$taskJsonStr")
            if (null == taskJsonStr) {
                return GetAtomConfigResult(
                    StoreMessageCode.USER_REPOSITORY_PULL_TASK_JSON_FILE_FAIL,
                    arrayOf(TASK_JSON_NAME), null, null
                )
            }
            return parseTaskJson(atomPackageSourceType, taskJsonStr, projectCode, atomCode, atomVersion, userId)
        } catch (e: Exception) {
            logger.error("getFileContent error is :$e", e)
            return GetAtomConfigResult(
                StoreMessageCode.USER_ATOM_CONF_INVALID,
                arrayOf(TASK_JSON_NAME), null, null
            )
        }
    }

    private fun validateAtomNameIsExist(
        atomName: String,
        atomRecords: org.jooq.Result<TAtomRecord>
    ): Boolean {
        val count = atomDao.countByName(dslContext, atomName)
        var flag = false
        if (count > 0) {
            for (item in atomRecords) {
                if (atomName == item.name) {
                    flag = true
                    break
                }
            }
            if (!flag) {
                return true
            }
        }
        return false
    }

    private fun upgradeMarketAtom(
        marketAtomUpdateRequest: MarketAtomUpdateRequest,
        context: DSLContext,
        userId: String,
        atomId: String,
        atomStatus: AtomStatusEnum,
        classType: String,
        props: String,
        atomEnvRequest: AtomEnvRequest,
        atomRecord: TAtomRecord
    ) {
        marketAtomDao.upgradeMarketAtom(
            dslContext = context,
            userId = userId,
            id = atomId,
            atomStatus = atomStatus,
            classType = classType,
            props = props,
            atomRecord = atomRecord,
            atomRequest = marketAtomUpdateRequest
        )
        marketAtomEnvInfoDao.addMarketAtomEnvInfo(context, atomId, atomEnvRequest)
        marketAtomVersionLogDao.addMarketAtomVersion(
            dslContext = context,
            userId = userId,
            atomId = atomId,
            releaseType = marketAtomUpdateRequest.releaseType.releaseType.toByte(),
            versionContent = marketAtomUpdateRequest.versionContent
        )
        // 通过websocket推送状态变更消息
        storeWebsocketService.sendWebsocketMessage(userId, atomId)
    }

    @Suppress("UNCHECKED_CAST")
    protected fun parseTaskJson(
        atomPackageSourceType: AtomPackageSourceTypeEnum,
        taskJsonStr: String,
        projectCode: String,
        atomCode: String,
        version: String,
        userId: String
    ): GetAtomConfigResult {
        val taskDataMap = JsonUtil.toMap(taskJsonStr)
        val getAtomConfResult =
            marketAtomCommonService.parseBaseTaskJson(taskJsonStr, atomCode, userId)
        return if (getAtomConfResult.errorCode != "0") {
            getAtomConfResult
        } else {
            if (atomPackageSourceType == AtomPackageSourceTypeEnum.UPLOAD) {
                // 上传插件包发布方式需要校验task.json里面的执行包路径
                val executionInfoMap = taskDataMap["execution"] as Map<String, Any>
                val packagePath = executionInfoMap["packagePath"] as? String
                if (packagePath.isNullOrEmpty()) {
                    GetAtomConfigResult(
                        StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_NULL,
                        arrayOf("packagePath"), null, null
                    )
                } else {
                    val atomEnvRequest = getAtomConfResult.atomEnvRequest!!
                    atomEnvRequest.pkgPath = "$projectCode/$atomCode/$version/$packagePath"
                    atomEnvRequest.shaContent = redisOperation.get("$projectCode:$atomCode:$version:packageShaContent")
                }
            }
            getAtomConfResult
        }
    }

    /**
     * 获取插件版本发布进度
     */
    override fun getProcessInfo(userId: String, atomId: String): Result<StoreProcessInfo> {
        logger.info("getProcessInfo userId is $userId,atomId is $atomId")
        val record = marketAtomDao.getAtomRecordById(dslContext, atomId)
        logger.info("getProcessInfo record is $record")
        return if (null == record) {
            MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(atomId))
        } else {
            val atomCode = record.atomCode
            // 判断用户是否有查询权限
            val queryFlag = storeMemberDao.isStoreMember(dslContext, userId, atomCode, StoreTypeEnum.ATOM.type.toByte())
            if (!queryFlag) {
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
            }
            val status = record.atomStatus.toInt()
            // 查看当前版本之前的版本是否有已发布的，如果有已发布的版本则只是普通的升级操作而不需要审核
            val isNormalUpgrade = getNormalUpgradeFlag(atomCode, status)
            val processInfo = handleProcessInfo(isNormalUpgrade, status)
            val storeProcessInfo = storeCommonService.generateStoreProcessInfo(
                userId = userId,
                storeId = atomId,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM,
                creator = record.creator,
                processInfo = processInfo
            )
            logger.info("getProcessInfo storeProcessInfo is $storeProcessInfo")
            Result(storeProcessInfo)
        }
    }

    private fun getNormalUpgradeFlag(atomCode: String, status: Int): Boolean {
        val releaseTotalNum = marketAtomDao.countReleaseAtomByCode(dslContext, atomCode)
        val currentNum = if (status == AtomStatusEnum.RELEASED.status) 1 else 0
        return releaseTotalNum > currentNum
    }

    abstract fun handleProcessInfo(isNormalUpgrade: Boolean, status: Int): List<ReleaseProcessItem>

    /**
     * 取消发布
     */
    override fun cancelRelease(userId: String, atomId: String): Result<Boolean> {
        logger.info("cancelRelease, userId=$userId, atomId=$atomId")
        val status = AtomStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        val (checkResult, code) = checkAtomVersionOptRight(userId, atomId, status)
        if (!checkResult) {
            return MessageCodeUtil.generateResponseDataObject(code)
        }
        marketAtomDao.setAtomStatusById(
            dslContext,
            atomId,
            status,
            userId,
            MessageCodeUtil.getCodeLanMessage(UN_RELEASE)
        )
        // 通过websocket推送状态变更消息
        storeWebsocketService.sendWebsocketMessage(userId, atomId)
        // 删除质量红线相关数据
        val record = marketAtomDao.getAtomRecordById(dslContext, atomId) ?: return Result(true)
        val atomCode = record.atomCode
        client.get(ServiceQualityIndicatorMarketResource::class).deleteTestIndicator(atomCode)
        client.get(ServiceQualityMetadataMarketResource::class).deleteTestMetadata(atomCode)
        client.get(ServiceQualityControlPointMarketResource::class).deleteTestControlPoint(atomCode)
        return Result(true)
    }

    abstract fun getPassTestStatus(isNormalUpgrade: Boolean): Byte

    /**
     * 通过测试
     */
    override fun passTest(userId: String, atomId: String): Result<Boolean> {
        logger.info("passTest, userId=$userId, atomId=$atomId")
        val atomRecord = marketAtomDao.getAtomRecordById(dslContext, atomId)
        logger.info("passTest atomRecord is:$atomRecord")
        if (null == atomRecord) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(atomId),
                false
            )
        }
        // 查看当前版本之前的版本是否有已发布的，如果有已发布的版本则只是普通的升级操作而不需要审核
        val isNormalUpgrade = getNormalUpgradeFlag(atomRecord.atomCode, atomRecord.atomStatus.toInt())
        logger.info("passTest isNormalUpgrade is:$isNormalUpgrade")
        val atomStatus = getPassTestStatus(isNormalUpgrade)
        val (checkResult, code) = checkAtomVersionOptRight(userId, atomId, atomStatus, isNormalUpgrade)
        if (!checkResult) {
            return MessageCodeUtil.generateResponseDataObject(code)
        }
        if (isNormalUpgrade) {
            // 更新质量红线信息
            atomQualityService.updateQualityInApprove(atomRecord.atomCode, atomStatus)
            val creator = atomRecord.creator
            dslContext.transaction { t ->
                val context = DSL.using(t)
                // 清空旧版本LATEST_FLAG
                marketAtomDao.cleanLatestFlag(context, atomRecord.atomCode)
                // 记录发布信息
                val pubTime = LocalDateTime.now()
                storeReleaseDao.addStoreReleaseInfo(
                    dslContext = context,
                    userId = userId,
                    storeReleaseCreateRequest = StoreReleaseCreateRequest(
                        storeCode = atomRecord.atomCode,
                        storeType = StoreTypeEnum.ATOM,
                        latestUpgrader = creator,
                        latestUpgradeTime = pubTime
                    )
                )
                marketAtomDao.updateAtomInfoById(
                    context,
                    userId,
                    atomId,
                    UpdateAtomInfo(atomStatus = atomStatus, latestFlag = true, pubTime = pubTime)
                )
                // 通过websocket推送状态变更消息
                storeWebsocketService.sendWebsocketMessage(userId, atomId)
            }
            // 发送版本发布邮件
            atomNotifyService.sendAtomReleaseAuditNotifyMessage(atomId, AuditTypeEnum.AUDIT_SUCCESS)
        } else {
            marketAtomDao.setAtomStatusById(dslContext, atomId, atomStatus, userId, "")
            // 通过websocket推送状态变更消息
            storeWebsocketService.sendWebsocketMessage(userId, atomId)
        }
        return Result(true)
    }

    /**
     * 检查版本发布过程中的操作权限
     */
    abstract fun checkAtomVersionOptRight(
        userId: String,
        atomId: String,
        status: Byte,
        isNormalUpgrade: Boolean? = null
    ): Pair<Boolean, String>

    /**
     * 处理用户提交的下架插件请求
     */
    override fun offlineAtom(
        userId: String,
        atomCode: String,
        atomOfflineReq: AtomOfflineReq,
        checkPermissionFlag: Boolean
    ): Result<Boolean> {
        // 判断用户是否有权限下线
        if (checkPermissionFlag && !storeMemberDao.isStoreAdmin(
                dslContext = dslContext,
                userId = userId,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM.type.toByte()
            )
        ) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val version = atomOfflineReq.version
        val reason = atomOfflineReq.reason
        if (!version.isNullOrEmpty()) {
            val atomRecord = atomDao.getPipelineAtom(dslContext, atomCode, version!!.trim())
            logger.info("atomRecord is $atomRecord")
            if (null == atomRecord) {
                return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_INVALID,
                    arrayOf("$atomCode:$version"),
                    false
                )
            }
            if (AtomStatusEnum.RELEASED.status.toByte() != atomRecord.atomStatus) {
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
            }
            dslContext.transaction { t ->
                val context = DSL.using(t)
                val releaseAtomRecords = marketAtomDao.getReleaseAtomsByCode(context, atomCode, 2)
                if (null != releaseAtomRecords && releaseAtomRecords.size > 0) {
                    marketAtomDao.updateAtomInfoById(
                        dslContext = context,
                        atomId = atomRecord.id,
                        userId = userId,
                        updateAtomInfo = UpdateAtomInfo(
                            atomStatus = AtomStatusEnum.UNDERCARRIAGED.status.toByte(),
                            atomStatusMsg = reason,
                            latestFlag = false
                        )
                    )
                    val newestReleaseAtomRecord = releaseAtomRecords[0]
                    if (newestReleaseAtomRecord.id == atomRecord.id) {
                        var atomId: String? = null
                        if (releaseAtomRecords.size == 1) {
                            val newestUndercarriagedAtom =
                                marketAtomDao.getNewestUndercarriagedAtomsByCode(context, atomCode)
                            if (null != newestUndercarriagedAtom) {
                                atomId = newestUndercarriagedAtom.id
                            }
                        } else {
                            // 把前一个发布的版本的latestFlag置为true
                            val tmpAtomRecord = releaseAtomRecords[1]
                            atomId = tmpAtomRecord.id
                        }
                        if (null != atomId) {
                            marketAtomDao.updateAtomInfoById(
                                dslContext = context,
                                atomId = atomId,
                                userId = userId,
                                updateAtomInfo = UpdateAtomInfo(
                                    latestFlag = true
                                )
                            )
                        }
                    }
                }
            }
        } else {
            // 设置插件状态为下架
            dslContext.transaction { t ->
                val context = DSL.using(t)
                marketAtomDao.setAtomStatusByCode(
                    dslContext = context,
                    atomCode = atomCode,
                    atomOldStatus = AtomStatusEnum.RELEASED.status.toByte(),
                    atomNewStatus = AtomStatusEnum.UNDERCARRIAGED.status.toByte(),
                    userId = userId,
                    msg = reason,
                    latestFlag = false
                )
                val newestUndercarriagedAtom = marketAtomDao.getNewestUndercarriagedAtomsByCode(dslContext, atomCode)
                if (null != newestUndercarriagedAtom) {
                    // 把发布时间最晚的下架版本latestFlag置为true
                    marketAtomDao.updateAtomInfoById(
                        dslContext = context,
                        atomId = newestUndercarriagedAtom.id,
                        userId = userId,
                        updateAtomInfo = UpdateAtomInfo(
                            latestFlag = true
                        )
                    )
                }
            }
        }
        // 通过websocket推送状态变更消息
        storeWebsocketService.sendWebsocketMessageByAtomCodeAndUserId(atomCode, userId)
        // 通知使用方插件即将下架 -- todo

        return Result(true)
    }
}
