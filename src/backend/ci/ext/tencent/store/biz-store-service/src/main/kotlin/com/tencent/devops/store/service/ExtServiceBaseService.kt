package com.tencent.devops.store.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.APPROVE
import com.tencent.devops.common.api.constant.BEGIN
import com.tencent.devops.common.api.constant.BUILD
import com.tencent.devops.common.api.constant.COMMIT
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.DOING
import com.tencent.devops.common.api.constant.END
import com.tencent.devops.common.api.constant.FAIL
import com.tencent.devops.common.api.constant.NUM_FIVE
import com.tencent.devops.common.api.constant.NUM_FOUR
import com.tencent.devops.common.api.constant.NUM_ONE
import com.tencent.devops.common.api.constant.NUM_SIX
import com.tencent.devops.common.api.constant.NUM_THREE
import com.tencent.devops.common.api.constant.NUM_TWO
import com.tencent.devops.common.api.constant.SUCCESS
import com.tencent.devops.common.api.constant.TEST
import com.tencent.devops.common.api.constant.UNDO
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.api.pojo.ServiceItem
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.service.ServiceItemResource
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceEnvDao
import com.tencent.devops.store.dao.ExtServiceFeatureDao
import com.tencent.devops.store.dao.ExtServiceItemRelDao
import com.tencent.devops.store.dao.ExtServiceLableRelDao
import com.tencent.devops.store.dao.ExtServiceVersionLogDao
import com.tencent.devops.store.dao.common.StoreBuildInfoDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreReleaseDao
import com.tencent.devops.store.pojo.ExtServiceCreateInfo
import com.tencent.devops.store.pojo.ExtServiceEnvCreateInfo
import com.tencent.devops.store.pojo.ExtServiceFeatureCreateInfo
import com.tencent.devops.store.pojo.ExtServiceItemRelCreateInfo
import com.tencent.devops.store.pojo.ExtServiceUpdateInfo
import com.tencent.devops.store.pojo.ExtServiceVersionLogCreateInfo
import com.tencent.devops.store.pojo.ExtensionJson
import com.tencent.devops.store.pojo.ItemPropCreateInfo
import com.tencent.devops.store.pojo.StoreServiceItem
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.StoreMediaInfoRequest
import com.tencent.devops.store.pojo.common.StoreProcessInfo
import com.tencent.devops.store.pojo.common.StoreReleaseCreateRequest
import com.tencent.devops.store.pojo.common.UN_RELEASE
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServicePackageSourceTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.dto.ExtSubmitDTO
import com.tencent.devops.store.pojo.dto.InitExtServiceDTO
import com.tencent.devops.store.pojo.dto.ServiceOfflineDTO
import com.tencent.devops.store.pojo.dto.SubmitDTO
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import com.tencent.devops.store.pojo.vo.MyServiceVO
import com.tencent.devops.store.pojo.vo.MyExtServiceRespItem
import com.tencent.devops.store.pojo.vo.ServiceVersionListItem
import com.tencent.devops.store.pojo.vo.ServiceVersionListResp
import com.tencent.devops.store.pojo.vo.ServiceVersionVO
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.common.StoreMediaService
import com.tencent.devops.store.service.common.StoreUserService
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.time.LocalDateTime

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
    lateinit var storeReleaseDao: StoreReleaseDao
    @Autowired
    lateinit var extFeatureDao: ExtServiceFeatureDao
    @Autowired
    lateinit var extServiceItemRelDao: ExtServiceItemRelDao
    @Autowired
    lateinit var redisOperation: RedisOperation
    @Autowired
    lateinit var storeCommonService: StoreCommonService
    @Autowired
    lateinit var extServiceVersionLogDao: ExtServiceVersionLogDao
    @Autowired
    lateinit var extServiceLabelDao: ExtServiceLableRelDao
    @Autowired
    lateinit var client: Client
    @Autowired
    lateinit var storeUserService: StoreUserService
    @Autowired
    lateinit var serviceNotifyService: ExtServiceNotifyService
    @Autowired
    lateinit var storeCommentService: StoreCommentService
    @Autowired
    lateinit var mediaService: StoreMediaService
    @Autowired
    lateinit var deptService: StoreVisibleDeptService
    @Autowired
    lateinit var objectMapper: ObjectMapper

    fun addExtService(
        userId: String,
        extensionInfo: InitExtServiceDTO
    ): Result<Boolean> {
        val serviceCode = extensionInfo.serviceCode
        logger.info("addExtService user[$userId], serviceCode[$serviceCode], info[$extensionInfo]")
        // 校验信息
        validateAddServiceReq(userId, extensionInfo)
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
                    creatorUser = userId,
                    publisher = userId,
                    publishTime = System.currentTimeMillis(),
                    status = 0,
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
            extensionInfo.extensionItemList.forEach {
                // 添加扩展服务扩展点
                extServiceItemRelDao.create(
                    dslContext = dslContext,
                    userId = userId,
                    extServiceItemRelCreateInfo = ExtServiceItemRelCreateInfo(
                        serviceId = id,
                        itemId = it,
                        creatorUser = userId,
                        modifierUser = userId
                    )
                )
            }
            // 添加扩展点使用记录
            client.get(ServiceItemResource::class).addServiceNum(extensionInfo.extensionItemList)
        }
        return Result(true)
    }

    fun updateExtService(
        userId: String,
        submitDTO: SubmitDTO
    ): Result<String> {
        logger.info("updateExtService userId[$userId],submitDTO[$submitDTO]")
        val serviceCode = submitDTO.serviceCode
        val extPackageSourceType = getExtServicePackageSourceType(serviceCode)
        logger.info("updateExtService servicePackageSourceType is :$extPackageSourceType")
        val version = submitDTO.version

        // 判断扩展服务是不是首次创建版本
        val serviceRecords = extServiceDao.listServiceByCode(dslContext, serviceCode)
        logger.info("the serviceRecords is :$serviceRecords")
        if (null == serviceRecords || serviceRecords.size < 1) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(serviceCode)
            )
        }
        val serviceRecord = serviceRecords[0]
        // 判断更新的扩展服务名称是否重复
        if (validateAddServiceReqByName(
                submitDTO.serviceName,
                submitDTO.serviceCode
            )
        ) return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PARAMETER_IS_EXIST,
            arrayOf(submitDTO.serviceName)
        )
        // 校验前端传的版本号是否正确
        val releaseType = submitDTO.releaseType
        val dbVersion = serviceRecord!!.version
        // 最近的版本处于上架中止状态，重新升级版本号不变
        val cancelFlag = serviceRecord.serviceStatus == ExtServiceStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        val requireVersion =
            if (cancelFlag && releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE) dbVersion else storeCommonService.getRequireVersion(
                dbVersion,
                releaseType
            )

        if (version != requireVersion) {
            return MessageCodeUtil.generateResponseDataObject(
                // TODO: 需在core内添加新状态码
                StoreMessageCode.USER_ATOM_VERSION_IS_INVALID,
                arrayOf(version, requireVersion)
            )
        }

        // 判断最近一个插件版本的状态，只有处于审核驳回、已发布、上架中止和已下架的状态才允许添加新的版本
        val serviceFinalStatusList = mutableListOf(
            ExtServiceStatusEnum.AUDIT_REJECT.status.toByte(),
            ExtServiceStatusEnum.RELEASED.status.toByte(),
            ExtServiceStatusEnum.GROUNDING_SUSPENSION.status.toByte(),
            ExtServiceStatusEnum.UNDERCARRIAGED.status.toByte()
        )

        if (serviceRecords.size == 1) {
            // 如果是首次发布，处于初始化的插件状态也允许添加新的版本
            serviceFinalStatusList.add(ExtServiceStatusEnum.INIT.status.toByte())
        }

        if (!serviceFinalStatusList.contains(serviceRecord.serviceStatus)) {
            return MessageCodeUtil.generateResponseDataObject(
                // TODO: 需在core内添加新状态码
                StoreMessageCode.USER_ATOM_VERSION_IS_NOT_FINISH,
                arrayOf(serviceRecord.serviceName, serviceRecord.version)
            )
        }

        var serviceId = UUIDUtil.generate()
        val serviceStatus =
            if (extPackageSourceType == ExtServicePackageSourceTypeEnum.REPO) ExtServiceStatusEnum.COMMITTING else ExtServiceStatusEnum.TESTING

        dslContext.transaction { t ->
            val context = DSL.using(t)
            if (StringUtils.isEmpty(serviceRecord.version) || (cancelFlag && releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE)) {
                // 首次创建版本或者取消发布后不变更版本号重新上架，则在该版本的记录上做更新操作
                serviceId = serviceRecord.id
                val finalReleaseType = if (releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE) {
                    val serviceVersion = extServiceVersionLogDao.getVersionLogByServiceId(context, serviceId)
                    serviceVersion.releaseType
                } else {
                    releaseType.releaseType.toByte()
                }
                updateExtService(
                    userId = userId,
                    serviceId = serviceId,
                    extServiceUpdateInfo = ExtServiceUpdateInfo(
                        serviceName = submitDTO.serviceName,
                        version = submitDTO.version,
                        status = serviceStatus.status,
                        statusMsg = "",
                        logoUrl = submitDTO.logoUrl,
                        summary = submitDTO.sunmmary,
                        description = submitDTO.description,
                        latestFlag = true,
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
                // 升级插件
                upgradeMarketExtService(
                    userId = userId,
                    serviceId = serviceId,
                    extServiceCreateInfo = ExtServiceCreateInfo(
                        serviceCode = submitDTO.serviceCode,
                        serviceName = submitDTO.serviceName,
                        creatorUser = userId,
                        version = "1.0.0",
                        publisher = userId,
                        publishTime = System.currentTimeMillis(),
                        status = 0
                    ),
                    extServiceVersionLogCreateInfo = ExtServiceVersionLogCreateInfo(
                        serviceId = serviceId,
                        releaseType = submitDTO.releaseType.releaseType.toByte(),
                        content = submitDTO.versionContent ?: "",
                        creatorUser = userId,
                        modifierUser = userId
                    )
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
            if (null != itemIdList) {
                extServiceItemRelDao.deleteByServiceId(context, serviceId)
                extServiceItemRelDao.batchAdd(
                    dslContext = dslContext,
                    userId = userId,
                    serviceId = serviceId,
                    itemPropList = itemCreateInfoList!!
                )
            }
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
        logger.info("getProcessInfo record is $record")
        return if (null == record) {
            MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(serviceId))
        } else {
            val serviceCode = record.serviceCode
            // 判断用户是否有查询权限
            val queryFlag =
                storeMemberDao.isStoreMember(dslContext, userId, serviceCode, StoreTypeEnum.SERVICE.type.toByte())
            if (!queryFlag) {
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
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
        val serviceItemIdMap = mutableMapOf<String, List<String>>()
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
            val itemIds = mutableListOf<String>()
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

        val myService = mutableListOf<MyExtServiceRespItem?>()
        records?.forEach {
            val serviceCode = it["serviceCode"] as String
            val serviceId = it["serviceId"] as String
            var releaseFlag = false // 是否有处于上架状态的插件插件版本
            val releaseServiceNum = extServiceDao.countReleaseServiceByCode(dslContext, serviceCode)
            if (releaseServiceNum > 0) {
                releaseFlag = true
            }
            val language = extServiceEnvDao.getMarketServiceEnvInfoByServiceId(dslContext, serviceId)?.language
            val serviceItemList = serviceItemIdMap[serviceId]
            logger.info("the getMyService serviceId is :$serviceId, itemList is :$serviceItemList")
            var itemName = ""
            serviceItemList?.forEach { itId ->
                itemName += itemInfoMap?.get(itId)?.itemName + "，"
            }
            itemName.substring(0, itemName.length - 1)
            logger.info("the getMyService serviceId is :$serviceId, itemName is :$itemName")
            myService.add(
                MyExtServiceRespItem(
                    serviceId = serviceId,
                    serviceName = it["serviceName"] as String,
                    serviceCode = serviceCode,
                    version = it["version"] as String,
                    category = it["category"] as String,
                    logoUrl = it["logoUrl"] as String?,
                    serviceStatus = ExtServiceStatusEnum.getServiceStatus((it["serviceStatus"] as Byte).toInt()),
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
                    itemName = itemName,
                    releaseFlag = releaseFlag
                )
            )
        }
        return Result(MyServiceVO(count, page, pageSize, myService))
    }

    fun offlineService(userId: String, serviceCode: String, serviceOfflineDTO: ServiceOfflineDTO): Result<Boolean> {
        // 判断用户是否有权限下线
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, serviceCode, StoreTypeEnum.SERVICE.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        // 设置插件状态为下架中
        extServiceDao.setServiceStatusByCode(
            dslContext, serviceCode, ExtServiceStatusEnum.RELEASED.status.toByte(),
            ExtServiceStatusEnum.UNDERCARRIAGING.status.toByte(), userId, serviceOfflineDTO.reason
        )
        // 通过websocket推送状态变更消息
//        storeWebsocketService.sendWebsocketMessageByAtomCodeAndUserId(serviceCode, userId)
        // 通知使用方插件即将下架 -- todo

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

    fun getServiceVersionListByCode(serviceCode: String, userId: String): Result<ServiceVersionListResp> {
        logger.info("getServiceVersionListByCode serviceCode[$serviceCode]")
        val records = extServiceDao.listServiceByCode(dslContext, serviceCode)
        val serviceVersions = mutableListOf<ServiceVersionListItem?>()
        records?.forEach {
            serviceVersions.add(
                ServiceVersionListItem(
                    serviceId = it!!.id,
                    serviceCode = it.serviceCode,
                    serviceName = it.serviceName,
                    version = it.version,
                    serviceStatus = ExtServiceStatusEnum.getServiceStatus((it.serviceStatus as Byte).toInt()),
                    creator = it.creator,
                    createTime = DateTimeUtil.toDateTime(it.createTime)
                )
            )
        }
        logger.info("getServiceVersionListByCode serviceVersions[$serviceVersions]")
        return Result(ServiceVersionListResp(serviceVersions.size, serviceVersions))
    }

    /**
     * 取消发布
     */
    fun cancelRelease(userId: String, serviceId: String): Result<Boolean> {
        logger.info("extService cancelRelease, userId=$userId, serviceId=$serviceId")
        val status = ExtServiceStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        val (checkResult, code) = checkServiceVersionOptRight(userId, serviceId, status)
        if (!checkResult) {
            return MessageCodeUtil.generateResponseDataObject(code)
        }
        extServiceDao.setServiceStatusById(
            dslContext,
            serviceId,
            status,
            userId,
            MessageCodeUtil.getCodeLanMessage(UN_RELEASE)
        )
//        // 通过websocket推送状态变更消息
//        storeWebsocketService.sendWebsocketMessage(userId, serviceId)

        return Result(true)
    }

    /**
     * 确认通过测试，继续发布
     */
    fun passTest(userId: String, serviceId: String): Result<Boolean> {
        logger.info("passTest, userId=$userId, serviceId=$serviceId")
        val serviceRecord = extServiceDao.getServiceById(dslContext, serviceId)
        logger.info("passTest serviceRecord is:$serviceRecord")
        if (null == serviceRecord) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(serviceId),
                false
            )
        }
        // 查看当前版本之前的版本是否有已发布的，如果有已发布的版本则只是普通的升级操作而不需要审核
        val isNormalUpgrade = getNormalUpgradeFlag(serviceRecord.serviceCode, serviceRecord.serviceStatus.toInt())
        logger.info("passTest isNormalUpgrade is:$isNormalUpgrade")
        val serviceStatus = getPassTestStatus(isNormalUpgrade)
        val (checkResult, code) = checkServiceVersionOptRight(userId, serviceId, serviceStatus, isNormalUpgrade)
        if (!checkResult) {
            return MessageCodeUtil.generateResponseDataObject(code)
        }
        if (isNormalUpgrade) {
            val creator = serviceRecord.creator
            dslContext.transaction { t ->
                val context = DSL.using(t)
                // 清空旧版本LATEST_FLAG
                extServiceDao.cleanLatestFlag(context, serviceRecord.serviceCode)
                // 记录发布信息
                val pubTime = LocalDateTime.now()
                storeReleaseDao.addStoreReleaseInfo(
                    dslContext = context,
                    userId = userId,
                    storeReleaseCreateRequest = StoreReleaseCreateRequest(
                        storeCode = serviceRecord.serviceCode,
                        storeType = StoreTypeEnum.SERVICE,
                        latestUpgrader = creator,
                        latestUpgradeTime = pubTime
                    )
                )
                extServiceDao.updateExtServiceBaseInfo(
                    dslContext = dslContext,
                    userId = userId,
                    serviceId = serviceId,
                    extServiceUpdateInfo = ExtServiceUpdateInfo(
                        status = serviceStatus.toInt(), latestFlag = true, modifierUser = userId
                    )
                )
//                // 通过websocket推送状态变更消息
//                storeWebsocketService.sendWebsocketMessage(userId, serviceId)
            }
            // 发送版本发布邮件
            serviceNotifyService.sendAtomReleaseAuditNotifyMessage(serviceId, AuditTypeEnum.AUDIT_SUCCESS)
        } else {
            extServiceDao.setServiceStatusById(dslContext, serviceId, serviceStatus, userId, "")
//            // 通过websocket推送状态变更消息
//            storeWebsocketService.sendWebsocketMessage(userId, serviceId)
        }
        return Result(true)
    }

    fun rebuild(projectCode: String, userId: String, serviceId: String): Result<Boolean> {
        logger.info("rebuild, projectCode=$projectCode, userId=$userId, serviceId=$serviceId")
        // 判断是否可以启动构建
        val status = ExtServiceStatusEnum.BUILDING.status.toByte()
        val (checkResult, code) = checkServiceVersionOptRight(userId, serviceId, status)
        if (!checkResult) {
            return MessageCodeUtil.generateResponseDataObject(code)
        }
        // 拉取extension.json，检查格式，更新入库
        val serviceRecord = extServiceDao.getServiceById(dslContext, serviceId) ?: return Result(false)
        val serviceCode = serviceRecord.serviceCode
        val serviceName = serviceRecord.serviceName
        val serviceVersion = serviceRecord.version
        // TODO 此处等carl完善
        // todo 解析extension.json的配置项数据，然后更新数据库
        asyncHandleUpdateService(dslContext, serviceId, userId)
        return Result(true)
    }

    fun getPassTestStatus(isNormalUpgrade: Boolean): Byte {
        return if (isNormalUpgrade) ExtServiceStatusEnum.RELEASED.status.toByte() else ExtServiceStatusEnum.AUDITING.status.toByte()
    }

    fun createMediaAndVisible(userId: String, serviceCode: String, submitInfo: ExtSubmitDTO): Result<Boolean> {
        val mediaList = submitInfo.mediaInfoList
        val deptList = submitInfo.deptInfoList
        mediaList?.forEach {
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
            logger.info("getServiceVersion featureInfoRecord: $featureInfoRecord")

            val repositoryHashId = featureInfoRecord!!.repositoryHashId
            val flag =
                storeUserService.isCanInstallStoreComponent(defaultFlag, userId, serviceCode, StoreTypeEnum.SERVICE)
            val userCommentInfo =
                storeCommentService.getStoreUserCommentInfo(userId, serviceCode, StoreTypeEnum.SERVICE)
            val serviceEnv = extServiceEnvDao.getMarketServiceEnvInfoByServiceId(dslContext, serviceId)
            logger.info("getServiceVersion serviceEnv: $serviceEnv")
            val itemList = getItemByItems(serviceId)
            val mediaList = mediaService.getByCode(serviceCode, StoreTypeEnum.SERVICE).data
//            val labelList = extServiceLabelDao.getLabelsByServiceId(dslContext, serviceId)?: emptyList<Label>()

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
                    labelList = emptyList(),
                    userCommentInfo = userCommentInfo,
                    visibilityLevel = VisibilityLevelEnum.getVisibilityLevel(featureInfoRecord.visibilityLevel),
                    recommendFlag = featureInfoRecord?.recommendFlag,
                    extensionItemList = itemList,
                    mediaList = mediaList
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

    private fun updateExtService(
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

    private fun getItemByItems(serviceId: String): List<StoreServiceItem> {
        val serviceItems = extServiceItemRelDao.getItemByServiceId(dslContext, serviceId)
        val itemIds = mutableSetOf<String>()
        serviceItems?.forEach { it ->
            itemIds.add(it.itemId)
        }
        logger.info("getItemByItems serviceId[$serviceId] items[$itemIds]")
        val itemList = mutableListOf<StoreServiceItem>()
        client.get(ServiceItemResource::class).getItemListsByIds(itemIds).data?.forEach {
            val childItem = it.childItem?.get(0)
            itemList.add(
                StoreServiceItem(
                    parentItemCode = it.serviceItem.itemCode,
                    parentItemId = it.serviceItem.itemId,
                    parentItemName = it.serviceItem.itemName,
                    childItemCode = childItem.itemCode,
                    childItemId = childItem.itemId,
                    childItemName = childItem.itemName
                )
            )
        }
        logger.info("getItemByItems itemList[$itemList]")
        return itemList
    }

    private fun upgradeMarketExtService(
        userId: String,
        serviceId: String,
        extServiceCreateInfo: ExtServiceCreateInfo,
        extServiceVersionLogCreateInfo: ExtServiceVersionLogCreateInfo
    ) {
        extServiceDao.createExtService(
            dslContext = dslContext,
            userId = userId,
            id = serviceId,
            extServiceCreateInfo = extServiceCreateInfo
        )
        extServiceVersionLogDao.create(
            dslContext = dslContext,
            userId = userId,
            id = serviceId,
            extServiceVersionLogCreateInfo = extServiceVersionLogCreateInfo
        )
    }

    private fun validateAddServiceReq(
        userId: String,
        extensionInfo: InitExtServiceDTO
    ): Result<Boolean> {
        logger.info("the validateExtServiceReq userId is :$userId,info[$extensionInfo]")
        if (extensionInfo.serviceCode == null) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf("serviceCode"),
                false
            )
        }
        if (extensionInfo.serviceName == null) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf("serviceName"),
                false
            )
        }
        val serviceCode = extensionInfo.serviceCode
        // 判断扩展服务是否存在
        val codeInfo = extServiceDao.getServiceLatestByCode(dslContext, serviceCode)
        if (codeInfo != null) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(serviceCode),
                false
            )
        }
        val serviceName = extensionInfo.serviceName
        val nameInfo = extServiceDao.getServiceByName(dslContext, serviceName)
        if (nameInfo != null) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(serviceName),
                false
            )
        }
        return Result(true)
    }

    private fun handleProcessInfo(isNormalUpgrade: Boolean, status: Int): List<ReleaseProcessItem> {
        val processInfo = initProcessInfo(isNormalUpgrade)
        val totalStep = if (isNormalUpgrade) NUM_FIVE else NUM_SIX
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

            ExtServiceStatusEnum.RELEASED.status -> {
                val currStep = if (isNormalUpgrade) NUM_FIVE else NUM_SIX
                storeCommonService.setProcessInfo(processInfo, totalStep, currStep, SUCCESS)
            }
        }
        return processInfo
    }

    private fun initProcessInfo(isNormalUpgrade: Boolean): List<ReleaseProcessItem> {
        val processInfo = mutableListOf<ReleaseProcessItem>()
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(BEGIN), BEGIN, NUM_ONE, SUCCESS))
        processInfo.add(ReleaseProcessItem("构建版本", BUILD, NUM_TWO, UNDO))
        processInfo.add(ReleaseProcessItem("版本测试", TEST, NUM_THREE, UNDO))
        processInfo.add(ReleaseProcessItem("填写相关信息", COMMIT, NUM_FOUR, UNDO))
        if (isNormalUpgrade) {
            processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(END), END, NUM_FIVE, UNDO))
        } else {
            processInfo.add(ReleaseProcessItem("版本审核", APPROVE, NUM_FIVE, UNDO))
            processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(END), END, NUM_SIX, UNDO))
        }
        return processInfo
    }

    // TODO 此处需调整为： 同一个名称只支持在同一个服务内相同
    private fun validateAddServiceReqByName(serviceName: String, serviceCode: String): Boolean {
        // 判断扩展服务是否存在
        val nameInfo = extServiceDao.listServiceByName(dslContext, serviceName)
        if (nameInfo != null) {
            for (code in nameInfo) {
                if (serviceCode != code!!.serviceCode) {
                    return true
                }
            }
        }
        return false
    }

    private fun getFileServiceProps(
        serviceCode: String,
        repositoryHashId: String,
        fileName: String,
        inputItemList : Set<String>
    ): List<ItemPropCreateInfo> {
        val itemCreateList = mutableListOf<ItemPropCreateInfo>()
        val inputItemMap = mutableMapOf<String, ItemPropCreateInfo>()
        // 页面属于与文件内的itemCode需取交集。重复以文件内的为准
        inputItemList.forEach {
            inputItemMap[it] = ItemPropCreateInfo(
                    itemId = it,
                    props = ""
                )
        }
        // 从工蜂拉取文件
        val fileStr = client.get(ServiceGitRepositoryResource::class).getFileContent(
            repositoryHashId,
            fileName, null, null, null
        ).data
        logger.info("getFileStr fileStr is:$fileStr")
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
        }

        if (fileItemList == null) {
            // 文件数据为空，直接返回输入数据
            return returnInputData(inputItemList)
        }

        // 文件与输入itemCode取交集，若文件内有，已文件props为准
        val itemCodeList = mutableSetOf<String>()
        val filePropMap = mutableMapOf<String, String>()
        fileItemList!!.forEach {
            itemCodeList.add(it.itemCode!!)
            filePropMap[it.itemCode] = it.props.toString()
        }
        logger.info("getServiceProps fileItemList[$fileItemList], filePropMap[$filePropMap], inputItemMap[$inputItemMap]")
        val itemRecords =
            client.get(ServiceItemResource::class).getItemByCodes(itemCodeList).data ?: return mutableListOf()
        itemRecords.forEach {
            if (filePropMap.containsKey(it.itemCode)) {
                inputItemMap[it.itemId] =
                    ItemPropCreateInfo(
                        itemId = it.itemId,
                        props = filePropMap[it.itemCode] ?: ""
                    )

            }
        }
        // 返回取完交集后的数据
        inputItemMap.forEach { (t, u) ->
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
                    props = ""
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
        logger.info("checkAtomVersionOptRight, userId=$userId, serviceId=$serviceId, status=$status, isNormalUpgrade=$isNormalUpgrade")
        val record =
            extServiceDao.getServiceById(dslContext, serviceId) ?: return Pair(
                false,
                CommonMessageCode.PARAMETER_IS_INVALID
            )
        val servcieCode = record.serviceCode
        val creator = record.creator
        val recordStatus = record.serviceStatus

        // 判断用户是否有权限(当前版本的创建者和管理员可以操作)
        if (!(storeMemberDao.isStoreAdmin(
                dslContext,
                userId,
                servcieCode,
                StoreTypeEnum.SERVICE.type.toByte()
            ) || creator == userId)
        ) {
            return Pair(false, CommonMessageCode.PERMISSION_DENIED)
        }
        logger.info("record status=$recordStatus, status=$status")
        val allowReleaseStatus = if (isNormalUpgrade != null && isNormalUpgrade) ExtServiceStatusEnum.TESTING
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
            recordStatus != ExtServiceStatusEnum.BUILDING.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ExtServiceStatusEnum.AUDITING.status.toByte() &&
            recordStatus != ExtServiceStatusEnum.TESTING.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ExtServiceStatusEnum.AUDIT_REJECT.status.toByte() &&
            recordStatus != ExtServiceStatusEnum.AUDITING.status.toByte()
        ) {
            validateFlag = false
        } else if (status == ExtServiceStatusEnum.RELEASED.status.toByte() &&
            recordStatus != allowReleaseStatus.status.toByte()
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

        return if (validateFlag) Pair(true, "") else Pair(false, StoreMessageCode.USER_ATOM_RELEASE_STEPS_ERROR)
    }

    companion object {
        val logger = LoggerFactory.getLogger(ExtServiceBaseService::class.java)
        const val EXTENSION_JSON_NAME = "extension.json"
    }
}