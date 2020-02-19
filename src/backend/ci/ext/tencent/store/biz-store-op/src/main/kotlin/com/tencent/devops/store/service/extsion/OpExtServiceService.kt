package com.tencent.devops.store.service.extsion

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.common.StoreReleaseDao
import com.tencent.devops.store.pojo.common.PASS
import com.tencent.devops.store.pojo.common.REJECT
import com.tencent.devops.store.pojo.common.StoreReleaseCreateRequest
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import com.tencent.devops.store.pojo.dto.ServiceApproveReq
import com.tencent.devops.store.pojo.vo.ExtServiceInfoResp
import com.tencent.devops.store.pojo.vo.ExtensionServiceVO
import com.tencent.devops.store.service.ExtServiceNotifyService
import org.jooq.impl.DSL
import org.jooq.impl.DefaultDSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OpExtServiceService @Autowired constructor(
    private val extServiceDao: ExtServiceDao,
    private val storeReleaseDao: StoreReleaseDao,
    private val dslContext: DefaultDSLContext,
    private val serviceNotifyService: ExtServiceNotifyService
) {

    fun queryServiceList(
        serviceName: String?,
        itemId: String?,
        lableId: String?,
        serviceStatus: ExtServiceStatusEnum?,
        sortType: String?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<ExtServiceInfoResp?> {
        val serviceRecords = extServiceDao.getOpPipelineServices(
            dslContext = dslContext,
            serviceName = serviceName,
            itemId = itemId,
            lableId = lableId,
            serviceStatus = serviceStatus,
            sortType = sortType,
            desc = desc,
            page = page,
            pageSize = pageSize
        )
        val extensionServiceInfoList = mutableListOf<ExtensionServiceVO>()
        serviceRecords?.forEach {
            extensionServiceInfoList.add(
                ExtensionServiceVO(
                    serviceId =  it.id,
                    serviceName = it.serviceName,
                    serviceCode = it.serviceCode,
                    serviceStatus = it.serviceStatus.toInt(),
                    version = it.version,
                    publisher = it.publisher,
                    // TODO: 添加调试项目
                    projectCode = it.serviceCode,
                    deleteFlag = it.deleteFlag,
                    modifierTime = DateTimeUtil.toDateTime(it.updateTime as LocalDateTime)
                )
            )
        }
        return Result(ExtServiceInfoResp(serviceRecords.size, page, pageSize, extensionServiceInfoList))
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
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(serviceId))
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
            extServiceDao.approveServiceFromOp(context, userId, serviceId, serviceStatus, approveReq, latestFlag, pubTime)
        }
//        // 通过websocket推送状态变更消息,推送所有有该插件权限的用户
//        storeWebsocketService.sendWebsocketMessageByAtomCodeAndAtomId(serviceCode, atomId)
        // 发送通知消息
        serviceNotifyService.sendAtomReleaseAuditNotifyMessage(serviceId, type)
        return Result(true)
    }
}