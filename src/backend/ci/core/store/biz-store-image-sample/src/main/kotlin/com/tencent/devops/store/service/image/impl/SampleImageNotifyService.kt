package com.tencent.devops.store.service.image.impl

import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.service.image.ImageNotifyService
import org.springframework.stereotype.Service

@Service
class SampleImageNotifyService : ImageNotifyService {

    /**
     * 发送镜像发布审核结果通知消息
     * @param imageId 镜像ID
     * @param auditType 审核类型
     */
    override fun sendImageReleaseAuditNotifyMessage(imageId: String, auditType: AuditTypeEnum) {
        // 开源版本不发送通知
    }
}
