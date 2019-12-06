package com.tencent.devops.store.service.image.impl

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.dao.image.ImageVersionLogDao
import com.tencent.devops.store.pojo.common.IMAGE_RELEASE_AUDIT_PASS_TEMPLATE
import com.tencent.devops.store.pojo.common.IMAGE_RELEASE_AUDIT_REFUSE_TEMPLATE
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.service.image.ImageNotifyService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TxImageNotifyService @Autowired constructor(
    private val dslContext: DSLContext,
    private val imageDao: ImageDao,
    private val imageVersionLogDao: ImageVersionLogDao,
    private val client: Client
) : ImageNotifyService {

    private val logger = LoggerFactory.getLogger(TxImageNotifyService::class.java)

    @Value("\${store.imageDetailBaseUrl}")
    private lateinit var imageDetailBaseUrl: String

    /**
     * 发送镜像发布审核结果通知消息
     * @param imageId 镜像ID
     * @param auditType 审核类型
     */
    override fun sendImageReleaseAuditNotifyMessage(imageId: String, auditType: AuditTypeEnum) {
        val image = imageDao.getImage(dslContext, imageId) ?: return
        // 查出版本日志
        val imageVersionLog = imageVersionLogDao.getLatestImageVersionLogByImageId(dslContext, image.id)?.get(0)
        val titleParams = mapOf(
            "name" to image.imageName,
            "version" to image.version
        )
        val releaseType = imageVersionLog?.releaseType
        val bodyParams = mapOf(
            "name" to image.imageName,
            "version" to image.version,
            "publisher" to image.publisher,
            "releaseType" to if (releaseType != null) MessageCodeUtil.getCodeLanMessage(
                "RELEASE_TYPE_" + ReleaseTypeEnum.getReleaseType(releaseType.toInt())
            ) else "",
            "versionDesc" to (imageVersionLog?.content ?: ""),
            "nameInBody" to image.imageName,
            "imageStatusMsg" to image.imageStatusMsg,
            "url" to imageDetailBaseUrl + image.imageCode
        )
        val receiver: String = image.creator
        val receivers = mutableSetOf(receiver)
        val templateCode = when (auditType) {
            AuditTypeEnum.AUDIT_SUCCESS -> {
                IMAGE_RELEASE_AUDIT_PASS_TEMPLATE
            }
            AuditTypeEnum.AUDIT_REJECT -> {
                IMAGE_RELEASE_AUDIT_REFUSE_TEMPLATE
            }
            else -> {
                IMAGE_RELEASE_AUDIT_REFUSE_TEMPLATE
            }
        }
        val sendNotifyMessageTemplateRequest = SendNotifyMessageTemplateRequest(
            templateCode = templateCode,
            sender = "DevOps",
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
