package com.tencent.devops.store.service

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
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.service.ServiceItemResource
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
import com.tencent.devops.store.pojo.ExtServiceCreateInfo
import com.tencent.devops.store.pojo.ExtServiceEnvCreateInfo
import com.tencent.devops.store.pojo.ExtServiceFeatureCreateInfo
import com.tencent.devops.store.pojo.ExtServiceItemRelCreateInfo
import com.tencent.devops.store.pojo.ExtServiceUpdateInfo
import com.tencent.devops.store.pojo.ExtServiceVersionLogCreateInfo
import com.tencent.devops.store.pojo.StoreServiceItem
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.StoreProcessInfo
import com.tencent.devops.store.pojo.enums.ExtServicePackageSourceTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.dto.InitExtServiceDTO
import com.tencent.devops.store.pojo.dto.ServiceOfflineDTO
import com.tencent.devops.store.pojo.dto.SubmitDTO
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import com.tencent.devops.store.pojo.vo.MyServiceVO
import com.tencent.devops.store.pojo.vo.MyExtServiceRespItem
import com.tencent.devops.store.pojo.vo.ServiceVersionVO
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.common.StoreUserService
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
    lateinit var storeCommentService: StoreCommentService


    fun addExtService(
        userId: String,
        extensionInfo: InitExtServiceDTO
    ): Result<Boolean> {
        val serviceCode = extensionInfo.serviceCode
        logger.info("addExtService user[$userId], serviceCode[$serviceCode], info[$extensionInfo]")
        //校验信息
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
                frontentEntryFile = "",
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
        }
        return Result(true)
    }

    fun updateExtService(
        userId: String,
        projectCode: String,
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
        if (null == serviceRecords) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(serviceCode)
            )
        }
        val serviceRecord = serviceRecords[0]
        // 判断更新的插件名称是否重复
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
                //TODO: 需在core内添加新状态码
                StoreMessageCode.USER_ATOM_VERSION_IS_INVALID,
                arrayOf(version, requireVersion)
            )
        }

        // 判断最近一个插件版本的状态，只有处于审核驳回、已发布、上架中止和已下架的状态才允许添加新的版本
        val serviceFinalStatusList = listOf(
            ExtServiceStatusEnum.AUDIT_REJECT.status.toByte(),
            ExtServiceStatusEnum.RELEASED.status.toByte(),
            ExtServiceStatusEnum.GROUNDING_SUSPENSION.status.toByte(),
            ExtServiceStatusEnum.UNDERCARRIAGED.status.toByte()
        )
        if (!serviceFinalStatusList.contains(serviceRecord.serviceStatus)) {
            return MessageCodeUtil.generateResponseDataObject(
                //TODO: 需在core内添加新状态码
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
                    val atomVersion = extServiceVersionLogDao.getVersionLogByServiceId(context, serviceId)
                    atomVersion.releaseType
                } else {
                    releaseType.releaseType.toByte()
                }
                updateExtService(
                    userId = userId,
                    serviceId = serviceId,
                    extServiceUpdateInfo = ExtServiceUpdateInfo(
                        serviceName = submitDTO.serviceName,
                        sunmmary = submitDTO.sunmmary,
                        description = submitDTO.description,
                        logoUrl = submitDTO.logoUrl,
                        modifierUser = userId,
                        status = serviceStatus.status,
                        statusMsg = "",
                        deleteFlag = false,
                        latestFlag = true
                    ),
                    extServiceVersionLogCreateInfo = ExtServiceVersionLogCreateInfo(
                        serviceId = serviceId,
                        releaseType = finalReleaseType,
                        content = submitDTO.versionContent,
                        creatorUser = userId,
                        modifierUser = userId
                    )
                )
            } else {
                // 升级插件
                upgradeMarketAtom(
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
                        content = submitDTO.versionContent,
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
            //TODO: 此处等carl完善
            //asyncHandleUpdateAtom(context, serviceId, userId)
        }
        return Result(serviceId)
    }

    fun getProcessInfo(userId: String, serviceId: String): Result<StoreProcessInfo> {
        logger.info("getProcessInfo userId is $userId,serviceId is $serviceId")
        val record = extServiceDao.getServiceById(dslContext, serviceId)
        logger.info("getProcessInfo record is $record")
        return if (null == record) {
            MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(serviceId))
        } else {
            val serviceCode = record.serviceCode
            // 判断用户是否有查询权限
            val queryFlag = storeMemberDao.isStoreMember(dslContext, userId, serviceCode, StoreTypeEnum.SERVICE.type.toByte())
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
        records?.forEach {
            val testProjectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                dslContext,
                userId,
                it["serviceCode"] as String,
                StoreTypeEnum.SERVICE
            )
            if (null != testProjectCode) projectCodeList.add(testProjectCode)
        }

        logger.info("the getMyService userId is :$userId,projectCodeList is :$projectCodeList")
        val projectMap = client.get(ServiceProjectResource::class).getNameByCode(projectCodeList.joinToString(",")).data
        logger.info("the getMyService userId is :$userId,projectMap is :$projectMap")
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
                    releaseFlag = releaseFlag
                )
            )
        }
        return Result(MyServiceVO(count, page, pageSize, myService))
    }

    fun offlineService(userId: String, serviceCode: String, serviceOfflineDTO: ServiceOfflineDTO): Result<Boolean>{
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
            val flag = storeUserService.isCanInstallStoreComponent(defaultFlag , userId, serviceCode, StoreTypeEnum.SERVICE)
            val serviceEnv = extServiceEnvDao.getMarketServiceEnvInfoByServiceId(dslContext, serviceId)
            logger.info("getServiceVersion serviceEnv: $serviceEnv")
            val itemList = getItemByItems(serviceId)

            Result(
                ServiceVersionVO(
                    serviceId = serviceId,
                    serviceCode = serviceCode,
                    serviceName = record.serviceName,
                    logoUrl = record.logoUrl,
//                    classifyCode = classifyCode,
//                    classifyName = classifyLanName,
//                    category = AtomCategoryEnum.getAtomCategory((record["category"] as Byte).toInt()),
                    summary = record.summary ?: "",
                    description = record.description ?: "",
                    version = record.version,
                    serviceStatus = ExtServiceStatusEnum.getServiceStatus((record.serviceStatus).toInt()),
//                    releaseType = if (record["releaseType"] != null) ReleaseTypeEnum.getReleaseType((record["releaseType"] as Byte).toInt()) else null,
//                    versionContent = record["versionContent"] as? String,
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
                    // TODO:带补充逻辑
//                    labelList = null,
//                    userCommentInfo = userCommentInfo,
                    visibilityLevel = VisibilityLevelEnum.getVisibilityLevel(featureInfoRecord.visibilityLevel),
                    recommendFlag = featureInfoRecord?.recommendFlag,
                    itemListStore = itemList
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
        //TODO: 此处等carl完善
//        extServiceEnvUpdateInfo: ExtServiceEnvUpdateInfo
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
//        extServiceEnvDao.updateExtServiceEnvInfo(
//            dslContext = dslContext,
//            userId = userId,
//            serviceId = serviceId,
//            extServiceEnvUpdateInfo = extServiceEnvUpdateInfo
//        )
    }

    private fun getItemByItems(serviceId: String) : List<StoreServiceItem>{
        val serviceItems = extServiceItemRelDao.getItemByServiceId(dslContext, serviceId)
        val itemIds = mutableListOf<String>()
        serviceItems?.forEach { it ->
            itemIds.add(it.id)
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

    private fun upgradeMarketAtom(
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
        //TODO: 此处等carl完善
//        extServiceEnvDao.create(context, atomId, atomEnvRequest)
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
        val codeInfo = extServiceDao.getServiceByCode(dslContext, serviceCode)
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
            ExtServiceStatusEnum.INIT.status, ExtServiceStatusEnum.COMMITTING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_TWO, DOING)
            }
            ExtServiceStatusEnum.BUILDING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, DOING)
            }
            ExtServiceStatusEnum.BUILD_FAIL.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, FAIL)
            }
            ExtServiceStatusEnum.TESTING.status -> {
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
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(COMMIT), COMMIT, NUM_TWO, UNDO))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(BUILD), BUILD, NUM_THREE, UNDO))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(TEST), TEST, NUM_FOUR, UNDO))
        if (isNormalUpgrade) {
            processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(END), END, NUM_FIVE, UNDO))
        } else {
            processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(APPROVE), APPROVE, NUM_FIVE, UNDO))
            processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(END), END, NUM_SIX, UNDO))
        }
        return processInfo
    }

    //TODO 此处需调整为： 同一个名称只支持在同一个服务内相同
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

    companion object {
        val logger = LoggerFactory.getLogger(ExtServiceBaseService::class.java)
    }
}