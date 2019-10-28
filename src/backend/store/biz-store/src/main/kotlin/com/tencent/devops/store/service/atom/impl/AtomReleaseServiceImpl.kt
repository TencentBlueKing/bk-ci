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

import com.tencent.devops.artifactory.api.service.ServiceImageManageResource
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.quality.api.v2.ServiceQualityControlPointResource
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
import com.tencent.devops.store.dao.atom.MarketAtomBuildAppRelDao
import com.tencent.devops.store.dao.atom.MarketAtomBuildInfoDao
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.atom.MarketAtomEnvInfoDao
import com.tencent.devops.store.dao.atom.MarketAtomFeatureDao
import com.tencent.devops.store.dao.atom.MarketAtomOfflineDao
import com.tencent.devops.store.dao.atom.MarketAtomVersionLogDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StorePipelineBuildRelDao
import com.tencent.devops.store.dao.common.StorePipelineRelDao
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
import com.tencent.devops.store.service.atom.AtomReleaseService
import com.tencent.devops.store.service.atom.MarketAtomArchiveService
import com.tencent.devops.store.service.atom.MarketAtomCommonService
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.websocket.StoreWebsocketService
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
    lateinit var marketAtomOfflineDao: MarketAtomOfflineDao
    @Autowired
    lateinit var marketAtomBuildInfoDao: MarketAtomBuildInfoDao
    @Autowired
    lateinit var marketAtomBuildAppRelDao: MarketAtomBuildAppRelDao
    @Autowired
    lateinit var storePipelineBuildRelDao: StorePipelineBuildRelDao
    @Autowired
    lateinit var storePipelineRelDao: StorePipelineRelDao
    @Autowired
    lateinit var marketAtomFeatureDao: MarketAtomFeatureDao
    @Autowired
    lateinit var atomLabelRelDao: AtomLabelRelDao
    @Autowired
    lateinit var storeReleaseDao: StoreReleaseDao
    @Autowired
    lateinit var atomNotifyService: AtomNotifyService
    @Autowired
    lateinit var marketAtomCommonService: MarketAtomCommonService
    @Autowired
    lateinit var marketAtomArchiveService: MarketAtomArchiveService
    @Autowired
    lateinit var storeCommonService: StoreCommonService
    @Autowired
    lateinit var websocketService: StoreWebsocketService
    @Autowired
    lateinit var redisOperation: RedisOperation
    @Autowired
    lateinit var client: Client

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
        if(marketAtomCreateRequest.atomPackageSourceType == AtomPackageSourceTypeEnum.REPO){
            marketAtomCreateRequest.authType ?: return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_NULL,
                arrayOf("authType"),
                false
            )
            marketAtomCreateRequest.visibilityLevel ?: return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_NULL,
                arrayOf("visibilityLevel"),
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
        val atomPackageSourceType = marketAtomCreateRequest.atomPackageSourceType
        val handleAtomPackageResult = handleAtomPackage(atomPackageSourceType, marketAtomCreateRequest, userId, atomCode)
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
                    atomCode = atomCode
                )
            )
        }
        return Result(true)
    }

    abstract fun handleAtomPackage(
        atomPackageSourceType: AtomPackageSourceTypeEnum,
        marketAtomCreateRequest: MarketAtomCreateRequest,
        userId: String,
        atomCode: String
    ): Result<Map<String,String>?>

    @Suppress("UNCHECKED_CAST")
    override fun updateMarketAtom(
        userId: String,
        projectCode: String,
        marketAtomUpdateRequest: MarketAtomUpdateRequest
    ): Result<String?> {
        logger.info("the get userId is :$userId,marketAtomUpdateRequest is :$marketAtomUpdateRequest")
        val atomPackageSourceType = marketAtomUpdateRequest.atomPackageSourceType
        val atomCode = marketAtomUpdateRequest.atomCode
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
        val dbVersion = atomRecord.version
        // 最近的版本处于上架中止状态，重新升级版本号不变
        val cancelFlag = atomRecord.atomStatus == AtomStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        val requireVersion =
            if (cancelFlag && releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE) dbVersion else storeCommonService.getRequireVersion(
                dbVersion,
                releaseType
            )
        val osList = marketAtomUpdateRequest.os
        val dbOsList = if (!StringUtils.isEmpty(atomRecord.os)) JsonUtil.getObjectMapper().readValue(
            atomRecord.os,
            List::class.java
        ) as List<String> else null
        // 支持的操作系统减少必须采用大版本升级方案
        val requireReleaseType =
            if (null != dbOsList && !osList.containsAll(dbOsList)) ReleaseTypeEnum.INCOMPATIBILITY_UPGRADE else marketAtomUpdateRequest.releaseType
        if (releaseType != requireReleaseType || version != requireVersion) {
            return MessageCodeUtil.generateResponseDataObject(
                StoreMessageCode.USER_ATOM_VERSION_IS_INVALID,
                arrayOf(version, requireVersion)
            )
        }
        if (atomRecords.size > 1) {
            // 判断最近一个插件版本的状态，只有处于审核驳回、已发布、上架中止和已下架的状态才允许添加新的版本
            val atomFinalStatusList = listOf(
                AtomStatusEnum.AUDIT_REJECT.status.toByte(),
                AtomStatusEnum.RELEASED.status.toByte(),
                AtomStatusEnum.GROUNDING_SUSPENSION.status.toByte(),
                AtomStatusEnum.UNDERCARRIAGED.status.toByte()
            )
            if (!atomFinalStatusList.contains(atomRecord.atomStatus)) {
                return MessageCodeUtil.generateResponseDataObject(
                    StoreMessageCode.USER_ATOM_VERSION_IS_NOT_FINISH,
                    arrayOf(atomRecord.name, atomRecord.version)
                )
            }
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
        var iconData: String? = ""
        if (null != logoUrl) {
            try {
                iconData = client.get(ServiceImageManageResource::class).compressImage(logoUrl, 18, 18).data
                logger.info("the iconData is :$iconData")
            } catch (e: Exception) {
                logger.error("compressImage error is :$e", e)
            }
        }

        marketAtomUpdateRequest.os.sort() // 给操作系统排序
        val atomStatus = if (atomPackageSourceType == AtomPackageSourceTypeEnum.REPO) AtomStatusEnum.COMMITTING else AtomStatusEnum.TESTING
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
                    context,
                    userId,
                    atomId,
                    atomStatus,
                    classType,
                    props,
                    iconData,
                    finalReleaseType,
                    marketAtomUpdateRequest,
                    atomEnvRequest
                )
            } else {
                // 升级插件
                upgradeMarketAtom(
                    marketAtomUpdateRequest,
                    context,
                    userId,
                    atomId,
                    atomStatus,
                    classType,
                    props,
                    iconData,
                    atomEnvRequest,
                    atomRecord
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
        iconData: String?,
        releaseType: Byte,
        marketAtomUpdateRequest: MarketAtomUpdateRequest,
        atomEnvRequest: AtomEnvRequest
    ) {
        marketAtomDao.updateMarketAtom(context, userId, atomId, atomStatus, classType, props, iconData, marketAtomUpdateRequest)
        marketAtomVersionLogDao.addMarketAtomVersion(
            context,
            userId,
            atomId,
            releaseType,
            marketAtomUpdateRequest.versionContent
        )
        marketAtomEnvInfoDao.updateMarketAtomEnvInfo(context, atomId, atomEnvRequest)
        // 通过websocket推送状态变更消息
        websocketService.sendWebsocketMessage(userId, atomId)
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
                val stage = when (qualityDataMap["stage"]) {
                    "DEVELOP" -> "开发"
                    "TEST" -> "测试"
                    "DEPLOY" -> "部署"
                    "SECURITY" -> "安全"
                    else -> throw RuntimeException("unsupported stage type, only allow:DEVELOP, TEST, DEPLOY, SECURITY")
                }

                // 先注册基础数据
                val metadataResultMap = registerMetadata(userId, atomCode, atomName, indicators)

                // 再注册指标
                registerIndicator(
                    userId,
                    projectCode,
                    atomCode,
                    atomName,
                    atomVersion,
                    stage,
                    metadataResultMap,
                    indicators
                )

                // 最后注册控制点
                registerControlPoint(userId, atomCode, atomName, atomVersion, stage, projectCode)

                GetAtomQualityConfigResult("0", arrayOf(""))
            } else {
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
        projectCode: String
    ) {
        client.get(ServiceQualityControlPointResource::class).set(
            userId, QualityControlPoint(
            "",
            atomCode,
            atomName,
            stage,
            listOf(ControlPointPosition(BEFORE_POSITION), ControlPointPosition(AFTER_POSITION)),
            ControlPointPosition(BEFORE_POSITION),
            true,
            atomVersion,
            projectCode
        )
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun registerIndicator(
        userId: String,
        projectCode: String,
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
                range = projectCode,
                tag = "IN_READY_TEST",
                enable = true,
                type = IndicatorType.MARKET,
                logPrompt = map["logPrompt"] as? String ?: ""
            )
        }
        client.get(ServiceQualityIndicatorMarketResource::class).setTestIndicator(userId, atomCode, indicatorsList)
    }

    @Suppress("UNCHECKED_CAST")
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
                -1L,
                it.key,
                map["label"] as String,
                atomCode,
                atomName,
                if (type.isNullOrBlank()) atomCode else type,
                map["valueType"] as String? ?: "INT",
                map["desc"] as String? ?: "",
                "IN_READY_TEST" // 标注是正在测试中的
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
        iconData: String?,
        atomEnvRequest: AtomEnvRequest,
        atomRecord: TAtomRecord
    ) {
        marketAtomDao.upgradeMarketAtom(
            context,
            userId,
            atomId,
            atomStatus,
            classType,
            props,
            iconData,
            atomRecord,
            marketAtomUpdateRequest
        )
        marketAtomEnvInfoDao.addMarketAtomEnvInfo(context, atomId, atomEnvRequest)
        marketAtomVersionLogDao.addMarketAtomVersion(
            context,
            userId,
            atomId,
            marketAtomUpdateRequest.releaseType.releaseType.toByte(),
            marketAtomUpdateRequest.versionContent
        )
        // 通过websocket推送状态变更消息
        websocketService.sendWebsocketMessage(userId, atomId)
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
            if (atomPackageSourceType == AtomPackageSourceTypeEnum.UPLOAD){
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
            val status = record.atomStatus.toInt()
            val atomCode = record.atomCode
            // 查看当前版本之前的版本是否有已发布的，如果有已发布的版本则只是普通的升级操作而不需要审核
            val isNormalUpgrade = getNormalUpgradeFlag(atomCode, status)
            val processInfo = handleProcessInfo(isNormalUpgrade, status)
            val storeProcessInfo = storeCommonService.generateStoreProcessInfo(
                userId = userId,
                storeId = atomId,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM,
                modifier = record.modifier,
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
        websocketService.sendWebsocketMessage(userId, atomId)
        // 删除质量红线相关数据
        val record = marketAtomDao.getAtomRecordById(dslContext, atomId) ?: return Result(true)
        val atomCode = record.atomCode
        client.get(ServiceQualityIndicatorMarketResource::class).deleteTestIndicator(atomCode)
        client.get(ServiceQualityMetadataMarketResource::class).deleteTestMetadata(atomCode)
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
        val (checkResult, code) = checkAtomVersionOptRight(userId, atomId, atomStatus)
        if (!checkResult) {
            return MessageCodeUtil.generateResponseDataObject(code)
        }
        if (isNormalUpgrade) {
            // 更新质量红线信息
            updateQualityInApprove(atomRecord.atomCode, atomStatus)
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
                marketAtomDao.updateAtomInfo(
                    context,
                    userId,
                    atomId,
                    UpdateAtomInfo(atomStatus = atomStatus, latestFlag = true, pubTime = pubTime)
                )
                // 通过websocket推送状态变更消息
                websocketService.sendWebsocketMessage(userId, atomId)
            }
            // 发送版本发布邮件
            atomNotifyService.sendAtomReleaseAuditNotifyMessage(atomId, AuditTypeEnum.AUDIT_SUCCESS)
        } else {
            marketAtomDao.setAtomStatusById(dslContext, atomId, atomStatus, userId, "")
            // 通过websocket推送状态变更消息
            websocketService.sendWebsocketMessage(userId, atomId)
        }
        return Result(true)
    }

    /**
     * 检查版本发布过程中的操作权限：重新构建、确认测试完成、取消发布
     */
    protected fun checkAtomVersionOptRight(userId: String, atomId: String, status: Byte): Pair<Boolean, String> {
        val record =
            marketAtomDao.getAtomById(dslContext, atomId) ?: return Pair(false, CommonMessageCode.PARAMETER_IS_INVALID)
        val atomCode = record["atomCode"] as String
        val creator = record["creator"] as String
        val recordStatus = record["atomStatus"] as Byte

        // 判断用户是否有权限
        if (!(storeMemberDao.isStoreAdmin(
                dslContext,
                userId,
                atomCode,
                StoreTypeEnum.ATOM.type.toByte()
            ) || creator == userId)
        ) {
            return Pair(false, CommonMessageCode.PERMISSION_DENIED)
        }

        logger.info("record status=$recordStatus, status=$status")
        if (status == AtomStatusEnum.AUDITING.status.toByte() &&
            recordStatus != AtomStatusEnum.TESTING.status.toByte()
        ) {
            return Pair(false, StoreMessageCode.USER_ATOM_RELEASE_STEPS_ERROR)
        } else if (status == AtomStatusEnum.BUILDING.status.toByte() &&
            recordStatus !in (listOf(AtomStatusEnum.BUILD_FAIL.status.toByte(), AtomStatusEnum.TESTING.status.toByte()))
        ) {
            return Pair(false, StoreMessageCode.USER_ATOM_RELEASE_STEPS_ERROR)
        } else if (status == AtomStatusEnum.GROUNDING_SUSPENSION.status.toByte() &&
            recordStatus in (listOf(AtomStatusEnum.RELEASED.status.toByte()))
        ) {
            return Pair(false, StoreMessageCode.USER_ATOM_RELEASE_STEPS_ERROR)
        }

        return Pair(true, "")
    }

    private fun updateQualityInApprove(atomCode: String, atomStatus: Byte) {
        logger.info("update quality atomStatus: $atomStatus")
        if (atomStatus == AtomStatusEnum.RELEASED.status.toByte()) {
            // 审核通过就刷新基础数据和指标
            val metadataMap =
                client.get(ServiceQualityMetadataMarketResource::class).refreshMetadata(atomCode).data ?: mapOf()
            client.get(ServiceQualityIndicatorMarketResource::class).refreshIndicator(atomCode, metadataMap)
            client.get(ServiceQualityControlPointResource::class).cleanTestProject(atomCode)
        }
        // 删除测试数据
        client.get(ServiceQualityMetadataMarketResource::class).deleteTestMetadata(atomCode)
        client.get(ServiceQualityIndicatorMarketResource::class).deleteTestIndicator(atomCode)
    }

    /**
     * 处理用户提交的下架插件请求
     */
    override fun offlineAtom(userId: String, atomCode: String, atomOfflineReq: AtomOfflineReq): Result<Boolean> {
        // 判断用户是否有权限下线
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, atomCode, StoreTypeEnum.ATOM.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        // 初始化下线记录
        marketAtomOfflineDao.create(dslContext, atomCode, atomOfflineReq.bufferDay, userId, 0)
        // 设置插件状态为下架中
        marketAtomDao.setAtomStatusByCode(
            dslContext, atomCode, AtomStatusEnum.RELEASED.status.toByte(),
            AtomStatusEnum.UNDERCARRIAGING.status.toByte(), userId, atomOfflineReq.reason
        )
        // 通过websocket推送状态变更消息
        websocketService.sendWebsocketMessageByAtomCodeAndUserId(atomCode, userId)
        // 通知使用方插件即将下架 -- todo

        return Result(true)
    }
}
