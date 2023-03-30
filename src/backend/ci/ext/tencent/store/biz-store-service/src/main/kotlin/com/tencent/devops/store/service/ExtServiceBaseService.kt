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

package com.tencent.devops.store.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.api.ServiceArchiveStoreFileResource
import com.tencent.devops.common.api.constant.*
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.archive.config.BkRepoConfig
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.BSProjectServiceCodec
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.api.pojo.ServiceItem
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.service.ServiceItemResource
import com.tencent.devops.project.pojo.ITEM_BK_SERVICE_REDIS_KEY
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.config.ExtServiceBcsNameSpaceConfig
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.ExtItemServiceDao
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceEnvDao
import com.tencent.devops.store.dao.ExtServiceFeatureDao
import com.tencent.devops.store.dao.ExtServiceItemRelDao
import com.tencent.devops.store.dao.ExtServiceLableRelDao
import com.tencent.devops.store.dao.ExtServiceVersionLogDao
import com.tencent.devops.store.dao.common.StoreBuildInfoDao
import com.tencent.devops.store.dao.common.StoreMediaInfoDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreStatisticTotalDao
import com.tencent.devops.store.pojo.EditInfoDTO
import com.tencent.devops.store.pojo.ExtServiceCreateInfo
import com.tencent.devops.store.pojo.ExtServiceEnvCreateInfo
import com.tencent.devops.store.pojo.ExtServiceFeatureCreateInfo
import com.tencent.devops.store.pojo.ExtServiceFeatureUpdateInfo
import com.tencent.devops.store.pojo.ExtServiceItemRelCreateInfo
import com.tencent.devops.store.pojo.ExtServiceUpdateInfo
import com.tencent.devops.store.pojo.ExtServiceVersionLogCreateInfo
import com.tencent.devops.store.pojo.ExtensionJson
import com.tencent.devops.store.pojo.ItemPropCreateInfo
import com.tencent.devops.store.pojo.common.EXTENSION_JSON_NAME
import com.tencent.devops.store.pojo.common.KEY_LABEL_CODE
import com.tencent.devops.store.pojo.common.KEY_LABEL_ID
import com.tencent.devops.store.pojo.common.KEY_LABEL_NAME
import com.tencent.devops.store.pojo.common.KEY_LABEL_TYPE
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.StoreMediaInfoRequest
import com.tencent.devops.store.pojo.common.StoreProcessInfo
import com.tencent.devops.store.pojo.common.UN_RELEASE
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.dto.ExtSubmitDTO
import com.tencent.devops.store.pojo.dto.InitExtServiceDTO
import com.tencent.devops.store.pojo.dto.ServiceOfflineDTO
import com.tencent.devops.store.pojo.dto.SubmitDTO
import com.tencent.devops.store.pojo.enums.DescInputTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServicePackageSourceTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import com.tencent.devops.store.pojo.vo.ExtServiceRespItem
import com.tencent.devops.store.pojo.vo.MyServiceVO
import com.tencent.devops.store.pojo.vo.ServiceVersionListItem
import com.tencent.devops.store.pojo.vo.ServiceVersionVO
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.common.StoreMediaService
import com.tencent.devops.store.service.common.StoreUserService
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import com.tencent.devops.store.utils.VersionUtils
import okhttp3.Request
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.time.LocalDateTime
import java.util.regex.Pattern

@Service
abstract class ExtServiceBaseService @Autowired constructor() {

    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var extServiceDao: ExtServiceDao
    @Autowired
    lateinit var storeProjectRelDao: StoreProjectRelDao
    @Autowired
    lateinit var extServiceEnvDao: ExtServiceEnvDao
    @Autowired
    lateinit var storeBuildInfoDao: StoreBuildInfoDao
    @Autowired
    lateinit var storeMemberDao: StoreMemberDao
    @Autowired
    lateinit var extFeatureDao: ExtServiceFeatureDao
    @Autowired
    lateinit var extServiceItemRelDao: ExtServiceItemRelDao
    @Autowired
    lateinit var extItemServiceDao: ExtItemServiceDao
    @Autowired
    lateinit var extServiceCommonService: ExtServiceCommonService
    @Autowired
    lateinit var storeCommonService: StoreCommonService
    @Autowired
    lateinit var extServiceVersionLogDao: ExtServiceVersionLogDao
    @Autowired
    lateinit var extServiceLabelDao: ExtServiceLableRelDao
    @Autowired
    lateinit var extServiceFeatureDao: ExtServiceFeatureDao
    @Autowired
    lateinit var client: Client
    @Autowired
    lateinit var storeUserService: StoreUserService
    @Autowired
    lateinit var storeCommentService: StoreCommentService
    @Autowired
    lateinit var mediaService: StoreMediaService
    @Autowired
    lateinit var deptService: StoreVisibleDeptService
    @Autowired
    lateinit var objectMapper: ObjectMapper
    @Autowired
    lateinit var extServiceBcsService: ExtServiceBcsService
    @Autowired
    lateinit var extServiceBcsNameSpaceConfig: ExtServiceBcsNameSpaceConfig
    @Autowired
    lateinit var permissionApi: AuthPermissionApi
    @Autowired
    lateinit var storeMediaInfoDao: StoreMediaInfoDao
    @Autowired
    lateinit var storeStatisticTotalDao: StoreStatisticTotalDao
    @Autowired
    lateinit var bsProjectServiceCodec: BSProjectServiceCodec
    @Autowired
    lateinit var redisOperation: RedisOperation
    @Autowired
    private lateinit var bkRepoConfig: BkRepoConfig

    fun addExtService(
        userId: String,
        extensionInfo: InitExtServiceDTO
    ): Result<Boolean> {
        val serviceCode = extensionInfo.serviceCode
        logger.info("addExtService user[$userId], serviceCode[$serviceCode], info[$extensionInfo]")
        // 校验信息
        validateAddServiceReq(userId, extensionInfo)
        checkProjectInfo(userId, extensionInfo.projectCode)
        val handleServicePackageResult = handleServicePackage(extensionInfo, userId, serviceCode)
        logger.info("addExtService the handleServicePackage is :$handleServicePackageResult")

        if (handleServicePackageResult.isNotOk()) {
            return Result(handleServicePackageResult.status, handleServicePackageResult.message, null)
        }
        val handleServicePackageMap = handleServicePackageResult.data
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val id = UUIDUtil.generate()
            // 添加扩展服务基本信息
            extServiceDao.createExtService(
                dslContext = context,
                userId = userId,
                id = id,
                extServiceCreateInfo = ExtServiceCreateInfo(
                    serviceCode = extensionInfo.serviceCode,
                    serviceName = extensionInfo.serviceName,
                    latestFlag = true,
                    creatorUser = userId,
                    publisher = userId,
                    publishTime = System.currentTimeMillis(),
                    status = ExtServiceStatusEnum.INIT.status,
                    version = ""
                )
            )
            // 添加扩展服务与项目关联关系，type为0代表新增扩展服务时关联的初始化项目
            storeProjectRelDao.addStoreProjectRel(
                dslContext = context,
                userId = userId,
                storeCode = serviceCode,
                projectCode = extensionInfo.projectCode,
                type = StoreProjectTypeEnum.INIT.type.toByte(),
                storeType = StoreTypeEnum.SERVICE.type.toByte()
            )
            storeProjectRelDao.addStoreProjectRel(
                dslContext = context,
                userId = userId,
                storeCode = serviceCode,
                projectCode = extensionInfo.projectCode,
                type = StoreProjectTypeEnum.TEST.type.toByte(),
                storeType = StoreTypeEnum.SERVICE.type.toByte()
            )
            val extServiceEnvCreateInfo = ExtServiceEnvCreateInfo(
                serviceId = id,
                language = extensionInfo.language,
                pkgPath = "",
                pkgShaContent = "",
                dockerFileContent = "",
                imagePath = "",
                creatorUser = userId,
                modifierUser = userId
            )
            extServiceEnvDao.create(context, extServiceEnvCreateInfo) // 添加扩展服务执行环境信息
            // 默认给新建扩展服务的人赋予管理员权限
            storeMemberDao.addStoreMember(
                dslContext = context,
                userId = userId,
                storeCode = serviceCode,
                userName = userId,
                type = StoreMemberTypeEnum.ADMIN.type.toByte(),
                storeType = StoreTypeEnum.SERVICE.type.toByte()
            )
            // 添加扩展服务特性信息
            extFeatureDao.create(
                dslContext = context,
                userId = userId,
                extServiceFeatureCreateInfo = ExtServiceFeatureCreateInfo(
                    serviceCode = serviceCode,
                    repositoryHashId = handleServicePackageMap?.get("repositoryHashId") ?: "",
                    codeSrc = handleServicePackageMap?.get("codeSrc") ?: "",
                    creatorUser = userId,
                    modifierUser = userId,
                    visibilityLevel = extensionInfo.visibilityLevel!!.level
                )
            )
            // 初始化统计表数据
            storeStatisticTotalDao.initStatisticData(
                dslContext = context,
                storeCode = serviceCode,
                storeType = StoreTypeEnum.SERVICE.type.toByte()
            )
            extensionInfo.extensionItemList.forEach {
                // 添加扩展服务扩展点
                extServiceItemRelDao.create(
                    dslContext = dslContext,
                    userId = userId,
                    extServiceItemRelCreateInfo = ExtServiceItemRelCreateInfo(
                        serviceId = id,
                        itemId = it,
                        creatorUser = userId,
                        modifierUser = userId,
                        bkServiceId = getItemBkServiceId(it)
                    )
                )
            }
            // 添加扩展点使用记录
            client.get(ServiceItemResource::class).addServiceNum(extensionInfo.extensionItemList)
        }
        return Result(true)
    }

    fun submitExtService(
        userId: String,
        submitDTO: SubmitDTO
    ): Result<String> {
        logger.info("updateExtService userId[$userId],submitDTO[$submitDTO]")
        val serviceCode = submitDTO.serviceCode
        val extPackageSourceType = getExtServicePackageSourceType(serviceCode)
        logger.info("updateExtService servicePackageSourceType is :$extPackageSourceType")
        val version = submitDTO.version

        // 判断扩展服务是不是首次创建版本
        val serviceCount = extServiceDao.countByCode(dslContext, serviceCode)
        if (serviceCount < 1) {
            return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(serviceCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val serviceRecord = extServiceDao.getNewestServiceByCode(dslContext, serviceCode)!!
        // 判断更新的扩展服务名称是否重复
        if (validateAddServiceReqByName(
                submitDTO.serviceName,
                submitDTO.serviceCode
            )
        ) return MessageUtil.generateResponseDataObject(
            messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
            params = arrayOf(submitDTO.serviceName),
            language = I18nUtil.getLanguage(userId)
        )
        // 校验前端传的版本号是否正确
        val releaseType = submitDTO.releaseType
        val dbVersion = serviceRecord.version
        // 最近的版本处于上架中止状态，重新升级版本号不变
        val cancelFlag = serviceRecord.serviceStatus == ExtServiceStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        val requireVersionList =
            if (cancelFlag && releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE) {
                listOf(dbVersion)
            } else {
                // 历史大版本下的小版本更新模式需获取要更新大版本下的最新版本
                val reqVersion = if (releaseType == ReleaseTypeEnum.HIS_VERSION_UPGRADE) {
                    extServiceDao.getExtService(
                        dslContext = dslContext,
                        serviceCode = serviceCode,
                        version = VersionUtils.convertLatestVersion(version)
                    )?.version
                } else {
                    null
                }
                storeCommonService.getRequireVersion(
                    reqVersion = reqVersion,
                    dbVersion = dbVersion,
                    releaseType = releaseType!!
                )
            }

        if (!requireVersionList.contains(version)) {
            return MessageUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_SERVICE_VERSION_IS_INVALID,
                params = arrayOf(version, requireVersionList.toString()),
                language = I18nUtil.getLanguage(userId)
            )
        }

        // 判断最近一个扩展服务版本的状态，只有处于审核驳回、已发布、上架中止和已下架的状态才允许添加新的版本
        val serviceFinalStatusList = mutableListOf(
            ExtServiceStatusEnum.AUDIT_REJECT.status.toByte(),
            ExtServiceStatusEnum.RELEASED.status.toByte(),
            ExtServiceStatusEnum.GROUNDING_SUSPENSION.status.toByte(),
            ExtServiceStatusEnum.UNDERCARRIAGED.status.toByte()
        )

        if (serviceCount == 1) {
            // 如果是首次发布，处于初始化的扩展服务状态也允许添加新的版本
            serviceFinalStatusList.add(ExtServiceStatusEnum.INIT.status.toByte())
        }

        if (!serviceFinalStatusList.contains(serviceRecord.serviceStatus)) {
            return MessageUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_SERVICE_VERSION_IS_NOT_FINISH,
                params = arrayOf(serviceRecord.serviceName, serviceRecord.version),
                language = I18nUtil.getLanguage(userId)
            )
        }

        var serviceId = UUIDUtil.generate()
        val serviceStatus =
            if (extPackageSourceType == ExtServicePackageSourceTypeEnum.REPO) ExtServiceStatusEnum.COMMITTING else ExtServiceStatusEnum.TESTING

        dslContext.transaction { t ->
            val context = DSL.using(t)
            logger.info("updateExtService cancelFlag[$cancelFlag] releaseType[$releaseType] version[${serviceRecord.version}]")
            if (StringUtils.isEmpty(serviceRecord.version) || (cancelFlag && releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE)) {
                // 首次创建版本或者取消发布后不变更版本号重新上架，则在该版本的记录上做更新操作
                serviceId = serviceRecord.id
                val finalReleaseType = if (releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE) {
                    val serviceVersion = extServiceVersionLogDao.getVersionLogByServiceId(context, serviceId)
                    serviceVersion!!.releaseType
                } else {
                    releaseType.releaseType.toByte()
                }
                submitExtService(
                    userId = userId,
                    serviceId = serviceId,
                    extServiceUpdateInfo = ExtServiceUpdateInfo(
                        serviceName = submitDTO.serviceName,
                        version = submitDTO.version,
                        status = serviceStatus.status,
                        statusMsg = "",
                        logoUrl = submitDTO.logoUrl,
                        iconData = submitDTO.iconData,
                        summary = submitDTO.summary,
                        description = submitDTO.description,
                        latestFlag = null,
                        modifierUser = userId
                    ),
                    extServiceVersionLogCreateInfo = ExtServiceVersionLogCreateInfo(
                        serviceId = serviceId,
                        releaseType = finalReleaseType,
                        content = submitDTO.versionContent ?: "",
                        creatorUser = userId,
                        modifierUser = userId
                    )
                )
            } else {
                // 升级扩展服务
                val serviceEnvRecord = extServiceEnvDao.getMarketServiceEnvInfoByServiceId(context, serviceRecord.id)
                // 若无已发布的扩展，则直接将当前的设置为latest
                val latestFlag = extServiceDao.getLatestFlag(context, serviceCode)
                if (latestFlag) {
                    extServiceDao.cleanLatestFlag(context, serviceCode)
                }
                upgradeMarketExtService(
                    context = context,
                    userId = userId,
                    serviceId = serviceId,
                    language = serviceEnvRecord!!.language,
                    extServiceCreateInfo = ExtServiceCreateInfo(
                        serviceCode = submitDTO.serviceCode,
                        serviceName = submitDTO.serviceName,
                        creatorUser = userId,
                        version = submitDTO.version,
                        logoUrl = submitDTO.logoUrl,
                        iconData = submitDTO.iconData,
                        latestFlag = latestFlag,
                        summary = submitDTO.summary,
                        description = submitDTO.description,
                        publisher = userId,
                        publishTime = System.currentTimeMillis(),
                        status = 0
                    ),
                    extServiceVersionLogCreateInfo = ExtServiceVersionLogCreateInfo(
                        serviceId = serviceId,
                        releaseType = submitDTO.releaseType!!.releaseType.toByte(),
                        content = submitDTO.versionContent ?: "",
                        creatorUser = userId,
                        modifierUser = userId
                    )
                )
            }
            if (submitDTO.descInputType != null) {
                val extServiceFeatureUpdateInfo = ExtServiceFeatureUpdateInfo(
                    descInputType = submitDTO.descInputType
                )
                extFeatureDao.updateExtServiceFeatureBaseInfo(
                    dslContext,
                    userId,
                    serviceCode,
                    extServiceFeatureUpdateInfo
                )
            }

            // 更新标签信息
            val labelIdList = submitDTO.labelIdList
            if (null != labelIdList) {
                extServiceLabelDao.deleteByServiceId(context, serviceId)
                if (labelIdList.isNotEmpty())
                    extServiceLabelDao.batchAdd(context, userId, serviceId, labelIdList)
            }

            // 添加扩展点
            val featureInfoRecord = extFeatureDao.getLatestServiceByCode(dslContext, serviceCode)
            val itemIdList = submitDTO.extensionItemList
            val itemCreateInfoList =
                getFileServiceProps(serviceCode, featureInfoRecord!!.repositoryHashId, EXTENSION_JSON_NAME, itemIdList)
            extServiceItemRelDao.deleteByServiceId(context, serviceId)
            extServiceItemRelDao.batchAdd(
                dslContext = dslContext,
                userId = userId,
                serviceId = serviceId,
                itemPropList = itemCreateInfoList
            )
            // 添加扩展点使用记录
            client.get(ServiceItemResource::class).addServiceNum(itemIdList)

            asyncHandleUpdateService(context, serviceId, userId)
        }
        return Result(serviceId)
    }

    /**
     * 异步处理上架扩展服务
     */
    abstract fun asyncHandleUpdateService(
        context: DSLContext,
        serviceId: String,
        userId: String
    )

    fun getExtensionServiceInfo(userId: String, serviceId: String): Result<StoreProcessInfo> {
        logger.info("getProcessInfo userId is $userId,serviceId is $serviceId")
        val record = extServiceDao.getServiceById(dslContext, serviceId)
        return if (null == record) {
            MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(serviceId),
                language = I18nUtil.getLanguage(userId)
            )
        } else {
            val serviceCode = record.serviceCode
            // 判断用户是否有查询权限
            val queryFlag =
                storeMemberDao.isStoreMember(dslContext, userId, serviceCode, StoreTypeEnum.SERVICE.type.toByte())
            if (!queryFlag) {
                return MessageUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PERMISSION_DENIED,
                    language = I18nUtil.getLanguage(userId))
            }
            val status = record.serviceStatus.toInt()
            // 查看当前版本之前的版本是否有已发布的，如果有已发布的版本则只是普通的升级操作而不需要审核
            val isNormalUpgrade = getNormalUpgradeFlag(serviceCode, status)
            val processInfo = handleProcessInfo(isNormalUpgrade, status)
            val storeProcessInfo = storeCommonService.generateStoreProcessInfo(
                userId = userId,
                storeId = serviceId,
                storeCode = serviceCode,
                storeType = StoreTypeEnum.SERVICE,
                creator = record.creator,
                processInfo = processInfo
            )
            logger.info("getProcessInfo storeProcessInfo is $storeProcessInfo")
            Result(storeProcessInfo)
        }
    }

    fun getMyService(
        userId: String,
        serviceName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<MyServiceVO> {
        logger.info("the getMyService userId is :$userId,serviceName is :$serviceName")
        // 获取有权限的扩展服务列表
        val records = extServiceDao.getMyService(dslContext, userId, serviceName, page, pageSize)
        val count = extServiceDao.countByUser(dslContext, userId, serviceName)
        logger.info("the getMyService userId is :$userId,records is :$records,count is :$count")
        // 获取项目ID对应的名称
        val projectCodeList = mutableListOf<String>()
        val serviceItemIdMap = mutableMapOf<String, Set<String>>()
        val itemIdList = mutableSetOf<String>()
        records?.forEach {
            val testProjectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                dslContext,
                userId,
                it["serviceCode"] as String,
                StoreTypeEnum.SERVICE
            )
            if (null != testProjectCode) projectCodeList.add(testProjectCode)

            val serviceItemRecords = extServiceItemRelDao.getItemByServiceId(
                dslContext,
                it["serviceId"] as String
            )
            val itemIds = mutableSetOf<String>()
            serviceItemRecords?.forEach { itemInfo ->
                itemIds.add(itemInfo.itemId)
                itemIdList.add(itemInfo.itemId)
            }
            serviceItemIdMap[it["serviceId"] as String] = itemIds
        }
        logger.info("the getMyService serviceItemIdMap is :$serviceItemIdMap")

        logger.info("the getMyService userId is :$userId,projectCodeList is :$projectCodeList")
        val projectMap = client.get(ServiceProjectResource::class).getNameByCode(projectCodeList.joinToString(",")).data
        logger.info("the getMyService userId is :$userId,projectMap is :$projectMap")

        val itemRecordList = client.get(ServiceItemResource::class).getItemInfoByIds(itemIdList).data
        val itemInfoMap = mutableMapOf<String, ServiceItem>()
        itemRecordList?.forEach {
            itemInfoMap[it.itemId] = it
        }
        logger.info("the getMyService userId is :$userId,itemRecordList is :$itemRecordList, itemInfoMap is :$itemInfoMap")

        val myService = mutableListOf<ExtServiceRespItem?>()
        records?.forEach {
            val serviceCode = it["serviceCode"] as String
            val serviceId = it["serviceId"] as String
            var releaseFlag = false // 是否有处于上架状态的扩展服务版本
            val releaseServiceNum = extServiceDao.countReleaseServiceByCode(dslContext, serviceCode)
            if (releaseServiceNum > 0) {
                releaseFlag = true
            }
            val language = extServiceEnvDao.getMarketServiceEnvInfoByServiceId(dslContext, serviceId)?.language
            val serviceItemList = serviceItemIdMap[serviceId]
            logger.info("the getMyService serviceId is :$serviceId, itemList is :$serviceItemList")
            val itemNameList = mutableListOf<String>()
            serviceItemList?.forEach { itId ->
                val itemInfo = itemInfoMap[itId]
                if (itemInfo != null) {
                    itemNameList.add("${itemInfo.parentName}-${itemInfo.itemName}")
                }
            }
            logger.info("the getMyService serviceId is :$serviceId, itemName is :${JsonUtil.toJson(itemNameList)}")
            myService.add(
                ExtServiceRespItem(
                    serviceId = serviceId,
                    serviceName = it["serviceName"] as String,
                    serviceCode = serviceCode,
                    version = it["version"] as String,
                    category = it["category"] as String,
                    logoUrl = it["logoUrl"] as String?,
                    serviceStatus = ExtServiceStatusEnum.getServiceStatus((it["serviceStatus"] as Byte).toInt()),
                    publisher = it["publisher"] as String,
                    publishTime = DateTimeUtil.toDateTime(it["pubTime"] as LocalDateTime),
                    creator = it["creator"] as String,
                    createTime = DateTimeUtil.toDateTime(it["createTime"] as LocalDateTime),
                    modifier = it["modifier"] as String,
                    updateTime = DateTimeUtil.toDateTime(it["updateTime"] as LocalDateTime),
                    projectName = projectMap?.get(
                        storeProjectRelDao.getUserStoreTestProjectCode(
                            dslContext,
                            userId,
                            it["serviceCode"] as String,
                            StoreTypeEnum.SERVICE
                        )
                    ) ?: "",
                    language = language ?: "",
                    itemName = itemNameList,
                    itemIds = serviceItemList ?: emptySet(),
                    releaseFlag = releaseFlag
                )
            )
        }
        return Result(MyServiceVO(count, page, pageSize, myService))
    }

    fun deleteExtensionService(userId: String, serviceCode: String): Result<Boolean> {
        logger.info("deleteService userId: $userId , serviceId: $serviceCode")
        val result = extServiceDao.getExtServiceIds(dslContext, serviceCode)
        var extServiceId: String? = null
        val extServiceIdList = result.map {
            if (it["latestFlag"] as Boolean) {
                extServiceId = it["id"] as String
            }
            it["id"] as String
        }
        if (extServiceId.isNullOrBlank()) {
            return MessageUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_SERVICE_NOT_EXIST,
                params = arrayOf(serviceCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val type = StoreTypeEnum.SERVICE.type.toByte()
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, serviceCode, type)) {
            return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PERMISSION_DENIED,
                language = I18nUtil.getLanguage(userId))
        }
        val releasedCount = extServiceDao.countReleaseServiceByCode(dslContext, serviceCode)
        logger.info("releasedCount: $releasedCount")
        if (releasedCount > 0) {
            return MessageUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_SERVICE_RELEASED_IS_NOT_ALLOW_DELETE,
                params = arrayOf(serviceCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 如果已经被安装到其他项目下使用，不能删除
        val installedCount = storeProjectRelDao.countInstalledProject(dslContext, serviceCode, type)
        logger.info("installedCount: $releasedCount")
        if (installedCount > 0) {
            return MessageUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_SERVICE_USED_IS_NOT_ALLOW_DELETE,
                params = arrayOf(serviceCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val initProjectCode =
            storeProjectRelDao.getInitProjectCodeByStoreCode(
                dslContext,
                serviceCode,
                StoreTypeEnum.SERVICE.type.toByte()
            )
        // 停止bcs灰度命名空间和正式命名空间的应用
        val bcsStopAppResult = extServiceBcsService.stopExtService(
            userId = userId,
            serviceCode = serviceCode,
            deploymentName = serviceCode,
            serviceName = "$serviceCode-service"
        )
        logger.info("the bcsStopAppResult is :$bcsStopAppResult")
        //  删除仓库镜像
        try {
            val serviceEnvRecord = extServiceEnvDao.getMarketServiceEnvInfoByServiceId(dslContext, extServiceId!!)
            if (serviceEnvRecord != null && serviceEnvRecord.imagePath.isNotEmpty()) {
                deleteNode(userId, serviceCode)
            }
        } catch (ignored: Throwable) {
            logger.warn("delete service[$serviceCode] repository image fail!", ignored)
        }

        // 删除代码库
        val extServiceRecord = extFeatureDao.getServiceByCode(dslContext, serviceCode)
        deleteExtServiceRepository(
            userId = userId,
            projectCode = initProjectCode,
            repositoryHashId = extServiceRecord!!.repositoryHashId,
            tokenType = TokenTypeEnum.PRIVATE_KEY
        )
        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeCommonService.deleteStoreInfo(context, serviceCode, StoreTypeEnum.SERVICE.type.toByte())
            extServiceEnvDao.deleteEnvInfo(context, extServiceIdList)
            extServiceFeatureDao.deleteExtFeatureServiceData(context, serviceCode)
            extServiceVersionLogDao.deleteByServiceId(context, extServiceIdList)
            extItemServiceDao.deleteByServiceId(context, extServiceIdList)
            extServiceDao.deleteExtServiceData(context, serviceCode)
        }
        return Result(true)
    }

    fun deleteExtServiceRepository(
        userId: String,
        projectCode: String?,
        repositoryHashId: String,
        tokenType: TokenTypeEnum
    ) {
        // 删除代码库信息
        if (!projectCode.isNullOrEmpty() && repositoryHashId.isNotBlank()) {
            try {
                val delGitRepositoryResult =
                    client.get(ServiceGitRepositoryResource::class)
                        .delete(
                            userId = userId,
                            projectId = projectCode,
                            repositoryHashId = repositoryHashId,
                            tokenType = tokenType
                        )
                logger.info("the delGitRepositoryResult is :$delGitRepositoryResult")
            } catch (ignored: Throwable) {
                logger.warn("delete service git repository fail!", ignored)
            }
        }
    }

    fun deleteNode(userId: String, serviceCode: String) {
        val serviceUrlPrefix = client.getServiceUrl(ServiceArchiveStoreFileResource::class)
        val serviceUrl = "$serviceUrlPrefix/service/artifactories/store/file/repos/" +
                "${bkRepoConfig.bkrepoDockerRepoName}/$serviceCode/delete?type=${StoreTypeEnum.SERVICE.name}"
        val request = Request.Builder()
            .url(serviceUrl)
            .delete()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                val responseContent = response.body!!.string()
                throw RemoteServiceException("delete node file failed: $responseContent", response.code)
            }
        }
    }

    fun offlineService(userId: String, serviceCode: String, serviceOfflineDTO: ServiceOfflineDTO): Result<Boolean> {
        // 判断用户是否有权限下线
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, serviceCode, StoreTypeEnum.SERVICE.type.toByte())) {
            return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PERMISSION_DENIED,
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 停止bcs灰度命名空间和正式命名空间的应用
        val bcsStopAppResult = extServiceBcsService.stopExtService(
            userId = userId,
            serviceCode = serviceCode,
            deploymentName = serviceCode,
            serviceName = "$serviceCode-service"
        )
        logger.info("the bcsStopAppResult is :$bcsStopAppResult")
        if (bcsStopAppResult.isNotOk()) {
            return bcsStopAppResult
        }
        // 设置扩展服务状态为下架中
        extServiceDao.setServiceStatusByCode(
            dslContext, serviceCode, ExtServiceStatusEnum.RELEASED.status.toByte(),
            ExtServiceStatusEnum.UNDERCARRIAGED.status.toByte(), userId, serviceOfflineDTO.reason
        )
        // 通过websocket推送状态变更消息
//        storeWebsocketService.sendWebsocketMessageByAtomCodeAndUserId(serviceCode, userId)
        // 通知使用方扩展服务即将下架 -- todo 待carl完善

        return Result(true)
    }

    fun listLanguage(): List<String?> {
        val records = storeBuildInfoDao.list(dslContext, StoreTypeEnum.SERVICE)
        val ret = mutableListOf<String>()
        records?.forEach {
            ret.add(
                it.language
            )
        }
        return ret
    }

    @Suppress("UNCHECKED_CAST")
    fun getServiceById(serviceId: String, userId: String): Result<ServiceVersionVO?> {
        return getServiceVersion(serviceId, userId)
    }

    fun getServiceByCode(userId: String, serviceCode: String): Result<ServiceVersionVO?> {
        val record = extServiceDao.getServiceLatestByCode(dslContext, serviceCode)
        return (if (null == record) {
            Result(data = null)
        } else {
            getServiceVersion(record.id, userId)
        })
    }

    fun getServiceVersionListByCode(
        userId: String,
        serviceCode: String,
        page: Int,
        pageSize: Int
    ): Result<Page<ServiceVersionListItem>> {
        logger.info("getServiceVersionListByCode params[$userId|$serviceCode|$page|$pageSize]")
        val totalCount = extServiceDao.countByCode(dslContext, serviceCode)
        val records = extServiceDao.listServiceByCode(dslContext, serviceCode)
        val atomVersions = mutableListOf<ServiceVersionListItem>()
        if (records != null) {
            val serviceIds = records.map { it.id }
            // 批量获取版本内容
            val versionRecords = extServiceVersionLogDao.getVersionLogsByServiceIds(
                dslContext = dslContext,
                serviceIds = serviceIds
            )
            val versionMap = mutableMapOf<String, String>()
            versionRecords?.forEach { versionRecord ->
                versionMap[versionRecord.serviceId] = versionRecord.content
            }
            records.forEach {
                atomVersions.add(
                    ServiceVersionListItem(
                        serviceId = it.id,
                        serviceCode = it.serviceCode,
                        serviceName = it.serviceName,
                        version = it.version,
                        versionContent = versionMap[it.id].toString(),
                        serviceStatus = ExtServiceStatusEnum.getServiceStatus((it.serviceStatus as Byte).toInt()),
                        creator = it.creator,
                        createTime = DateTimeUtil.toDateTime(it.createTime)
                    )
                )
            }
        }
        return Result(Page(page, pageSize, totalCount.toLong(), atomVersions))
    }

    /**
     * 取消发布
     */
    fun cancelRelease(userId: String, serviceId: String): Result<Boolean> {
        logger.info("extService cancelRelease, userId=$userId, serviceId=$serviceId")
        val serviceRecord = extServiceDao.getServiceById(dslContext, serviceId)
            ?: return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(serviceId),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        val status = ExtServiceStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        val (checkResult, code) = checkServiceVersionOptRight(userId, serviceId, status)
        if (!checkResult) {
            return MessageUtil.generateResponseDataObject(
                messageCode = code,
                language = I18nUtil.getLanguage(userId))
        }
        // 如果该版本的状态已处于测试中及其后面的状态，取消发布则需要停掉灰度命名空间的应用
        val serviceCode = serviceRecord.serviceCode
        val bcsStopAppResult = extServiceBcsService.stopExtService(
            userId = userId,
            serviceCode = serviceCode,
            deploymentName = serviceCode,
            serviceName = "$serviceCode-service",
            checkPermissionFlag = true,
            grayFlag = true
        )
        logger.info("$serviceCode bcsStopAppResult is :$bcsStopAppResult")
        if (bcsStopAppResult.isNotOk()) {
            return bcsStopAppResult
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            extFeatureDao.updateExtServiceFeatureBaseInfo(
                dslContext = context,
                serviceCode = serviceCode,
                userId = userId,
                extServiceFeatureUpdateInfo = ExtServiceFeatureUpdateInfo(
                    killGrayAppFlag = null,
                    killGrayAppMarkTime = null
                )
            )
            extServiceDao.setServiceStatusById(
                dslContext = context,
                serviceId = serviceId,
                serviceStatus = status,
                userId = userId,
                msg = I18nUtil.getCodeLanMessage(
                    messageCode = UN_RELEASE,
                    language = I18nUtil.getLanguage(userId))
            )
        }
        return Result(true)
    }

    /**
     * 确认通过测试，继续发布
     */
    fun passTest(userId: String, serviceId: String): Result<Boolean> {
        logger.info("passTest, userId=$userId, serviceId=$serviceId")
        val serviceRecord = extServiceDao.getServiceById(dslContext, serviceId)
            ?: return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(serviceId),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        // 查看当前版本之前的版本是否有已发布的，如果有已发布的版本则只是普通的升级操作而不需要审核
        val serviceStatus = ExtServiceStatusEnum.EDIT.status.toByte()

        extServiceDao.setServiceStatusById(dslContext, serviceId, serviceStatus, userId, "")
//            // 通过websocket推送状态变更消息
//            storeWebsocketService.sendWebsocketMessage(userId, serviceId)

        return Result(true)
    }

    fun rebuild(projectCode: String, userId: String, serviceId: String): Result<Boolean> {
        logger.info("rebuild, projectCode=$projectCode, userId=$userId, serviceId=$serviceId")
        // 判断是否可以启动构建
        val status = ExtServiceStatusEnum.BUILDING.status.toByte()
        val (checkResult, code) = checkServiceVersionOptRight(userId, serviceId, status)
        if (!checkResult) {
            return MessageUtil.generateResponseDataObject(
                messageCode = code,
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 拉取extension.json，检查格式，更新入库
        val serviceRecord =
            extServiceDao.getServiceById(dslContext, serviceId) ?: return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(serviceId),
                language = I18nUtil.getLanguage(userId)
            )
        val serviceCode = serviceRecord.serviceCode
        val extServiceFeature = extFeatureDao.getServiceByCode(dslContext, serviceCode)!!
        // 从工蜂拉取文件
        val fileStr = client.get(ServiceGitRepositoryResource::class).getFileContent(
            extServiceFeature.repositoryHashId,
            EXTENSION_JSON_NAME, null, null, null
        ).data
        logger.info("get serviceCode[$serviceCode] file($EXTENSION_JSON_NAME) fileStr is:$fileStr")
        dslContext.transaction { t ->
            val context = DSL.using(t)
            if (!fileStr.isNullOrEmpty()) {
                val extensionJson = JsonUtil.to(fileStr, ExtensionJson::class.java)
                logger.info("extensionJson is:$extensionJson")
                val fileServiceCode = extensionJson.serviceCode
                val fileItemList = extensionJson.itemList
                if (fileServiceCode != serviceCode) {
                    logger.warn("input serviceCode[$serviceCode], $EXTENSION_JSON_NAME serviceCode[$fileServiceCode] ")
                }
                if (fileItemList != null) {
                    val itemCodeList = mutableSetOf<String>()
                    val filePropMap = mutableMapOf<String, String>()
                    fileItemList.forEach {
                        itemCodeList.add(it.itemCode)
                        filePropMap[it.itemCode] = JsonUtil.toJson(it.props ?: "{}")
                    }
                    // 用配置文件最新的配置替换数据库中相关记录的配置
                    val serviceItemList = client.get(ServiceItemResource::class).getItemByCodes(itemCodeList).data
                    logger.info("get serviceCode[$serviceCode] serviceItemList is:$serviceItemList")
                    serviceItemList?.forEach {
                        val props = filePropMap[it.itemCode]
                        filePropMap[it.itemId] = props!!
                        filePropMap.remove(it.itemCode)
                    }
                    logger.info("get serviceCode[$serviceCode] filePropMap is:$filePropMap")
                    val serviceItemRelList = extServiceItemRelDao.getItemByServiceId(context, serviceId)
                    if (serviceItemRelList != null && serviceItemRelList.isNotEmpty) {
                        serviceItemRelList.forEach {
                            // 如果json配置文件配了该扩展点，就将数据库扩展点记录的props字段替换成最新的
                            val props = filePropMap[it.itemId]
                            if (props != null) {
                                it.props = props
                            }
                        }
                        // 批量更新扩展服务和扩展点关联关系记录
                        extServiceItemRelDao.batchUpdateServiceItemRel(context, serviceItemRelList)
                    }
                }
            }
            asyncHandleUpdateService(context, serviceId, userId)
        }
        return Result(true)
    }

    fun getCompleteEditStatus(isNormalUpgrade: Boolean): Byte {
        return if (isNormalUpgrade) ExtServiceStatusEnum.RELEASE_DEPLOYING.status.toByte() else ExtServiceStatusEnum.AUDITING.status.toByte()
    }

    fun createMediaAndVisible(userId: String, serviceId: String, submitInfo: ExtSubmitDTO): Result<Boolean> {
        val mediaList = submitInfo.mediaInfoList
        val deptList = submitInfo.deptInfoList

        val serviceInfo = extServiceDao.getServiceById(dslContext, serviceId) ?: throw ErrorCodeException(errorCode = StoreMessageCode.USER_SERVICE_NOT_EXIST)
        val serviceCode = serviceInfo.serviceCode

        val oldStatus = serviceInfo.serviceStatus
        val isNormalUpgrade = getNormalUpgradeFlag(serviceCode, oldStatus.toInt())
        val newStatus = getCompleteEditStatus(isNormalUpgrade)
        val (checkResult, code) = checkServiceVersionOptRight(userId, serviceId, newStatus, isNormalUpgrade)

        if (!checkResult) {
            return MessageUtil.generateResponseDataObject(
                messageCode = code,
                language = I18nUtil.getLanguage(userId))
        }
        // 先集中删除，再添加媒体信息
        mediaService.deleteByStoreCode(userId, serviceCode, StoreTypeEnum.SERVICE)
        mediaList.forEach {
            mediaService.add(
                userId = userId,
                type = StoreTypeEnum.SERVICE,
                storeMediaInfo = StoreMediaInfoRequest(
                    storeCode = serviceCode,
                    mediaUrl = it.mediaUrl,
                    mediaType = it.mediaType.name,
                    modifier = userId
                )
            )
        }
        deptService.addVisibleDept(
            userId = userId,
            storeType = StoreTypeEnum.SERVICE,
            storeCode = serviceCode,
            deptInfos = deptList
        )
        if (isNormalUpgrade) {
            // 正式发布最新的扩展服务版本
            val deployExtServiceResult = extServiceBcsService.deployExtService(
                userId = userId,
                grayFlag = false,
                serviceCode = serviceCode,
                version = serviceInfo.version
            )
            logger.info("deployExtServiceResult is:$deployExtServiceResult")
            if (deployExtServiceResult.isNotOk()) {
                return deployExtServiceResult
            }
        }

        extServiceDao.setServiceStatusById(
            dslContext = dslContext,
            serviceId = serviceId,
            serviceStatus = newStatus,
            userId = userId,
            msg = "add media file "
        )
        return Result(true)
    }

    fun backToTest(userId: String, serviceId: String): Result<Boolean> {
        logger.info("back to test： serviceId[$serviceId], userId[$serviceId]")
        val newStatus = ExtServiceStatusEnum.TESTING
        val (checkResult, code) = checkServiceVersionOptRight(userId, serviceId, newStatus.status.toByte())

        if (!checkResult) {
            return MessageUtil.generateResponseDataObject(
                messageCode = code,
                language = I18nUtil.getLanguage(userId))
        }

        extServiceDao.setServiceStatusById(
            dslContext = dslContext,
            serviceId = serviceId,
            userId = userId,
            serviceStatus = newStatus.status.toByte(),
            msg = "back to test"
        )
        return Result(true)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getServiceVersion(serviceId: String, userId: String): Result<ServiceVersionVO?> {
        logger.info("getServiceVersion serviceID[$serviceId], userID[$userId]")
        val record = extServiceDao.getServiceById(dslContext, serviceId)
        return if (null == record) {
            Result(data = null)
        } else {
            logger.info("getServiceVersion ServiceRecord: $record")
            val serviceCode = record.serviceCode
            val defaultFlag = record.deleteFlag
            val projectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
                dslContext,
                serviceCode,
                StoreTypeEnum.SERVICE.type.toByte()
            )
            logger.info("getServiceVersion projectCode: $projectCode")
            val featureInfoRecord = extFeatureDao.getLatestServiceByCode(dslContext, serviceCode)

            val flag =
                storeUserService.isCanInstallStoreComponent(defaultFlag, userId, serviceCode, StoreTypeEnum.SERVICE)
            val userCommentInfo =
                storeCommentService.getStoreUserCommentInfo(userId, serviceCode, StoreTypeEnum.SERVICE)
            val serviceEnv = extServiceEnvDao.getMarketServiceEnvInfoByServiceId(dslContext, serviceId)
            logger.info("getServiceVersion serviceEnv: $serviceEnv")
            val itemList = getItemByItems(serviceId)
            val mediaList = mediaService.getByCode(serviceCode, StoreTypeEnum.SERVICE).data
            val labelRecords = extServiceLabelDao.getLabelsByServiceId(dslContext, serviceId)
            val lableList = mutableListOf<Label>()
            labelRecords?.forEach {
                lableList.add(
                    Label(
                        id = it[KEY_LABEL_ID] as String,
                        labelCode = it[KEY_LABEL_CODE] as String,
                        labelName = it[KEY_LABEL_NAME] as String,
                        labelType = StoreTypeEnum.getStoreType((it[KEY_LABEL_TYPE] as Byte).toInt())
                    )
                )
            }
            val extensionName = getAllItemName(itemList.toSet())
            val serviceVersion = extServiceVersionLogDao.getVersionLogByServiceId(dslContext, serviceId)

            Result(
                ServiceVersionVO(
                    serviceId = serviceId,
                    serviceCode = serviceCode,
                    serviceName = record.serviceName,
                    logoUrl = record.logoUrl,
                    summary = record.summary ?: "",
                    description = record.description ?: "",
                    version = record.version,
                    serviceStatus = ExtServiceStatusEnum.getServiceStatus((record.serviceStatus).toInt()),
                    language = serviceEnv!!.language,
                    codeSrc = featureInfoRecord!!.codeSrc ?: "",
                    publisher = record.publisher,
                    modifier = record.modifier,
                    creator = record.creator,
                    createTime = DateTimeUtil.toDateTime(record.createTime as LocalDateTime),
                    updateTime = DateTimeUtil.toDateTime(record.updateTime as LocalDateTime),
                    flag = flag,
                    repositoryAuthorizer = record.publisher,
                    defaultFlag = defaultFlag,
                    projectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                        dslContext,
                        userId,
                        serviceCode,
                        StoreTypeEnum.SERVICE
                    ),
                    labelList = lableList,
                    labelIdList = lableList.map { it.id },
                    userCommentInfo = userCommentInfo,
                    visibilityLevel = VisibilityLevelEnum.getVisibilityLevel(featureInfoRecord.visibilityLevel),
                    recommendFlag = featureInfoRecord.recommendFlag,
                    publicFlag = featureInfoRecord.publicFlag,
                    certificationFlag = featureInfoRecord.certificationFlag,
                    descInputType = featureInfoRecord.descInputType,
                    weight = featureInfoRecord.weight,
                    serviceType = featureInfoRecord.serviceType.toInt(),
                    extensionItemList = itemList,
                    mediaList = mediaList,
                    itemName = extensionName,
                    bkServiceId = getBkServiceByItems(serviceId),
                    versionContent = serviceVersion?.content ?: "",
                    releaseType = ReleaseTypeEnum.getReleaseType(
                        serviceVersion?.releaseType?.toInt() ?: ReleaseTypeEnum.NEW.releaseType
                    ),
                    editFlag = extServiceCommonService.checkEditCondition(serviceCode)
                )
            )
        }
    }

    abstract fun getRepositoryInfo(projectCode: String?, repositoryHashId: String?): Result<Repository?>

    abstract fun handleServicePackage(
        extensionInfo: InitExtServiceDTO,
        userId: String,
        serviceCode: String
    ): Result<Map<String, String>?>

    abstract fun getExtServicePackageSourceType(serviceCode: String): ExtServicePackageSourceTypeEnum

    private fun getNormalUpgradeFlag(serviceCode: String, status: Int): Boolean {
        val releaseTotalNum = extServiceDao.countReleaseServiceByCode(dslContext, serviceCode)
        val currentNum = if (status == ExtServiceStatusEnum.RELEASED.status) 1 else 0
        return releaseTotalNum > currentNum
    }

    private fun submitExtService(
        userId: String,
        serviceId: String,
        extServiceUpdateInfo: ExtServiceUpdateInfo,
        extServiceVersionLogCreateInfo: ExtServiceVersionLogCreateInfo
    ) {
        extServiceDao.updateExtServiceBaseInfo(
            dslContext = dslContext,
            userId = userId,
            serviceId = serviceId,
            extServiceUpdateInfo = extServiceUpdateInfo
        )
        extServiceVersionLogDao.create(
            dslContext = dslContext,
            userId = userId,
            id = serviceId,
            extServiceVersionLogCreateInfo = extServiceVersionLogCreateInfo
        )
    }

    private fun getItemByItems(serviceId: String): List<String> {
        val serviceItems = extServiceItemRelDao.getItemByServiceId(dslContext, serviceId)
        val itemIds = mutableListOf<String>()
        serviceItems?.forEach { it ->
            itemIds.add(it.itemId)
        }
        logger.info("getItemByItems serviceId[$serviceId] items[$itemIds]")
        return itemIds
    }

    private fun getBkServiceByItems(serviceId: String): Set<Long> {
        val serviceItems = extServiceItemRelDao.getItemByServiceId(dslContext, serviceId)
        val bkServiceIds = mutableSetOf<Long>()
        serviceItems?.forEach { it ->
            bkServiceIds.add(it.bkServiceId)
        }
        logger.info("getItemByItems serviceId[$serviceId] bkServiceIds[$bkServiceIds]")
        return bkServiceIds
    }

    private fun getAllItemName(itemList: Set<String>): String {
        val itemRecords = client.get(ServiceItemResource::class).getItemInfoByIds(itemList).data
        var itemNames = ""
        itemRecords?.forEach {
            itemNames = itemNames + it.parentName + "-" + it.itemName + ","
        }
        itemNames = itemNames.substringBeforeLast(",")

        return itemNames
    }

    private fun upgradeMarketExtService(
        context: DSLContext,
        userId: String,
        serviceId: String,
        language: String,
        extServiceCreateInfo: ExtServiceCreateInfo,
        extServiceVersionLogCreateInfo: ExtServiceVersionLogCreateInfo
    ) {
        extServiceDao.createExtService(
            dslContext = context,
            userId = userId,
            id = serviceId,
            extServiceCreateInfo = extServiceCreateInfo
        )
        val extServiceEnvCreateInfo = ExtServiceEnvCreateInfo(
            serviceId = serviceId,
            language = language,
            pkgPath = "",
            pkgShaContent = "",
            dockerFileContent = "",
            imagePath = "",
            creatorUser = userId,
            modifierUser = userId
        )
        extServiceEnvDao.create(context, extServiceEnvCreateInfo) // 添加扩展服务执行环境信息
        extServiceVersionLogDao.create(
            dslContext = context,
            userId = userId,
            id = serviceId,
            extServiceVersionLogCreateInfo = extServiceVersionLogCreateInfo
        )
    }

    private fun validateAddServiceReq(
        userId: String,
        extensionInfo: InitExtServiceDTO
    ) {
        logger.info("validateExtServiceReq userId is :$userId,info[$extensionInfo]")
        val serviceCode = extensionInfo.serviceCode
        if (!Pattern.matches("^[a-z]([-a-z-0-9]*[a-z|0-9])?\$", serviceCode)) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(serviceCode))
        }
        // 判断扩展服务是否存在
        val codeInfo = extServiceDao.getServiceLatestByCode(dslContext, serviceCode)
        if (codeInfo != null) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_EXIST, params = arrayOf(serviceCode))
        }
        val serviceName = extensionInfo.serviceName
        val nameInfo = extServiceDao.getServiceByName(dslContext, serviceName)
        if (nameInfo != null) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_EXIST, params = arrayOf(serviceName))
        }
    }

    private fun handleProcessInfo(isNormalUpgrade: Boolean, status: Int): List<ReleaseProcessItem> {
        val processInfo = initProcessInfo(isNormalUpgrade)
        val totalStep = if (isNormalUpgrade) NUM_SIX else NUM_SEVEN
        when (status) {
            ExtServiceStatusEnum.INIT.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_ONE, DOING)
            }
            ExtServiceStatusEnum.BUILDING.status, ExtServiceStatusEnum.COMMITTING.status, ExtServiceStatusEnum.DEPLOYING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_TWO, DOING)
            }
            ExtServiceStatusEnum.BUILD_FAIL.status, ExtServiceStatusEnum.DEPLOY_FAIL.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_TWO, FAIL)
            }
            ExtServiceStatusEnum.TESTING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, DOING)
            }
            ExtServiceStatusEnum.EDIT.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FOUR, DOING)
            }
            ExtServiceStatusEnum.AUDITING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FIVE, DOING)
            }
            ExtServiceStatusEnum.AUDIT_REJECT.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FIVE, FAIL)
            }
            ExtServiceStatusEnum.RELEASE_DEPLOYING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, if (isNormalUpgrade) NUM_FIVE else NUM_SIX, DOING)
            }
            ExtServiceStatusEnum.RELEASE_DEPLOY_FAIL.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, if (isNormalUpgrade) NUM_FIVE else NUM_SIX, FAIL)
            }

            ExtServiceStatusEnum.RELEASED.status -> {
                val currStep = if (isNormalUpgrade) NUM_SIX else NUM_SEVEN
                storeCommonService.setProcessInfo(processInfo, totalStep, currStep, SUCCESS)
            }
        }
        return processInfo
    }

    private fun initProcessInfo(isNormalUpgrade: Boolean): List<ReleaseProcessItem> {
        val processInfo = mutableListOf<ReleaseProcessItem>()
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
            messageCode = BEGIN,
            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())), BEGIN, NUM_ONE, SUCCESS))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
            messageCode = TEST_ENV_PREPARE,
            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())), TEST_ENV_PREPARE, NUM_TWO, UNDO))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
            messageCode = TEST,
            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())), TEST, NUM_THREE, UNDO))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
            messageCode = EDIT,
            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())), COMMIT, NUM_FOUR, UNDO))
        if (isNormalUpgrade) {
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
                messageCode = ONLINE,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())), ONLINE, NUM_FIVE, UNDO))
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
                messageCode = END,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())), END, NUM_SIX, UNDO))
        } else {
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
                messageCode = APPROVE,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())), APPROVE, NUM_FIVE, UNDO))
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
                messageCode = ONLINE,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())), ONLINE, NUM_SIX, UNDO))
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
                messageCode = END,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())), END, NUM_SEVEN, UNDO))
        }
        return processInfo
    }

    private fun validateAddServiceReqByName(serviceName: String, serviceCode: String): Boolean {
        var flag = false
        val count = extServiceDao.countByName(dslContext, serviceName)
        if (count > 0) {
            // 判断微扩展名称是否重复（微扩展升级允许名称一样）
            flag = extServiceDao.countByName(
                dslContext = dslContext,
                serviceName = serviceName,
                serviceCode = serviceCode
            ) < count
        }
        return flag
    }

    private fun getFileServiceProps(
        serviceCode: String,
        repositoryHashId: String,
        fileName: String,
        inputItemList: Set<String>
    ): List<ItemPropCreateInfo> {
        val itemCreateList = mutableListOf<ItemPropCreateInfo>()
        val inputItemMap = mutableMapOf<String, ItemPropCreateInfo>()
        // 匹配页面输入的itemId，是否在json文件内有对应的props，若没有则为空。有则替换。  json文件内多余的itemCode无视
        inputItemList.forEach {
            inputItemMap[it] = ItemPropCreateInfo(
                itemId = it,
                props = "",
                bkServiceId = getItemBkServiceId(it)
            )
        }

        // 从工蜂拉取文件
        val fileStr = client.get(ServiceGitRepositoryResource::class).getFileContent(
            repositoryHashId,
            fileName, null, null, null
        ).data
        if (fileStr.isNullOrEmpty()) {
            // 文件数据为空，直接返回输入数据
            return returnInputData(inputItemList)
        }
        val taskDataMap = objectMapper.readValue<ExtensionJson>(fileStr!!)
        logger.info("getServiceProps taskDataMap[$taskDataMap]")
        val fileServiceCode = taskDataMap.serviceCode
        val fileItemList = taskDataMap.itemList
        if (fileServiceCode != serviceCode) {
            logger.warn("getServiceProps input serviceCode[$serviceCode], extension.json serviceCode[$fileServiceCode] ")
            throw ErrorCodeException(errorCode = StoreMessageCode.USER_SERVICE_CODE_DIFF)
        }

        if (fileItemList == null) {
            // 文件数据为空，直接返回输入数据
            return returnInputData(inputItemList)
        }

        // 文件与输入itemCode取交集，若文件内有props，以文件props为准
        val filePropMap = mutableMapOf<String, String>()
        fileItemList.forEach {
            filePropMap[it.itemCode] = JsonUtil.toJson(it.props ?: "")
        }
        val itemRecords = client.get(ServiceItemResource::class).getItemInfoByIds(inputItemList).data
        itemRecords?.forEach {
            val itemCode = it.itemCode
            val itemId = it.itemId
            if (filePropMap.keys.contains(itemCode)) {
                val propsInfo = filePropMap[itemCode]
                inputItemMap[itemId] = ItemPropCreateInfo(
                    itemId = itemId,
                    props = propsInfo ?: "",
                    bkServiceId = getItemBkServiceId(itemId)
                )
            }
        }

        // 返回取完交集后的数据
        inputItemMap.forEach { (_, u) ->
            itemCreateList.add(u)
        }
        logger.info("getServiceProps itemCreateList[$itemCreateList], filePropMap[$filePropMap]")

        return itemCreateList
    }

    private fun returnInputData(inputItem: Set<String>): List<ItemPropCreateInfo> {
        // 默认添加页面选中的扩展点, props给空
        val itemCreateList = mutableListOf<ItemPropCreateInfo>()
        inputItem.forEach {
            itemCreateList.add(
                ItemPropCreateInfo(
                    itemId = it,
                    props = "",
                    bkServiceId = getItemBkServiceId(it)
                )
            )
        }
        return itemCreateList
    }

    /**
     * 检查版本发布过程中的操作权限
     */
    fun checkServiceVersionOptRight(
        userId: String,
        serviceId: String,
        status: Byte,
        isNormalUpgrade: Boolean? = null
    ): Pair<Boolean, String> {
        logger.info("checkServiceVersionOptRight params[$userId|$serviceId|$status|$isNormalUpgrade]")
        val record =
            extServiceDao.getServiceById(dslContext, serviceId) ?: return Pair(
                false,
                CommonMessageCode.PARAMETER_IS_INVALID
            )
        val serviceCode = record.serviceCode
        val owner = record.owner
        val recordStatus = record.serviceStatus

        // 判断用户是否有权限(当前版本的创建者和管理员可以操作)
        if (!(storeMemberDao.isStoreAdmin(
                dslContext = dslContext,
                userId = userId,
                storeCode = serviceCode,
                storeType = StoreTypeEnum.SERVICE.type.toByte()
            ) || owner == userId)
        ) {
            return Pair(false, CommonMessageCode.PERMISSION_DENIED)
        }
        val allowDeployStatus = if (isNormalUpgrade != null && isNormalUpgrade) ExtServiceStatusEnum.EDIT
        else ExtServiceStatusEnum.AUDITING
        var validateFlag = true
        if (status == ExtServiceStatusEnum.COMMITTING.status.toByte() &&
            recordStatus != ExtServiceStatusEnum.INIT.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ExtServiceStatusEnum.BUILDING.status.toByte() &&
            recordStatus !in (
                listOf(
                    ExtServiceStatusEnum.COMMITTING.status.toByte(),
                    ExtServiceStatusEnum.BUILD_FAIL.status.toByte(),
                    ExtServiceStatusEnum.TESTING.status.toByte()
                ))
        ) {
            validateFlag = false
        } else if (status == ExtServiceStatusEnum.BUILD_FAIL.status.toByte() &&
            recordStatus !in (
                listOf(
                    ExtServiceStatusEnum.COMMITTING.status.toByte(),
                    ExtServiceStatusEnum.BUILDING.status.toByte(),
                    ExtServiceStatusEnum.BUILD_FAIL.status.toByte(),
                    ExtServiceStatusEnum.TESTING.status.toByte()
                ))
        ) {
            validateFlag = false
        } else if (status == ExtServiceStatusEnum.TESTING.status.toByte() &&
            recordStatus !in (
                listOf(
                    ExtServiceStatusEnum.BUILDING.status.toByte(),
                    ExtServiceStatusEnum.EDIT.status.toByte()
                ))
        ) {
            validateFlag = false
        } else if (status == ExtServiceStatusEnum.EDIT.status.toByte() &&
            recordStatus != ExtServiceStatusEnum.TESTING.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ExtServiceStatusEnum.AUDITING.status.toByte() &&
            recordStatus != ExtServiceStatusEnum.EDIT.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ExtServiceStatusEnum.AUDIT_REJECT.status.toByte() &&
            recordStatus != ExtServiceStatusEnum.AUDITING.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ExtServiceStatusEnum.RELEASE_DEPLOYING.status.toByte() &&
            recordStatus != allowDeployStatus.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ExtServiceStatusEnum.RELEASE_DEPLOY_FAIL.status.toByte() &&
            recordStatus != ExtServiceStatusEnum.RELEASE_DEPLOYING.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ExtServiceStatusEnum.RELEASED.status.toByte() &&
            recordStatus != ExtServiceStatusEnum.RELEASE_DEPLOYING.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ExtServiceStatusEnum.GROUNDING_SUSPENSION.status.toByte() &&
            recordStatus == ExtServiceStatusEnum.RELEASED.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ExtServiceStatusEnum.UNDERCARRIAGING.status.toByte() &&
            recordStatus == ExtServiceStatusEnum.RELEASED.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ExtServiceStatusEnum.UNDERCARRIAGED.status.toByte() &&
            recordStatus !in (
                listOf(
                    ExtServiceStatusEnum.UNDERCARRIAGING.status.toByte(),
                    ExtServiceStatusEnum.RELEASED.status.toByte()
                ))
        ) {
            validateFlag = false
        }
        return if (validateFlag) Pair(true, "") else Pair(false, StoreMessageCode.USER_SERVICE_RELEASE_STEPS_ERROR)
    }

    fun getItemBkServiceId(itemId: String): Long {
        var bkServiceId = redisOperation.hget(ITEM_BK_SERVICE_REDIS_KEY, itemId)
        // redis中没有取到蓝盾服务id，则去数据库中取
        if (null == bkServiceId) {
            val serviceItem = client.get(ServiceItemResource::class).getItemById(itemId).data
            if (null == serviceItem) {
                throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(itemId))
            } else {
                bkServiceId = serviceItem.parentId
            }
        }
        return bkServiceId.toLong()
    }

    fun updateExtInfo(
        userId: String,
        serviceId: String,
        serviceCode: String,
        infoResp: EditInfoDTO,
        checkPermissionFlag: Boolean = true
    ): Result<Boolean> {
        logger.info("updateExtInfo: serviceId[$serviceId], serviceCode[$serviceCode] infoResp[$infoResp]")
        // 判断当前用户是否是该扩展的成员
        if (checkPermissionFlag && !storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = serviceCode,
                storeType = StoreTypeEnum.SERVICE.type.toByte()
            )
        ) {
            return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PERMISSION_DENIED,
                language = I18nUtil.getLanguage(userId))
        }
        // 查询扩展的最新记录
        val newestServiceRecord = extServiceDao.getNewestServiceByCode(dslContext, serviceCode)
            ?: throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(serviceCode))
        val editFlag = extServiceCommonService.checkEditCondition(serviceCode)
        if (!editFlag) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_ATOM_VERSION_IS_NOT_FINISH,
                params = arrayOf(newestServiceRecord.serviceName, newestServiceRecord.version)
            )
        }
        val baseInfo = infoResp.baseInfo
        val settingInfo = infoResp.settingInfo
        if (baseInfo != null) {
            extServiceDao.updateExtServiceBaseInfo(
                dslContext = dslContext,
                userId = userId,
                serviceId = serviceId,
                extServiceUpdateInfo = ExtServiceUpdateInfo(
                    serviceName = baseInfo.serviceName,
                    logoUrl = baseInfo.logoUrl,
                    iconData = baseInfo.iconData,
                    summary = baseInfo.summary,
                    description = baseInfo.description,
                    modifierUser = userId,
                    status = null,
                    latestFlag = null
                )
            )

            // 更新标签信息
            val labelIdList = baseInfo.labels
            if (null != labelIdList) {
                extServiceLabelDao.deleteByServiceId(dslContext, serviceId)
                if (labelIdList.isNotEmpty())
                    extServiceLabelDao.batchAdd(dslContext, userId, serviceId, labelIdList)
            }
            val itemIds = baseInfo.itemIds
            if (itemIds != null) {
                val featureInfoRecord = extFeatureDao.getLatestServiceByCode(dslContext, serviceCode)
                val itemCreateInfoList =
                    getFileServiceProps(serviceCode, featureInfoRecord!!.repositoryHashId, EXTENSION_JSON_NAME, itemIds)
                extServiceItemRelDao.deleteByServiceId(dslContext, serviceId)
                extServiceItemRelDao.batchAdd(
                    dslContext = dslContext,
                    userId = userId,
                    serviceId = serviceId,
                    itemPropList = itemCreateInfoList
                )

                // 添加扩展点使用记录
                client.get(ServiceItemResource::class).addServiceNum(itemIds)
            }
        }

        mediaService.deleteByStoreCode(userId, serviceCode, StoreTypeEnum.SERVICE)
        infoResp.mediaInfo?.forEach {
            storeMediaInfoDao.add(
                dslContext = dslContext,
                userId = userId,
                type = StoreTypeEnum.SERVICE.type.toByte(),
                id = UUIDUtil.generate(),
                storeMediaInfoReq = StoreMediaInfoRequest(
                    storeCode = serviceCode,
                    mediaUrl = it.mediaUrl,
                    mediaType = it.mediaType.name,
                    modifier = userId
                )
            )
        }

        if (settingInfo != null) {
            extFeatureDao.updateExtServiceFeatureBaseInfo(
                dslContext = dslContext,
                userId = userId,
                serviceCode = serviceCode,
                extServiceFeatureUpdateInfo = ExtServiceFeatureUpdateInfo(
                    publicFlag = settingInfo.publicFlag,
                    recommentFlag = settingInfo.recommendFlag,
                    certificationFlag = settingInfo.certificationFlag,
                    weight = settingInfo.weight,
                    modifierUser = userId,
                    serviceTypeEnum = settingInfo.type,
                    descInputType = baseInfo?.descInputType ?: DescInputTypeEnum.MANUAL
                )
            )
        }

        return Result(true)
    }

    private fun checkProjectInfo(userId: String, projectCode: String) {
        val permissionCheck = permissionApi.validateUserResourcePermission(
            user = userId,
            projectCode = projectCode,
            serviceCode = bsProjectServiceCodec,
            resourceType = AuthResourceType.PIPELINE_DEFAULT,
            resourceCode = "*",
            permission = AuthPermission.CREATE
        )
        if (!permissionCheck) {
            throw ErrorCodeException(errorCode = StoreMessageCode.USER_SERVICE_PROJECT_NOT_PERMISSION)
        }
        val projectInfo = client.get(ServiceProjectResource::class).get(projectCode).data
        if (projectInfo == null) {
            throw ErrorCodeException(errorCode = StoreMessageCode.USER_SERVICE_PROJECT_UNENABLE)
        } else {
            if (projectInfo.enabled == false) {
                throw ErrorCodeException(errorCode = StoreMessageCode.USER_SERVICE_PROJECT_UNENABLE)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExtServiceBaseService::class.java)
    }
}
