package com.tencent.devops.store.service.image

import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import org.springframework.stereotype.Service

@Service
interface ImageNotifyService {

    /**
     * 发送镜像发布审核结果通知消息
     * @param imageId 镜像ID
     * @param auditType 审核类型
     */
    fun sendImageReleaseAuditNotifyMessage(imageId: String, auditType: AuditTypeEnum)
}
