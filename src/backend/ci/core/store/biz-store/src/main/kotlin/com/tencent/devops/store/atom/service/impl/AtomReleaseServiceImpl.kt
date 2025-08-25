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

package com.tencent.devops.store.atom.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.DEFAULT_LOCALE_LANGUAGE
import com.tencent.devops.common.api.constant.DEPLOY
import com.tencent.devops.common.api.constant.DEVELOP
import com.tencent.devops.common.api.constant.IN_READY_TEST
import com.tencent.devops.common.api.constant.KEY_DEFAULT_LOCALE_LANGUAGE
import com.tencent.devops.common.api.constant.KEY_REPOSITORY_HASH_ID
import com.tencent.devops.common.api.constant.MASTER
import com.tencent.devops.common.api.constant.SECURITY
import com.tencent.devops.common.api.constant.TEST
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonSchemaUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
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
import com.tencent.devops.store.atom.dao.AtomDao
import com.tencent.devops.store.atom.dao.AtomLabelRelDao
import com.tencent.devops.store.atom.dao.MarketAtomDao
import com.tencent.devops.store.atom.dao.MarketAtomEnvInfoDao
import com.tencent.devops.store.atom.dao.MarketAtomFeatureDao
import com.tencent.devops.store.atom.dao.MarketAtomVersionLogDao
import com.tencent.devops.store.atom.service.AtomIndexTriggerCalService
import com.tencent.devops.store.atom.service.AtomNotifyService
import com.tencent.devops.store.atom.service.AtomQualityService
import com.tencent.devops.store.atom.service.AtomReleaseService
import com.tencent.devops.store.atom.service.MarketAtomArchiveService
import com.tencent.devops.store.atom.service.MarketAtomCommonService
import com.tencent.devops.store.common.dao.StoreErrorCodeInfoDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.dao.StoreProjectRelDao
import com.tencent.devops.store.common.dao.StoreReleaseDao
import com.tencent.devops.store.common.dao.StoreStatisticTotalDao
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.service.StoreFileService
import com.tencent.devops.store.common.service.StoreI18nMessageService
import com.tencent.devops.store.common.service.StoreWebsocketService
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.common.utils.VersionUtils
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.GET_INFO_NO_PERMISSION
import com.tencent.devops.store.constant.StoreMessageCode.NO_COMPONENT_ADMIN_PERMISSION
import com.tencent.devops.store.constant.StoreMessageCode.USER_REPOSITORY_ERROR_JSON_FIELD_IS_INVALID
import com.tencent.devops.store.constant.StoreMessageCode.USER_UPLOAD_PACKAGE_INVALID
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.atom.AtomFeatureRequest
import com.tencent.devops.store.pojo.atom.AtomOfflineReq
import com.tencent.devops.store.pojo.atom.AtomReleaseRequest
import com.tencent.devops.store.pojo.atom.GetAtomConfigResult
import com.tencent.devops.store.pojo.atom.GetAtomQualityConfigResult
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.atom.UpdateAtomInfo
import com.tencent.devops.store.pojo.atom.UpdateAtomPackageInfo
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.ATOM_POST_VERSION_TEST_FLAG_KEY_PREFIX
import com.tencent.devops.store.pojo.common.ATOM_UPLOAD_ID_KEY_PREFIX
import com.tencent.devops.store.pojo.common.ERROR_JSON_NAME
import com.tencent.devops.store.pojo.common.KEY_CODE_SRC
import com.tencent.devops.store.pojo.common.KEY_CONFIG
import com.tencent.devops.store.pojo.common.KEY_EXECUTION
import com.tencent.devops.store.pojo.common.KEY_INPUT
import com.tencent.devops.store.pojo.common.KEY_INPUT_GROUPS
import com.tencent.devops.store.pojo.common.KEY_LANGUAGE
import com.tencent.devops.store.pojo.common.KEY_OUTPUT
import com.tencent.devops.store.pojo.common.KEY_PACKAGE_PATH
import com.tencent.devops.store.pojo.common.KEY_RELEASE_INFO
import com.tencent.devops.store.pojo.common.KEY_VERSION_INFO
import com.tencent.devops.store.pojo.common.QUALITY_JSON_NAME
import com.tencent.devops.store.pojo.common.STORE_LATEST_TEST_FLAG_KEY_PREFIX
import com.tencent.devops.store.pojo.common.StoreErrorCodeInfo
import com.tencent.devops.store.pojo.common.StoreI18nConfig
import com.tencent.devops.store.pojo.common.TASK_JSON_NAME
import com.tencent.devops.store.pojo.common.UN_RELEASE
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.PackageSourceTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.publication.StoreProcessInfo
import com.tencent.devops.store.pojo.common.publication.StoreReleaseCreateRequest
import java.time.LocalDateTime
import java.util.Locale
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@Suppress("ALL")
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
    lateinit var storeErrorCodeInfoDao: StoreErrorCodeInfoDao
    @Autowired
    lateinit var storeStatisticTotalDao: StoreStatisticTotalDao
    @Autowired
    lateinit var marketAtomCommonService: MarketAtomCommonService
    @Autowired
    lateinit var marketAtomArchiveService: MarketAtomArchiveService
    @Autowired
    lateinit var atomNotifyService: AtomNotifyService
    @Autowired
    lateinit var atomQualityService: AtomQualityService
    @Autowired
    lateinit var atomIndexTriggerCalService: AtomIndexTriggerCalService
    @Autowired
    lateinit var storeCommonService: StoreCommonService
    @Autowired
    lateinit var storeI18nMessageService: StoreI18nMessageService
    @Autowired
    lateinit var redisOperation: RedisOperation
    @Autowired
    lateinit var client: Client
    @Autowired
    lateinit var storeWebsocketService: StoreWebsocketService
    @Autowired
    lateinit var storeFileService: StoreFileService
    @Autowired
    lateinit var config: CommonConfig

    @Value("\${store.defaultAtomErrorCodeLength:6}")
    private var defaultAtomErrorCodeLength: Int = 6

    @Value("\${store.defaultAtomErrorCodePrefix:8}")
    private lateinit var defaultAtomErrorCodePrefix: String

    @Value("\${store.defaultAtomPublishReviewers:#{null}}")
    private val defaultAtomPublishReviewers: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(AtomReleaseServiceImpl::class.java)
    }

    private fun validateAddMarketAtomReq(
        marketAtomCreateRequest: MarketAtomCreateRequest
    ): Result<String>? {
        val atomCode = marketAtomCreateRequest.atomCode
        // 判断插件代码是否存在
        val codeCount = atomDao.countByCode(dslContext, atomCode)
        if (codeCount > 0) {
            // 抛出错误提示
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(atomCode)
            )
        }
        val atomName = marketAtomCreateRequest.name
        // 判断插件名称是否存在
        val nameCount = atomDao.countByName(dslContext, atomName)
        if (nameCount > 0) {
            // 抛出错误提示
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(atomName)
            )
        }
        return null
    }

    @BkTimed(extraTags = ["publish", "addMarketAtom"], value = "store_publish_pipeline_atom")
    override fun addMarketAtom(
        userId: String,
        marketAtomCreateRequest: MarketAtomCreateRequest
    ): Result<String> {
        logger.info("addMarketAtom userId is :$userId,marketAtomCreateRequest is :$marketAtomCreateRequest")
        val atomCode = marketAtomCreateRequest.atomCode
        val validateResult = validateAddMarketAtomReq(marketAtomCreateRequest)
        if (validateResult != null) {
            logger.info("the validateResult is :$validateResult")
            return validateResult
        }
        val handleAtomPackageResult = handleAtomPackage(marketAtomCreateRequest, userId, atomCode)
        logger.info("the handleAtomPackageResult is :$handleAtomPackageResult")
        if (handleAtomPackageResult.isNotOk()) {
            return Result(handleAtomPackageResult.status, handleAtomPackageResult.message, null)
        }
        val handleAtomPackageMap = handleAtomPackageResult.data
        val id = UUIDUtil.generate()
        val atomEnvRequest = AtomEnvRequest(
            userId = userId,
            language = marketAtomCreateRequest.language,
            defaultFlag = true
        )
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // 添加插件基本信息
            marketAtomDao.addMarketAtom(
                dslContext = context,
                userId = userId,
                id = id,
                repositoryHashId = handleAtomPackageMap?.get(KEY_REPOSITORY_HASH_ID) ?: "",
                codeSrc = handleAtomPackageMap?.get(KEY_CODE_SRC) ?: "",
                docsLink = storeCommonService.getStoreDetailUrl(StoreTypeEnum.ATOM, atomCode),
                marketAtomCreateRequest = marketAtomCreateRequest
            )
            // 初始化插件统计表数据
            storeStatisticTotalDao.initStatisticData(
                dslContext = context,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM.type.toByte()
            )
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
            if (marketAtomCreateRequest.projectCode.isBlank()) {
                return@transaction
            }
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
            marketAtomEnvInfoDao.addMarketAtomEnvInfo(context, id, listOf(atomEnvRequest))
        }
        handleAtomExtend(marketAtomCreateRequest, userId, atomCode)
        return Result(id)
    }

    abstract fun handleAtomPackage(
        marketAtomCreateRequest: MarketAtomCreateRequest,
        userId: String,
        atomCode: String
    ): Result<Map<String, String>?>

    abstract fun handleAtomExtend(
        marketAtomCreateRequest: MarketAtomCreateRequest,
        userId: String,
        atomCode: String
    )

    fun getAtomPackageSourceType(repositoryHashId: String?): PackageSourceTypeEnum {
        return if (repositoryHashId.isNullOrBlank()) {
            PackageSourceTypeEnum.UPLOAD
        } else {
            PackageSourceTypeEnum.REPO
        }
    }

    @Suppress("UNCHECKED_CAST")
    @BkTimed(extraTags = ["publish", "updateMarketAtom"], value = "store_publish_pipeline_atom")
    override fun updateMarketAtom(
        userId: String,
        projectCode: String,
        marketAtomUpdateRequest: MarketAtomUpdateRequest
    ): Result<String> {
        logger.info("updateMarketAtom userId is :$userId,marketAtomUpdateRequest is :$marketAtomUpdateRequest")
        val atomCode = marketAtomUpdateRequest.atomCode
        val version = marketAtomUpdateRequest.version
        // 判断插件是不是首次创建版本
        val atomCount = atomDao.countByCode(dslContext, atomCode)
        if (atomCount < 1) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val atomRecord = atomDao.getMaxVersionAtomByCode(dslContext, atomCode)!!
        val atomPackageSourceType = getAtomPackageSourceType(atomRecord.repositoryHashId)
        logger.info("updateMarketAtom atomPackageSourceType is :$atomPackageSourceType")
        val releaseType = marketAtomUpdateRequest.releaseType
        // 历史大版本下的小版本更新发布类型支持用户自定义分支，其他发布类型只支持master分支发布
        val branch = if (marketAtomUpdateRequest.branch.isNullOrBlank() ||
            releaseType != ReleaseTypeEnum.HIS_VERSION_UPGRADE
        ) {
            marketAtomUpdateRequest.branch = MASTER
            MASTER
        } else {
            marketAtomUpdateRequest.branch
        }
        val getAtomConfResult = getAtomConfig(
            projectCode = projectCode,
            atomCode = atomCode,
            atomVersion = version,
            userId = userId,
            repositoryHashId = atomRecord.repositoryHashId,
            branch = branch
        )
        if (getAtomConfResult.errorCode != "0") {
            return I18nUtil.generateResponseDataObject(
                messageCode = getAtomConfResult.errorCode,
                params = getAtomConfResult.errorParams,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val taskJsonMap = getAtomConfResult.taskDataMap
        val executionInfoMap = taskJsonMap[KEY_EXECUTION] as Map<String, Any>
        val atomLanguage = executionInfoMap[KEY_LANGUAGE].toString()
        // 把传入的请求报文参数做国际化
        val jsonMap = JsonUtil.toMutableMap(marketAtomUpdateRequest)
        val versionContentFieldName = MarketAtomUpdateRequest::versionContent.name
        jsonMap["$KEY_VERSION_INFO.$versionContentFieldName"] = marketAtomUpdateRequest.versionContent
        val defaultLocaleLanguage = taskJsonMap[KEY_DEFAULT_LOCALE_LANGUAGE]?.toString()
        jsonMap[KEY_DEFAULT_LOCALE_LANGUAGE] = defaultLocaleLanguage ?: DEFAULT_LOCALE_LANGUAGE
        val i18nDir = StoreUtils.getStoreI18nDir(atomLanguage, atomPackageSourceType)
        val updateRequestDataMap = storeI18nMessageService.parseJsonMapI18nInfo(
            userId = userId,
            jsonMap = jsonMap,
            storeI18nConfig = StoreI18nConfig(
                projectCode = projectCode,
                storeCode = atomCode,
                fileDir = "$atomCode/$version",
                i18nDir = i18nDir,
                propertiesKeyPrefix = KEY_RELEASE_INFO,
                dbKeyPrefix = StoreUtils.getStoreFieldKeyPrefix(StoreTypeEnum.ATOM, atomCode, version),
                repositoryHashId = atomRecord.repositoryHashId,
                branch = branch
            ),
            version = version
        ).toMutableMap()
        updateRequestDataMap[versionContentFieldName] =
            updateRequestDataMap["$KEY_VERSION_INFO.$versionContentFieldName"].toString()
        val convertUpdateRequest = JsonUtil.mapTo(updateRequestDataMap, MarketAtomUpdateRequest::class.java)
        // 判断更新的插件名称是否重复
        if (validateAtomNameIsExist(
                atomCode = atomCode,
                atomName = convertUpdateRequest.name
            )
        ) return I18nUtil.generateResponseDataObject(
            CommonMessageCode.PARAMETER_IS_EXIST,
            arrayOf(convertUpdateRequest.name)
        )
        // 校验前端传的版本号是否正确
        val osList = convertUpdateRequest.os
        val newestAtomRecord = atomDao.getNewestAtomByCode(dslContext, atomCode)!!
        val validateAtomVersionResult =
            marketAtomCommonService.validateAtomVersion(
                atomRecord = if (releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE) newestAtomRecord else atomRecord,
                releaseType = releaseType,
                osList = osList,
                version = version
            )
        logger.info("validateAtomVersionResult is :$validateAtomVersionResult")
        if (validateAtomVersionResult.isNotOk()) {
            return Result(status = validateAtomVersionResult.status, message = validateAtomVersionResult.message ?: "")
        }
        val atomId = if (atomPackageSourceType == PackageSourceTypeEnum.UPLOAD) {
            redisOperation.get("$ATOM_UPLOAD_ID_KEY_PREFIX:$atomCode:$version")
                ?: throw ErrorCodeException(errorCode = USER_UPLOAD_PACKAGE_INVALID)
        } else {
            UUIDUtil.generate()
        }
        val newVersionFlag = !(releaseType == ReleaseTypeEnum.NEW || releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE)
        return updateAtomVersionInfo(
            userId = userId,
            projectCode = projectCode,
            newVersionFlag = newVersionFlag,
            updateAtomPackageInfo = UpdateAtomPackageInfo(
                atomId = if (newVersionFlag) atomId else newestAtomRecord.id,
                i18nDir = i18nDir,
                packagePath = executionInfoMap[KEY_PACKAGE_PATH] as? String,
                atomPackageSourceType = atomPackageSourceType
            ),
            convertUpdateRequest = convertUpdateRequest,
            getAtomConfResult = getAtomConfResult
        )
    }

    /**
     * 校验升级插件参数
     */
    abstract fun validateUpdateMarketAtomReq(
        userId: String,
        marketAtomUpdateRequest: MarketAtomUpdateRequest,
        repositoryHashId: String?
    ): Result<Boolean>

    /**
     * 异步处理上架插件信息
     */
    abstract fun asyncHandleUpdateAtom(
        context: DSLContext,
        atomId: String,
        userId: String,
        branch: String? = null,
        validOsNameFlag: Boolean? = null,
        validOsArchFlag: Boolean? = null
    )

    protected fun updateMarketAtom(
        context: DSLContext,
        userId: String,
        atomId: String,
        atomStatus: AtomStatusEnum,
        classType: String,
        props: String,
        releaseType: Byte,
        marketAtomUpdateRequest: MarketAtomUpdateRequest,
        atomEnvRequests: List<AtomEnvRequest>,
        repositoryHashId: String?
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
        val atomPackageSourceType = getAtomPackageSourceType(repositoryHashId)
        if (atomPackageSourceType != PackageSourceTypeEnum.UPLOAD) {
            marketAtomEnvInfoDao.deleteAtomEnvInfoById(context, atomId)
            marketAtomEnvInfoDao.addMarketAtomEnvInfo(context, atomId, atomEnvRequests)
        }
        // 通过websocket推送状态变更消息
        storeWebsocketService.sendWebsocketMessage(userId, atomId)
    }

    override fun syncAtomErrorCodeConfig(
        atomCode: String,
        atomVersion: String,
        userId: String,
        repositoryHashId: String?,
        branch: String?
    ) {
        val projectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
            dslContext = dslContext,
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM.type.toByte()
        ) ?: ""
        try {
            // 获取插件error.json文件内容
            val errorJsonStr = getFileStr(
                projectCode = projectCode,
                atomCode = atomCode,
                atomVersion = atomVersion,
                fileName = ERROR_JSON_NAME,
                repositoryHashId = repositoryHashId,
                branch = if (branch.isNullOrBlank()) MASTER else branch
            )
            if (!errorJsonStr.isNullOrBlank() && JsonSchemaUtil.validateJson(errorJsonStr)) {
                val errorCodes = JsonUtil.to(errorJsonStr, object : TypeReference<Set<Int>>() {})
                if (errorCodes.isEmpty()) {
                    return
                }
                // 校验code码是否符合插件自定义错误码规范
                errorCodes.forEach { errorCode ->
                    val errorCodeStr = errorCode.toString()
                    if (errorCodeStr.length != defaultAtomErrorCodeLength ||
                        (!errorCodeStr.startsWith(defaultAtomErrorCodePrefix))
                    ) {
                        throw ErrorCodeException(errorCode = USER_REPOSITORY_ERROR_JSON_FIELD_IS_INVALID)
                    }
                }
                val atomLanguage =
                    marketAtomEnvInfoDao.getAtomLanguage(dslContext, atomCode, atomVersion) ?: throw ErrorCodeException(
                        errorCode = CommonMessageCode.ERROR_CLIENT_REST_ERROR
                    )
                storeI18nMessageService.parseErrorCodeI18nInfo(
                    userId = userId,
                    errorCodes = errorCodes,
                    version = atomVersion,
                    storeI18nConfig = StoreI18nConfig(
                        projectCode = projectCode,
                        storeCode = atomCode,
                        fileDir = "$atomCode/$atomVersion",
                        i18nDir = StoreUtils.getStoreI18nDir(atomLanguage, getAtomPackageSourceType(repositoryHashId)),
                        dbKeyPrefix = "${StoreTypeEnum.ATOM.name}.$atomCode.$atomVersion",
                        repositoryHashId = repositoryHashId,
                        branch = branch
                    )
                )
                val storeErrorCodeInfo = StoreErrorCodeInfo(
                    storeCode = atomCode,
                    storeType = StoreTypeEnum.ATOM,
                    errorCodes = errorCodes
                )
                storeErrorCodeInfoDao.batchUpdateErrorCodeInfo(dslContext, userId, storeErrorCodeInfo)
            }
        } catch (ignored: Exception) {
            logger.warn("syncAtomErrorCodeConfig fail $atomCode|error=${ignored.message}", ignored)
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun getAtomQualityConfig(
        projectCode: String,
        atomCode: String,
        atomName: String,
        atomVersion: String,
        userId: String,
        i18nDir: String,
        repositoryHashId: String? = null,
        branch: String? = null
    ): GetAtomQualityConfigResult {
        try {
            val qualityJsonStr = getFileStr(
                projectCode = projectCode,
                atomCode = atomCode,
                atomVersion = atomVersion,
                fileName = QUALITY_JSON_NAME,
                repositoryHashId = repositoryHashId,
                branch = branch
            )
            return if (!qualityJsonStr.isNullOrBlank() && JsonSchemaUtil.validateJson(qualityJsonStr)) {
                val qualityDataMap = storeI18nMessageService.parseJsonMapI18nInfo(
                    userId = userId,
                    jsonMap = JsonUtil.toMutableMap(qualityJsonStr),
                    storeI18nConfig = StoreI18nConfig(
                        projectCode = projectCode,
                        storeCode = atomCode,
                        fileDir = "$atomCode/$atomVersion",
                        i18nDir = i18nDir,
                        dbKeyPrefix = StoreUtils.getStoreFieldKeyPrefix(StoreTypeEnum.ATOM, atomCode, atomVersion),
                        repositoryHashId = repositoryHashId,
                        branch = branch
                    ),
                    version = atomVersion
                )
                val indicators = qualityDataMap["indicators"] as Map<String, Any>
                val stageCode = (qualityDataMap["stage"] as String).lowercase(Locale.getDefault())
                if (stageCode !in listOf(DEVELOP, TEST, DEPLOY, SECURITY)) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                        params = arrayOf(stageCode)
                    )
                }

                // 先注册基础数据
                val metadataResultMap = registerMetadata(
                    userId = userId,
                    atomCode = atomCode,
                    atomName = atomName,
                    indicators = indicators,
                    version = if (atomVersion.startsWith("$TEST-$branch-")) atomVersion else null
                )

                // 再注册指标
                registerIndicator(
                    userId = userId,
                    projectId = projectCode,
                    atomCode = atomCode,
                    atomName = atomName,
                    atomVersion = atomVersion,
                    stage = stageCode,
                    metadataResultMap = metadataResultMap,
                    indicators = indicators
                )

                // 最后注册控制点
                registerControlPoint(
                    userId = userId,
                    atomCode = atomCode,
                    atomName = atomName,
                    atomVersion = atomVersion,
                    stage = stageCode,
                    projectId = projectCode
                )

                GetAtomQualityConfigResult("0", arrayOf(""))
            } else {
                val extra = if (atomVersion.startsWith("$TEST-$atomVersion-")) {
                    "$IN_READY_TEST($atomVersion)"
                } else {
                    IN_READY_TEST
                }
                try {
                    client.get(ServiceQualityIndicatorMarketResource::class)
                        .deleteTestIndicator(atomCode, extra)
                    client.get(ServiceQualityMetadataMarketResource::class).deleteTestMetadata(atomCode, extra)
                    client.get(ServiceQualityControlPointMarketResource::class).deleteTestControlPoint(atomCode, extra)
                } catch (ignored: Throwable) {
                    logger.warn("clear atom:$atomCode test quality data fail", ignored)
                }

                GetAtomQualityConfigResult(
                    StoreMessageCode.USER_REPOSITORY_PULL_QUALITY_JSON_FILE_FAIL,
                    arrayOf(branch ?: MASTER, QUALITY_JSON_NAME)
                )
            }
        } catch (ignored: Throwable) {
            logger.error("BKSystemErrorMonitor|getQualityJsonContent|$atomCode|error=${ignored.message}", ignored)
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
        fileName: String,
        repositoryHashId: String? = null,
        branch: String? = null
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
            userId = userId,
            tag =
            if (atomVersion.startsWith("$TEST-$atomVersion-")) "$IN_READY_TEST($atomVersion)" else IN_READY_TEST,
            controlPoint = QualityControlPoint(
                hashId = "",
                type = atomCode,
                name = atomName,
                stage = stage,
                availablePos = listOf(
                    ControlPointPosition.create(BEFORE_POSITION),
                    ControlPointPosition.create(AFTER_POSITION)
                ),
                defaultPos = ControlPointPosition.create(BEFORE_POSITION),
                enable = true,
                atomVersion = atomVersion,
                testProject = projectId
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
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
        val tag =
            if (atomVersion.startsWith("$TEST-$atomVersion-")) "$IN_READY_TEST($atomVersion)" else IN_READY_TEST
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
                tag = tag,
                enable = true,
                type = IndicatorType.MARKET,
                logPrompt = map["logPrompt"] as? String ?: ""
            )
        }
        client.get(ServiceQualityIndicatorMarketResource::class).setTestIndicator(
            userId = userId,
            atomCode = atomCode,
            tag = tag,
            indicatorUpdateList = indicatorsList
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun registerMetadata(
        userId: String,
        atomCode: String,
        atomName: String,
        indicators: Map<String, Any>,
        version: String?
    ): Map<String, Long> {
        val extra = version?.let { "$IN_READY_TEST($version)" } ?: IN_READY_TEST // 标注是正在测试中的
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
                extra = extra
            )
        }
        return client.get(ServiceQualityMetadataMarketResource::class).setTestMetadata(
            userId = userId,
            atomCode = atomCode,
            extra = extra,
            metadataList = metadataList
        ).data ?: mapOf()
    }

    @Suppress("UNCHECKED_CAST")
    protected fun getAtomConfig(
        projectCode: String,
        atomCode: String,
        atomVersion: String,
        userId: String,
        repositoryHashId: String? = null,
        branch: String? = null
    ): GetAtomConfigResult {
        // 拉取task.json配置文件校验其合法性
        val taskJsonStr: String?
        try {
            taskJsonStr = getFileStr(
                projectCode = projectCode,
                atomCode = atomCode,
                atomVersion = atomVersion,
                fileName = TASK_JSON_NAME,
                repositoryHashId = repositoryHashId,
                branch = branch
            )
        } catch (exception: RemoteServiceException) {
            logger.error("BKSystemErrorMonitor|getTaskJsonContent|$atomCode|error=${exception.message}", exception)
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_PULL_FILE_FAIL,
                params = arrayOf(TASK_JSON_NAME, exception.message ?: "")
            )
        } catch (ignored: Throwable) {
            logger.error("BKSystemErrorMonitor|getTaskJsonContent|$atomCode|error=${ignored.message}", ignored)
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_ATOM_CONF_INVALID,
                params = arrayOf(TASK_JSON_NAME, "${ignored.message}")
            )
        }
        if ((null == taskJsonStr) || !JsonSchemaUtil.validateJson(taskJsonStr)) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_REPOSITORY_PULL_TASK_JSON_FILE_FAIL,
                params = arrayOf(branch ?: MASTER, TASK_JSON_NAME)
            )
        }
        return marketAtomCommonService.parseBaseTaskJson(
            taskJsonStr = taskJsonStr,
            projectCode = projectCode,
            atomCode = atomCode,
            version = atomVersion,
            userId = userId
        )
    }

    private fun validateAtomNameIsExist(
        atomCode: String,
        atomName: String
    ): Boolean {
        var flag = false
        val count = atomDao.countByName(dslContext, atomName)
        if (count > 0) {
            // 判断插件名称是否重复（插件升级允许名称一样）
            flag = atomDao.countByName(dslContext = dslContext, name = atomName, atomCode = atomCode) < count
        }
        return flag
    }

    private fun upgradeMarketAtom(
        marketAtomUpdateRequest: MarketAtomUpdateRequest,
        context: DSLContext,
        userId: String,
        atomId: String,
        atomStatus: AtomStatusEnum,
        classType: String,
        props: String,
        atomEnvRequests: List<AtomEnvRequest>,
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
        val atomPackageSourceType = getAtomPackageSourceType(atomRecord.repositoryHashId)
        if (atomPackageSourceType != PackageSourceTypeEnum.UPLOAD) {
            marketAtomEnvInfoDao.addMarketAtomEnvInfo(context, atomId, atomEnvRequests)
        }
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

    /**
     * 获取插件版本发布进度
     */
    override fun getProcessInfo(userId: String, atomId: String): Result<StoreProcessInfo> {
        logger.info("getProcessInfo userId is $userId,atomId is $atomId")
        val record = marketAtomDao.getAtomRecordById(dslContext, atomId)
        return if (null == record) {
            I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomId),
                language = I18nUtil.getLanguage(userId)
            )
        } else {
            val atomCode = record.atomCode
            // 判断用户是否有查询权限
            val queryFlag = storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM.type.toByte()
            )
            if (!queryFlag) {
                throw ErrorCodeException(
                    errorCode = GET_INFO_NO_PERMISSION,
                    params = arrayOf(atomCode)
                )
            }
            val status = record.atomStatus.toInt()
            // 查看当前版本之前的版本是否有已发布的，如果有已发布的版本则只是普通的升级操作而不需要审核
            val isNormalUpgrade = marketAtomCommonService.getNormalUpgradeFlag(atomCode, status)
            val processInfo = handleProcessInfo(
                userId = userId,
                atomId = atomId,
                isNormalUpgrade = isNormalUpgrade,
                status = status
            )
            val storeProcessInfo = storeCommonService.generateStoreProcessInfo(
                userId = userId,
                storeId = atomId,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM,
                creator = record.creator,
                processInfo = processInfo
            )
            Result(storeProcessInfo)
        }
    }

    abstract fun handleProcessInfo(
        userId: String,
        atomId: String,
        isNormalUpgrade: Boolean,
        status: Int
    ): List<ReleaseProcessItem>

    /**
     * 取消发布
     */
    override fun cancelRelease(userId: String, atomId: String): Result<Boolean> {
        logger.info("cancelRelease, userId=$userId, atomId=$atomId")
        val record = marketAtomDao.getAtomRecordById(dslContext, atomId) ?: return Result(true)
        val atomCode = record.atomCode
        val status = AtomStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        val (checkResult, code, params) = checkAtomVersionOptRight(userId, atomId, status)
        if (!checkResult) {
            throw ErrorCodeException(
                errorCode = code,
                params = params
            )
        }
        storeFileService.cleanStoreVersionReferenceFile(atomCode, record.version)
        marketAtomDao.setAtomStatusById(
            dslContext = dslContext,
            atomId = atomId,
            atomStatus = status,
            userId = userId,
            msg = I18nUtil.getCodeLanMessage(UN_RELEASE)
        )
        // 更新插件当前大版本内是否有测试版本标识
        redisOperation.hset(
            key = "$ATOM_POST_VERSION_TEST_FLAG_KEY_PREFIX:$atomCode",
            hashKey = VersionUtils.convertLatestVersion(record.version),
            values = "false"
        )
        checkUpdateAtomLatestTestFlag(userId, atomCode, atomId)
        doCancelReleaseBus(userId, atomId)
        // 通过websocket推送状态变更消息
        storeWebsocketService.sendWebsocketMessage(userId, atomId)
        // 删除质量红线相关数据
        client.get(ServiceQualityIndicatorMarketResource::class).deleteTestIndicator(atomCode, IN_READY_TEST)
        client.get(ServiceQualityMetadataMarketResource::class).deleteTestMetadata(atomCode, IN_READY_TEST)
        client.get(ServiceQualityControlPointMarketResource::class).deleteTestControlPoint(atomCode, IN_READY_TEST)
        return Result(true)
    }

    abstract fun doCancelReleaseBus(userId: String, atomId: String)

    abstract fun getPreValidatePassTestStatus(atomCode: String, atomId: String, atomStatus: Byte): Byte

    /**
     * 通过测试
     */
    @BkTimed(extraTags = ["publish", "passTest"], value = "store_publish_pipeline_atom")
    override fun passTest(userId: String, atomId: String): Result<Boolean> {
        logger.info("passTest, userId=$userId, atomId=$atomId")
        val atomRecord = marketAtomDao.getAtomRecordById(dslContext, atomId)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomId),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        val atomCode = atomRecord.atomCode
        // 查看当前版本之前的版本是否有已发布的，如果有已发布的版本则只是普通的升级操作而不需要审核
        val isNormalUpgrade = marketAtomCommonService.getNormalUpgradeFlag(atomCode, atomRecord.atomStatus.toInt())
        logger.info("passTest isNormalUpgrade is:$isNormalUpgrade")
        val atomStatus = getPreValidatePassTestStatus(atomCode, atomId, atomRecord.atomStatus)
        val (checkResult, code, params) = checkAtomVersionOptRight(
            userId = userId,
            atomId = atomId,
            status = atomStatus,
            isNormalUpgrade = isNormalUpgrade
        )
        if (!checkResult) {
            throw ErrorCodeException(errorCode = code, params = params)
        }
        val version = atomRecord.version
        val releaseFlag = atomStatus == AtomStatusEnum.RELEASED.status.toByte()
        val atomReleaseRecord = marketAtomVersionLogDao.getAtomVersion(dslContext, atomId)
        return handleAtomRelease(
            userId = userId,
            releaseFlag = releaseFlag,
            atomReleaseRequest = AtomReleaseRequest(
                atomId = atomId,
                atomCode = atomCode,
                version = version,
                atomStatus = atomStatus,
                releaseType = ReleaseTypeEnum.getReleaseTypeObj(atomReleaseRecord.releaseType.toInt())!!,
                repositoryHashId = atomRecord.repositoryHashId,
                branch = atomRecord.branch,
                atomName = atomRecord.name
            )
        )
    }

    @BkTimed(extraTags = ["publish", "handleAtomRelease"], value = "store_publish_pipeline_atom")
    override fun handleAtomRelease(
        userId: String,
        releaseFlag: Boolean,
        atomReleaseRequest: AtomReleaseRequest
    ): Result<Boolean> {
        val atomId = atomReleaseRequest.atomId
        val atomCode = atomReleaseRequest.atomCode
        val atomStatus = atomReleaseRequest.atomStatus
        val atomName = atomReleaseRequest.atomName
        if (releaseFlag) {
            storeFileService.cleanStoreVersionReferenceFile(atomCode, atomReleaseRequest.version)
            // 处理插件发布逻辑
            doAtomReleaseBus(userId, atomReleaseRequest)
            // 更新质量红线信息
            atomQualityService.updateQualityInApprove(atomCode, atomStatus)
            checkUpdateAtomLatestTestFlag(userId, atomCode, atomId)
            dslContext.transaction { t ->
                val context = DSL.using(t)
                // 记录发布信息
                val pubTime = LocalDateTime.now()
                storeReleaseDao.addStoreReleaseInfo(
                    dslContext = context,
                    userId = userId,
                    storeReleaseCreateRequest = StoreReleaseCreateRequest(
                        storeCode = atomCode,
                        storeType = StoreTypeEnum.ATOM,
                        latestUpgrader = atomReleaseRequest.publisher ?: userId,
                        latestUpgradeTime = pubTime
                    )
                )
                val newestVersionFlag = atomDao.getLatestAtomByCode(context, atomCode)?.let {
                    StoreUtils.isGreaterVersion(atomReleaseRequest.version, it.version)
                } ?: true
                val releaseType = atomReleaseRequest.releaseType
                val latestFlag = if (releaseType == ReleaseTypeEnum.HIS_VERSION_UPGRADE && !newestVersionFlag) {
                    // 历史大版本下的小版本更新不把latestFlag置为true（利用这种发布方式发布最新版本除外）
                    null
                } else {
                    // 清空旧版本LATEST_FLAG
                    marketAtomDao.cleanLatestFlag(context, atomCode)
                    true
                }
                marketAtomDao.updateAtomInfoById(
                    dslContext = context,
                    userId = userId,
                    atomId = atomId,
                    updateAtomInfo = UpdateAtomInfo(
                        atomStatus = atomStatus,
                        latestFlag = latestFlag,
                        pubTime = pubTime
                    )
                )
                // 处理插件缓存
                marketAtomCommonService.handleAtomCache(
                    atomId = atomId,
                    atomCode = atomCode,
                    version = atomReleaseRequest.version,
                    releaseFlag = true
                )
                // 计算插件指标数据
                atomIndexTriggerCalService.upgradeTriggerCalculate(
                    userId = userId,
                    atomCode = atomCode,
                    version = atomReleaseRequest.version,
                    releaseType = releaseType
                )
                // 通过websocket推送状态变更消息
                storeWebsocketService.sendWebsocketMessage(userId, atomId)
            }
            // 发送版本发布邮件
            atomNotifyService.sendAtomReleaseAuditNotifyMessage(atomId, AuditTypeEnum.AUDIT_SUCCESS)
        } else {
            marketAtomDao.setAtomStatusById(
                dslContext = dslContext,
                atomId = atomId,
                atomStatus = atomStatus,
                userId = userId,
                msg = ""
            )
            // 通过websocket推送状态变更消息
            storeWebsocketService.sendWebsocketMessage(userId, atomId)
            // 研发商店插件上架进入审核阶段时通知管理员审批
            if (atomName !== null && !defaultAtomPublishReviewers.isNullOrBlank()) {
                sendPendingReview(
                    userId = userId,
                    atomName = atomName,
                    version = atomReleaseRequest.version,
                    atomId = atomId
                )
            }
        }
        return Result(true)
    }

    abstract fun doAtomReleaseBus(
        userId: String,
        atomReleaseRequest: AtomReleaseRequest
    )

    /**
     * 检查版本发布过程中的操作权限
     */
    abstract fun checkAtomVersionOptRight(
        userId: String,
        atomId: String,
        status: Byte,
        isNormalUpgrade: Boolean? = null
    ): Triple<Boolean, String, Array<String>?>

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
            throw ErrorCodeException(errorCode = NO_COMPONENT_ADMIN_PERMISSION)
        }
        val version = atomOfflineReq.version
        val reason = atomOfflineReq.reason
        if (!version.isNullOrEmpty()) {
            offlineAtomByVersion(atomCode = atomCode, version = version, userId = userId, reason = reason)
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

    private fun offlineAtomByVersion(
        atomCode: String,
        version: String,
        userId: String,
        reason: String?
    ) {
        val atomRecord = atomDao.getPipelineAtom(dslContext, atomCode, version.trim())
            ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("$atomCode:$version")
            )
        if (AtomStatusEnum.RELEASED.status.toByte() != atomRecord.atomStatus) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_CLIENT_REST_ERROR)
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            handleOfflineAtomByVersion(
                context = context,
                atomCode = atomCode,
                atomId = atomRecord.id,
                userId = userId,
                reason = reason
            )
        }
    }

    private fun handleOfflineAtomByVersion(
        context: DSLContext,
        atomCode: String,
        atomId: String,
        userId: String,
        reason: String?
    ) {
        marketAtomDao.updateAtomInfoById(
            dslContext = context,
            atomId = atomId,
            userId = userId,
            updateAtomInfo = UpdateAtomInfo(
                atomStatus = AtomStatusEnum.UNDERCARRIAGED.status.toByte(),
                atomStatusMsg = reason,
                latestFlag = false
            )
        )
        redisOperation.delete(StoreUtils.getStoreRunInfoKey(StoreTypeEnum.ATOM.name, atomCode))
        // 获取插件已发布版本数量
        val releaseCount = marketAtomDao.countReleaseAtomByCode(context, atomCode)
        val tmpAtomId = if (releaseCount > 0) {
            // 获取已发布最大版本的插件记录
            val maxReleaseVersionRecord = marketAtomDao.getMaxVersionAtomByCode(
                dslContext = context,
                atomCode = atomCode,
                atomStatus = AtomStatusEnum.RELEASED
            )
            maxReleaseVersionRecord?.let {
                // 处理插件缓存(保证用户用到当前大版本中已发布的版本)
                marketAtomCommonService.handleAtomCache(
                    atomId = maxReleaseVersionRecord.id,
                    atomCode = atomCode,
                    version = maxReleaseVersionRecord.version,
                    releaseFlag = false
                )
            }
            maxReleaseVersionRecord?.id
        } else {
            // 获取已下架最大版本的插件记录
            val maxUndercarriagedVersionRecord = marketAtomDao.getMaxVersionAtomByCode(
                dslContext = context,
                atomCode = atomCode,
                atomStatus = AtomStatusEnum.UNDERCARRIAGED
            )
            maxUndercarriagedVersionRecord?.id
        }
        if (null != tmpAtomId) {
            marketAtomDao.cleanLatestFlag(context, atomCode)
            marketAtomDao.updateAtomInfoById(
                dslContext = context,
                atomId = tmpAtomId,
                userId = userId,
                updateAtomInfo = UpdateAtomInfo(
                    latestFlag = true
                )
            )
        }
    }

    protected fun updateAtomVersionInfo(
        userId: String,
        projectCode: String,
        newVersionFlag: Boolean,
        updateAtomPackageInfo: UpdateAtomPackageInfo,
        convertUpdateRequest: MarketAtomUpdateRequest,
        getAtomConfResult: GetAtomConfigResult
    ): Result<String> {
        val atomId = updateAtomPackageInfo.atomId
        val atomCode = convertUpdateRequest.atomCode
        val version = convertUpdateRequest.version
        val branch = convertUpdateRequest.branch
        val atomRecord = atomDao.getMaxVersionAtomByCode(dslContext, atomCode)!!
        val releaseType = convertUpdateRequest.releaseType
        val taskDataMap = storeI18nMessageService.parseJsonMapI18nInfo(
            userId = userId,
            jsonMap = getAtomConfResult.taskDataMap.toMutableMap(),
            storeI18nConfig = StoreI18nConfig(
                projectCode = projectCode,
                storeCode = atomCode,
                fileDir = "$atomCode/$version",
                i18nDir = updateAtomPackageInfo.i18nDir,
                dbKeyPrefix = StoreUtils.getStoreFieldKeyPrefix(StoreTypeEnum.ATOM, atomCode, version),
                repositoryHashId = atomRecord.repositoryHashId,
                branch = branch
            ),
            version = version
        )
        if (!convertUpdateRequest.isBranchTestVersion) {
            // 校验插件发布类型
            marketAtomCommonService.validateReleaseType(
                atomId = atomRecord.id,
                atomCode = atomCode,
                version = version,
                releaseType = releaseType,
                taskDataMap = taskDataMap,
                fieldCheckConfirmFlag = convertUpdateRequest.fieldCheckConfirmFlag
            )
        }
        val validateResult = validateUpdateMarketAtomReq(userId, convertUpdateRequest, atomRecord.repositoryHashId)
        logger.info("validateUpdateMarketAtomReq validateResult is :$validateResult")
        if (validateResult.isNotOk()) {
            return Result(validateResult.status, validateResult.message, null)
        }

        // 解析quality.json
        val getAtomQualityResult = getAtomQualityConfig(
            projectCode = projectCode,
            atomCode = atomCode,
            atomName = convertUpdateRequest.name,
            atomVersion = version,
            userId = userId,
            i18nDir = updateAtomPackageInfo.i18nDir,
            repositoryHashId = atomRecord.repositoryHashId,
            branch = branch
        )
        logger.info("update market atom, getAtomQualityResult: $getAtomQualityResult")
        if (getAtomQualityResult.errorCode == StoreMessageCode.USER_REPOSITORY_PULL_QUALITY_JSON_FILE_FAIL) {
            logger.info("quality.json not found , skip...")
        } else if (getAtomQualityResult.errorCode != "0") {
            return I18nUtil.generateResponseDataObject(
                messageCode = getAtomQualityResult.errorCode,
                params = getAtomQualityResult.errorParams,
                language = I18nUtil.getLanguage(userId)
            )
        }

        val atomEnvRequests = getAtomConfResult.atomEnvRequests ?: return I18nUtil.generateResponseDataObject(
            messageCode = StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_NULL,
            params = arrayOf(KEY_EXECUTION),
            language = I18nUtil.getLanguage(userId)
        )
        val packagePath = updateAtomPackageInfo.packagePath
        val atomPackageSourceType = updateAtomPackageInfo.atomPackageSourceType
        val classType = if (packagePath.isNullOrBlank() && atomPackageSourceType == PackageSourceTypeEnum.UPLOAD) {
            // 没有可执行文件的插件是老的内置插件，插件的classType为插件标识
            atomCode
        } else if (convertUpdateRequest.os.isEmpty()) {
            MarketBuildLessAtomElement.classType
        } else {
            MarketBuildAtomElement.classType
        }
        val propsMap = mutableMapOf<String, Any?>()
        val inputDataMap = taskDataMap[KEY_INPUT] as? Map<String, Any>
        if (convertUpdateRequest.frontendType == FrontendTypeEnum.HISTORY) {
            inputDataMap?.let { propsMap.putAll(inputDataMap) }
        } else {
            propsMap[KEY_INPUT_GROUPS] = taskDataMap[KEY_INPUT_GROUPS]
            propsMap[KEY_INPUT] = inputDataMap
            propsMap[KEY_OUTPUT] = taskDataMap[KEY_OUTPUT]
            propsMap[KEY_CONFIG] = taskDataMap[KEY_CONFIG]
        }
        convertUpdateRequest.os.sort() // 给操作系统排序
        val atomStatus =
            if (atomPackageSourceType == PackageSourceTypeEnum.REPO) {
                AtomStatusEnum.COMMITTING
            } else AtomStatusEnum.TESTING
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val props = JsonUtil.toJson(propsMap, formatted = false)
            if (!newVersionFlag) {
                updateMarketAtom(
                    context = context,
                    userId = userId,
                    atomId = atomId,
                    atomStatus = atomStatus,
                    classType = classType,
                    props = props,
                    releaseType = releaseType.releaseType.toByte(),
                    marketAtomUpdateRequest = convertUpdateRequest,
                    atomEnvRequests = atomEnvRequests,
                    repositoryHashId = atomRecord.repositoryHashId
                )
            } else {
                // 升级插件
                upgradeMarketAtom(
                    marketAtomUpdateRequest = convertUpdateRequest,
                    context = context,
                    userId = userId,
                    atomId = atomId,
                    atomStatus = atomStatus,
                    classType = classType,
                    props = props,
                    atomEnvRequests = atomEnvRequests,
                    atomRecord = atomRecord
                )
            }
            if (!convertUpdateRequest.isBranchTestVersion && atomStatus == AtomStatusEnum.TESTING) {
                // 插件大版本内有测试版本则写入缓存
                redisOperation.hset(
                    key = "$ATOM_POST_VERSION_TEST_FLAG_KEY_PREFIX:$atomCode",
                    hashKey = VersionUtils.convertLatestVersion(version),
                    values = "true"
                )
            }
            // 更新标签信息
            val labelIdList = convertUpdateRequest.labelIdList?.filter { !it.isNullOrBlank() }
            if (!convertUpdateRequest.isBranchTestVersion && null != labelIdList) {
                atomLabelRelDao.deleteByAtomId(context, atomId)
                if (labelIdList.isNotEmpty()) {
                    atomLabelRelDao.batchAdd(context, userId = userId, atomId = atomId, labelIdList = labelIdList)
                }
            }

            // 更新红线标识
            val qualityFlag = getAtomQualityResult.errorCode == "0"
            marketAtomFeatureDao.updateAtomFeature(
                dslContext = context,
                userId = userId,
                atomFeatureRequest = AtomFeatureRequest(atomCode = atomCode, qualityFlag = qualityFlag)
            )
            asyncHandleUpdateAtom(
                context = context,
                atomId = atomId,
                userId = userId,
                branch = branch,
                validOsNameFlag = marketAtomCommonService.getValidOsNameFlag(atomEnvRequests),
                validOsArchFlag = marketAtomCommonService.getValidOsArchFlag(atomEnvRequests)
            )
        }
        return Result(atomId)
    }

    fun checkUpdateAtomLatestTestFlag(userId: String, atomCode: String, atomId: String) {
        RedisLock(
            redisOperation,
            "$STORE_LATEST_TEST_FLAG_KEY_PREFIX:$atomCode",
            60L
        ).use { redisLock ->
            redisLock.lock()
            if (marketAtomDao.isAtomLatestTestVersion(dslContext, atomId) > 0) {
                val latestTestVersionId = marketAtomDao.queryAtomLatestTestVersionId(dslContext, atomCode, atomId)
                latestTestVersionId?.let {
                    updateAtomLatestTestFlag(
                        userId = userId,
                        atomCode = atomCode,
                        atomId = it
                    )
                }
            }
        }
    }

    override fun updateAtomLatestTestFlag(userId: String, atomCode: String, atomId: String) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            marketAtomDao.resetAtomLatestTestFlagByCode(
                dslContext = transactionContext,
                atomCode = atomCode
            )
            marketAtomDao.setupAtomLatestTestFlagById(
                dslContext = transactionContext,
                atomId = atomId,
                userId = userId,
                latestFlag = true
            )
        }
    }

    private fun sendPendingReview(userId: String, atomName: String, version: String, atomId: String) {
        val atomReleaseStatusUrl = "${config.devopsHostGateway}/console/store/releaseProgress/upgrade/%s"
        val bodyParams = mapOf(
            "userId" to userId,
            "atomName" to atomName,
            "version" to version,
            "url" to String.format(atomReleaseStatusUrl, atomId)
        )

        val receivers = defaultAtomPublishReviewers!!
            .split(",")
            .map { it.trim() }
            .toMutableSet()

        val request = SendNotifyMessageTemplateRequest(
            templateCode = "BK_STORE_ATOM_AUDIT_NOTIFY",
            receivers = receivers,
            bodyParams = bodyParams,
            notifyType = mutableSetOf(NotifyType.WEWORK.name)
        )

        try {
            client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
        } catch (ignored: Throwable) {
            logger.warn("Failed to send notify message", ignored)
        }
    }
}
