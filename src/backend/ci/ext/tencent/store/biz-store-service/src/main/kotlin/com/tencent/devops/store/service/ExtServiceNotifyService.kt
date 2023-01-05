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

package com.tencent.devops.store.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceVersionLogDao
import com.tencent.devops.store.dao.common.StoreMemberDao
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
     * @param sendAllAdminFlag 是否发送给所有管理员
     * @param templateCode 通知模板代码
     */
    fun sendServiceReleaseNotifyMessage(serviceId: String, sendAllAdminFlag: Boolean, templateCode: String) {
        val serviceRecord = serviceDao.getServiceById(dslContext, serviceId) ?: return
        // 查出版本日志
        val serviceVersionLogRecord = serviceVersionLogDao.getVersionLogByServiceId(dslContext, serviceId)
        val serviceCode = serviceRecord.serviceCode
        val serviceName = serviceRecord.serviceName
        val titleParams = mapOf(
            "name" to serviceName,
            "version" to serviceRecord.version
        )
        val releaseType = serviceVersionLogRecord!!.releaseType
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
        if (sendAllAdminFlag) {
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
