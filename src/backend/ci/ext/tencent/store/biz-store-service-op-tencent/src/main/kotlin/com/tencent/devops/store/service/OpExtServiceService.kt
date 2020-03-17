package com.tencent.devops.store.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.api.service.service.ServiceItemResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceFeatureDao
import com.tencent.devops.store.dao.ExtServiceItemRelDao
import com.tencent.devops.store.dao.common.StoreMediaInfoDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreReleaseDao
import com.tencent.devops.store.pojo.ExtServiceFeatureUpdateInfo
import com.tencent.devops.store.pojo.ExtServiceItemRelCreateInfo
import com.tencent.devops.store.pojo.ExtServiceUpdateInfo
import com.tencent.devops.store.pojo.OpEditInfoDTO
import com.tencent.devops.store.pojo.common.PASS
import com.tencent.devops.store.pojo.common.REJECT
import com.tencent.devops.store.pojo.common.StoreMediaInfoRequest
import com.tencent.devops.store.pojo.common.StoreReleaseCreateRequest
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import com.tencent.devops.store.pojo.dto.ServiceApproveReq
import com.tencent.devops.store.pojo.enums.ExtServiceSortTypeEnum
import com.tencent.devops.store.pojo.vo.ExtServiceInfoResp
import com.tencent.devops.store.pojo.vo.ExtensionServiceVO
import org.jooq.impl.DSL
import org.jooq.impl.DefaultDSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OpExtServiceService @Autowired constructor(
    private val extServiceDao: ExtServiceDao,
    private val extServiceFeatureDao: ExtServiceFeatureDao,
    private val storeReleaseDao: StoreReleaseDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storeMemberService: TxExtServiceMemberImpl,
    private val extServiceItemDao: ExtServiceItemRelDao,
    private val storeMediaInfoDao: StoreMediaInfoDao,
    private val dslContext: DefaultDSLContext,
    private val serviceNotifyService: ExtServiceNotifyService,
    private val client: Client
) {

    fun queryServiceList(
        serviceName: String?,
        itemId: String?,
        lableId: String?,
        isRecommend: Boolean?,
        isPublic: Boolean?,
        isApprove: Boolean?,
        sortType: String?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<ExtServiceInfoResp?> {
        val serviceRecords = extServiceDao.queryServicesFromOp(
            dslContext = dslContext,
            serviceName = serviceName,
            isPublic = isPublic,
            isRecommend = isRecommend,
            itemId = itemId,
            lableId = lableId,
            isApprove = isApprove,
            sortType = sortType ?: ExtServiceSortTypeEnum.UPDATE_TIME.sortType,
            desc = desc ?: true,
            page = page,
            pageSize = pageSize
        )

        val count = extServiceDao.queryCountFromOp(
            dslContext = dslContext,
            serviceName = serviceName,
            isPublic = isPublic,
            isRecommend = isRecommend,
            itemId = itemId,
            isApprove = isApprove
        )

        val extensionServiceInfoList = mutableSetOf<ExtensionServiceVO>()
        serviceRecords?.forEach {
            val serviceId = it["itemId"] as String

            extensionServiceInfoList.add(
                ExtensionServiceVO(
                    serviceId = serviceId,
                    serviceCode = it["serviceCode"] as String,
                    serviceName = it["serviceName"] as String,
                    serviceStatus = (it["serviceStatus"] as Byte).toInt(),
                    publisher = it["publisher"] as String,
                    projectCode = it["projectCode"] as String,
                    modifierTime = (it["updateTime"] as LocalDateTime).timestamp().toString(),
                    version = it["version"] as String
                )
            )
        }

        return Result(ExtServiceInfoResp(count, page, pageSize, extensionServiceInfoList))
    }

    fun editExtInfo(userId: String, serviceId: String, serviceCode: String, infoResp: OpEditInfoDTO): Result<Boolean> {
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
                    summary = baseInfo.summary,
                    description = baseInfo.description,
                    modifierUser = userId,
                    status = null
                )
            )
            val itemIds = baseInfo.itemIds
            val lables = baseInfo.labels
            if (itemIds != null) {
                val existenceItems = extServiceItemDao.getItemByServiceId(dslContext, serviceId)
                val existenceItemIds = mutableListOf<String>()
                existenceItems?.forEach {
                    existenceItemIds.add(it.itemId)
                }
                itemIds.forEach { itemId ->
                    if (!existenceItemIds.contains(itemId)) {
                        extServiceItemDao.create(
                            userId = userId,
                            dslContext = dslContext,
                            extServiceItemRelCreateInfo = ExtServiceItemRelCreateInfo(
                                serviceId = serviceId,
                                modifierUser = userId,
                                creatorUser = userId,
                                itemId = itemId
                            )
                        )
                    }
                }
                // 添加扩展点使用记录
                client.get(ServiceItemResource::class).addServiceNum(itemIds)
            }
        }

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
            extServiceFeatureDao.updateExtServiceFeatureBaseInfo(
                dslContext = dslContext,
                userId = userId,
                serviceCode = serviceCode,
                extServiceFeatureUpdateInfo = ExtServiceFeatureUpdateInfo(
                    publicFlag = settingInfo.publicFlag,
                    recommentFlag = settingInfo.recommendFlag,
                    certificationFlag = settingInfo.certificationFlag,
                    modifierUser = userId
                )
            )
        }

        return Result(true)
    }

    fun approveService(userId: String, serviceId: String, approveReq: ServiceApproveReq): Result<Boolean> {
        // 判断扩展服务是否存在
        val serviceRecord = extServiceDao.getServiceById(dslContext, serviceId)
            ?: return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(serviceId)
            )

        val oldStatus = serviceRecord.serviceStatus
        if (oldStatus != ExtServiceStatusEnum.AUDITING.status.toByte()) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(serviceId)
            )
        }

        if (approveReq.result != PASS && approveReq.result != REJECT) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(approveReq.result)
            )
        }
        val creator = serviceRecord.creator
        val serviceCode = serviceRecord.serviceCode
        val serviceStatus =
            if (approveReq.result == PASS) {
                ExtServiceStatusEnum.RELEASED.status.toByte()
            } else {
                ExtServiceStatusEnum.AUDIT_REJECT.status.toByte()
            }
        val type = if (approveReq.result == PASS) AuditTypeEnum.AUDIT_SUCCESS else AuditTypeEnum.AUDIT_REJECT

        dslContext.transaction { t ->
            val context = DSL.using(t)
            val latestFlag = approveReq.result == PASS
            var pubTime: LocalDateTime? = null
            if (latestFlag) {
                pubTime = LocalDateTime.now()
//                // 清空旧版本LATEST_FLAG
//                marketAtomDao.cleanLatestFlag(context, approveReq.serviceCode)
                // 记录发布信息
                storeReleaseDao.addStoreReleaseInfo(
                    dslContext = context,
                    userId = userId,
                    storeReleaseCreateRequest = StoreReleaseCreateRequest(
                        storeCode = serviceCode,
                        storeType = StoreTypeEnum.SERVICE,
                        latestUpgrader = creator,
                        latestUpgradeTime = pubTime
                    )
                )
            }

            // 入库信息，并设置当前版本的LATEST_FLAG
            extServiceDao.approveServiceFromOp(
                context,
                userId,
                serviceId,
                serviceStatus,
                approveReq,
                latestFlag,
                pubTime
            )
            extServiceFeatureDao.updateExtServiceFeatureBaseInfo(
                dslContext = dslContext,
                serviceCode = serviceCode,
                userId = userId,
                extServiceFeatureUpdateInfo = ExtServiceFeatureUpdateInfo(
                    weight = approveReq.weight,
                    recommentFlag = approveReq.recommendFlag,
                    publicFlag = approveReq.publicFlag,
                    certificationFlag = approveReq.certificationFlag
                )
            )
        }
//        // 通过websocket推送状态变更消息,推送所有有该插件权限的用户
//        storeWebsocketService.sendWebsocketMessageByAtomCodeAndAtomId(serviceCode, atomId)
        // 发送通知消息
        serviceNotifyService.sendAtomReleaseAuditNotifyMessage(serviceId, type)
        return Result(true)
    }

    fun deleteService(userId: String, serviceCode: String): Result<Boolean> {
        logger.info("deleteService userId: $userId , serviceCode: $serviceCode")
        val type = StoreTypeEnum.SERVICE.type.toByte()
        val isOwner = storeMemberService.isStoreAdmin(userId, serviceCode, StoreTypeEnum.SERVICE.type.toByte())
        if (!isOwner) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED, arrayOf())
        }
        val releasedCount = extServiceDao.countReleaseServiceByCode(dslContext, serviceCode)
        logger.info("releasedCount: $releasedCount")
        if (releasedCount > 0) {
            return MessageCodeUtil.generateResponseDataObject(
                // TODO: 此处应在core添加扩展服务相关异常信息
                StoreMessageCode.USER_ATOM_RELEASED_IS_NOT_ALLOW_DELETE,
                arrayOf()
            )
        }
        // 如果已经被安装到其他项目下使用，不能删除
        val installedCount = storeProjectRelDao.countInstalledProject(dslContext, serviceCode, type)
        logger.info("installedCount: $releasedCount")
        if (installedCount > 0) {
            return MessageCodeUtil.generateResponseDataObject(
                // TODO: 此处应在core添加扩展服务相关异常信息
                StoreMessageCode.USER_ATOM_USED_IS_NOT_ALLOW_DELETE,
                arrayOf()
            )
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            extServiceDao.deleteExtService(context, userId, serviceCode)
            extServiceFeatureDao.deleteExtFeatureService(context, userId, serviceCode)
        }
        return Result(true)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}