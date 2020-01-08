package com.tencent.devops.store.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceEnvDao
import com.tencent.devops.store.dao.ExtServiceFeatureDao
import com.tencent.devops.store.dao.ExtServiceItemRelDao
import com.tencent.devops.store.dao.ExtServiceLableRelDao
import com.tencent.devops.store.dao.ExtServiceVersionLogDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.ExtServiceCreateInfo
import com.tencent.devops.store.pojo.ExtServiceEnvCreateInfo
import com.tencent.devops.store.pojo.ExtServiceFeatureCreateInfo
import com.tencent.devops.store.pojo.ExtServiceItemRelCreateInfo
import com.tencent.devops.store.pojo.ExtServiceUpdateInfo
import com.tencent.devops.store.pojo.ExtServiceVersionLogCreateInfo
import com.tencent.devops.store.pojo.enums.ExtServicePackageSourceTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.dto.InitExtServiceDTO
import com.tencent.devops.store.pojo.dto.SubmitDTO
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import com.tencent.devops.store.service.common.StoreCommonService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils

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

    fun addExtService(
        userId: String,
        serviceCode: String,
        extensionInfo: InitExtServiceDTO
    ): Result<Boolean> {
        logger.info("addExtService user[$userId], serviceCode[$serviceCode], info[$extensionInfo]")
        //校验信息
        validateAddServiceReq(userId, extensionInfo)
        val handleAtomPackageResult = handleAtomPackage(extensionInfo, userId, serviceCode)
        logger.info("addExtService the handleAtomPackageResult is :$handleAtomPackageResult")
        if (handleAtomPackageResult.isNotOk()) {
            return Result(handleAtomPackageResult.status, handleAtomPackageResult.message, null)
        }
        val handleAtomPackageMap = handleAtomPackageResult.data
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
                    creatorUser = extensionInfo.creatorUser,
                    version = "1.0.0",
                    publisher = userId,
                    publishTime = System.currentTimeMillis(),
                    status = 0
                )
            )
            // 添加插件与项目关联关系，type为0代表新增插件时关联的初始化项目
            storeProjectRelDao.addStoreProjectRel(
                dslContext = context,
                userId = userId,
                storeCode = serviceCode,
                projectCode = extensionInfo.itemCode,
                type = StoreProjectTypeEnum.INIT.type.toByte(),
                storeType = StoreTypeEnum.SERVICE.type.toByte()
            )
            storeProjectRelDao.addStoreProjectRel(
                dslContext = context,
                userId = userId,
                storeCode = serviceCode,
                projectCode = extensionInfo.itemCode,
                type = StoreProjectTypeEnum.TEST.type.toByte(),
                storeType = StoreTypeEnum.SERVICE.type.toByte()
            )
            val extServiceEnvCreateInfo = ExtServiceEnvCreateInfo(
                serviceId = id,
                language = "",
                pkgPath = "",
                pkgShaContent = "",
                dockerFileContent = "",
                imagePath = "",
                imageCmd = "",
                frontentEntryFile = "",
                creatorUser = userId,
                modifierUser = userId
            )
            extServiceEnvDao.create(context, id, extServiceEnvCreateInfo) // 添加流水线插件执行环境信息
            // 默认给新建插件的人赋予管理员权限
            storeMemberDao.addStoreMember(
                dslContext = context,
                userId = userId,
                storeCode = serviceCode,
                userName = userId,
                type = StoreMemberTypeEnum.ADMIN.type.toByte(),
                storeType = StoreTypeEnum.SERVICE.type.toByte()
            )
            // 添加插件特性信息
            extFeatureDao.create(
                dslContext = context,
                userId = userId,
                id = id,
                extServiceFeatureCreateInfo = ExtServiceFeatureCreateInfo(
                    serviceCode = serviceCode,
                    repositoryHashId = handleAtomPackageMap?.get("repositoryHashId") ?: "",
                    codeSrc = handleAtomPackageMap?.get("codeSrc") ?: "",
                    creatorUser = userId,
                    modifierUser = userId
                )
            )
            extServiceItemRelDao.create(
                dslContext = dslContext,
                userId = userId,
                id = id,
                extServiceItemRelCreateInfo = ExtServiceItemRelCreateInfo(
                    serviceId = id,
                    itemId = extensionInfo.itemId,
                    creatorUser = userId,
                    modifierUser = userId
                )
            )
        }
        return Result(true)
    }

    fun updateExtService(
        userId: String,
        projectCode: String,
        submitDTO: SubmitDTO
    ): Result<String?> {
        logger.info("updateExtService userId[$userId],submitDTO[$submitDTO]")
        val serviceCode = submitDTO.serviceCode
        val extPackageSourceType = getExtServicePackageSourceType(serviceCode)
        logger.info("updateExtService atomPackageSourceType is :$extPackageSourceType")
        val version = submitDTO.version

        // 判断扩展服务是不是首次创建版本
        val serviceRecord = extServiceDao.getServiceByCode(dslContext, serviceCode)
        logger.info("the serviceRecords is :$serviceRecord")
        if (null == serviceRecord) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(serviceCode)
            )
        }
        // 判断更新的插件名称是否重复
        if (validateAddServiceReqByName(submitDTO.serviceName).data!!) return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PARAMETER_IS_EXIST,
            arrayOf(submitDTO.serviceName)
        )
        // 校验前端传的版本号是否正确
        val releaseType = submitDTO.releaseType
        val dbVersion = serviceRecord.version
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
        val serviceStatus = if (extPackageSourceType == ExtServicePackageSourceTypeEnum.REPO) ExtServiceStatusEnum.COMMITTING else ExtServiceStatusEnum.TESTING

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
                        releaseType = submitDTO.releaseType.releaseType,
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
                        releaseType = submitDTO.releaseType.releaseType,
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

    abstract fun handleAtomPackage(
        extensionInfo: InitExtServiceDTO,
        userId: String,
        serviceCode: String
    ): Result<Map<String, String>?>

    abstract fun getExtServicePackageSourceType(serviceCode: String): ExtServicePackageSourceTypeEnum

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

        validateAddServiceReqByName(extensionInfo.serviceName)
        return Result(true)
    }

    private fun validateAddServiceReqByName(serviceName: String): Result<Boolean> {
        // 判断扩展服务是否存在
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

    companion object {
        val logger = LoggerFactory.getLogger(ExtServiceBaseService::class.java)
    }
}