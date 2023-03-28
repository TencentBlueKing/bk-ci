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

package com.tencent.devops.store.service.image.impl

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
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
class TxImageNotifyService @Autowired constructor() : ImageNotifyService {

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var imageDao: ImageDao

    @Autowired
    private lateinit var imageVersionLogDao: ImageVersionLogDao

    @Autowired
    private lateinit var client: Client

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
            "releaseType" to if (releaseType != null) MessageUtil.getCodeLanMessage(
                messageCode = "RELEASE_TYPE_" + ReleaseTypeEnum.getReleaseType(releaseType.toInt()),
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
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
