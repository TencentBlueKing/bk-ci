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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.notify.service.inner

import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.common.notify.pojo.WechatNotifyPost
import com.tencent.devops.common.notify.utils.CommonUtils
import com.tencent.devops.common.notify.utils.TOFConfiguration
import com.tencent.devops.common.notify.utils.TOFService
import com.tencent.devops.model.notify.tables.records.TNotifyWechatRecord
import com.tencent.devops.notify.EXCHANGE_NOTIFY
import com.tencent.devops.notify.ROUTE_WECHAT
import com.tencent.devops.notify.dao.WechatNotifyDao
import com.tencent.devops.notify.model.WechatNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.NotificationResponse
import com.tencent.devops.notify.pojo.NotificationResponseWithPage
import com.tencent.devops.notify.pojo.WechatNotifyMessage
import com.tencent.devops.notify.service.WechatService
import com.tencent.devops.common.notify.utils.TOFService.Companion.WECHAT_URL
import com.tencent.devops.common.wechatwork.WechatWorkService
import com.tencent.devops.common.wechatwork.model.enums.UploadMediaType
import com.tencent.devops.notify.model.WeworkNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.WeworkNotifyMessage
import com.tencent.devops.notify.service.WeworkService
import com.tencent.devops.support.model.wechatwork.result.UploadMediaResult
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service
class WeworkServiceImpl @Autowired constructor(
    private val wechatWorkService: WechatWorkService
) : WeworkService {

    private val logger = LoggerFactory.getLogger(WeworkServiceImpl::class.java)
    override fun sendMessage(weworkNotifyMessage: WeworkNotifyMessage) {
        val uploadMediaResponse = wechatWorkService.uploadMedia(
            mediaType = UploadMediaType.valueOf(weworkNotifyMessage.mediaType.name),
            mediaName = weworkNotifyMessage.mediaName,
            mediaInputStream = weworkNotifyMessage.mediaInputStream
        )
        if (uploadMediaResponse == null) {
            logger.error("Upload media failed.")
        } else {
            weworkNotifyMessage.receivers.forEach {
                val sendString = String.format(
                    """{
   "receiver":
   {
       "type": "%s",
       "id": "%s"
   },
   "msgtype": "%s",
   "image" : {
        "media_id" : "%s"
   }
}"""
                    ,
                    weworkNotifyMessage.receiverType,
                    it,
                    weworkNotifyMessage.mediaType,
                    uploadMediaResponse.media_id
                )
                val sendResult = wechatWorkService.sendMessage(sendString)
                if (sendResult) {
                    logger.info("Send wework media success.")
                } else {
                    logger.error("Send wework  media failed.")
                }
            }
        }
    }
}