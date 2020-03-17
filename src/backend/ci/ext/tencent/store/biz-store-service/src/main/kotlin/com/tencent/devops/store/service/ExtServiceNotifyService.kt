package com.tencent.devops.store.service

import com.tencent.devops.common.api.constant.DEVOPS
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceVersionLogDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.EXTENSION_RELEASE_AUDIT_PASS_TEMPLATE
import com.tencent.devops.store.pojo.common.EXTENSION_RELEASE_AUDIT_REFUSE_TEMPLATE
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ExtServiceNotifyService {

    private val logger = LoggerFactory.getLogger(ExtServiceNotifyService::class.java)

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var serviceDao: ExtServiceDao

    @Autowired
    private lateinit var serviceVersionLogDao: ExtServiceVersionLogDao

    @Autowired
    private lateinit var storeMemberDao: StoreMemberDao

    @Autowired
    private lateinit var client: Client

    @Value("\${store.serviceDetailBaseUrl}")
    private lateinit var serviceDetailBaseUrl: String

    /**
     * 发送扩展服务发布审核结果通知消息
     * @param serviceId 插件ID
     * @param auditType 审核类型
     */
    fun sendAtomReleaseAuditNotifyMessage(serviceId: String, auditType: AuditTypeEnum) {
        val serviceRecord = serviceDao.getServiceById(dslContext, serviceId) ?: return
        // 查出版本日志
        val serviceVersionLogRecord = serviceVersionLogDao.getVersionLogByServiceId(dslContext, serviceId)
        val serviceCode = serviceRecord.serviceCode
        val serviceName = serviceRecord.serviceName
        val titleParams = mapOf(
            "name" to serviceName,
            "version" to serviceRecord.version
        )
        val releaseType = serviceVersionLogRecord.releaseType
        val bodyParams = mapOf(
            "name" to serviceName,
            "version" to serviceRecord.version,
            "publisher" to serviceRecord.publisher,
            "releaseType" to if (releaseType != null) MessageCodeUtil.getCodeLanMessage(
                "RELEASE_TYPE_" + ReleaseTypeEnum.getReleaseType(releaseType.toInt())
            ) else "",
            "versionDesc" to (serviceVersionLogRecord.content ?: ""),
            "nameInBody" to serviceName,
            "serviceStatusMsg" to serviceRecord.serviceStatusMsg,
            "url" to serviceDetailBaseUrl + serviceCode
        )
        val creator = serviceRecord.creator
        val receiver: String = creator
        val ccs = mutableSetOf(creator)
        if (auditType == AuditTypeEnum.AUDIT_SUCCESS) {
            val serviceAdminRecords = storeMemberDao.list(
                dslContext = dslContext,
                storeCode = serviceCode,
                type = StoreMemberTypeEnum.ADMIN.type.toByte(),
                storeType = StoreTypeEnum.SERVICE.type.toByte()
            )
            serviceAdminRecords?.map {
                ccs.add(it.username)
            }
        }
        val receivers = mutableSetOf(receiver)
        val templateCode = when (auditType) {
            AuditTypeEnum.AUDIT_SUCCESS -> {
                EXTENSION_RELEASE_AUDIT_PASS_TEMPLATE
            }
            AuditTypeEnum.AUDIT_REJECT -> {
                EXTENSION_RELEASE_AUDIT_REFUSE_TEMPLATE
            }
            else -> {
                EXTENSION_RELEASE_AUDIT_REFUSE_TEMPLATE
            }
        }
        val sendNotifyMessageTemplateRequest = SendNotifyMessageTemplateRequest(
            templateCode = templateCode,
            sender = DEVOPS,
            receivers = receivers,
            cc = receivers,
            titleParams = titleParams,
            bodyParams = bodyParams
        )
        val sendNotifyResult = client.get(ServiceNotifyMessageTemplateResource::class)
            .sendNotifyMessageByTemplate(sendNotifyMessageTemplateRequest)
        logger.info("sendNotifyResult is:$sendNotifyResult")
    }
}