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
package com.tencent.devops.notify.service.inner

import com.tencent.devops.common.wechatwork.WechatWorkService
import com.tencent.devops.common.wechatwork.model.enums.UploadMediaType
import com.tencent.devops.notify.pojo.WeworkNotifyMediaMessage
import com.tencent.devops.notify.pojo.WeworkNotifyTextMessage
import com.tencent.devops.notify.service.WeworkService
import org.apache.commons.lang3.StringEscapeUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "notify", name = ["weworkChannel"], havingValue = "oldWework")
class OldWeworkServiceImpl @Autowired constructor(
    private val wechatWorkService: WechatWorkService
) : WeworkService {

    private val logger = LoggerFactory.getLogger(OldWeworkServiceImpl::class.java)
    override fun sendMediaMessage(weworkNotifyMediaMessage: WeworkNotifyMediaMessage) {
        val uploadMediaResponse = wechatWorkService.uploadMedia(
            mediaType = UploadMediaType.valueOf(weworkNotifyMediaMessage.mediaType.name),
            mediaName = weworkNotifyMediaMessage.mediaName,
            mediaInputStream = weworkNotifyMediaMessage.mediaInputStream
        )
        if (uploadMediaResponse == null) {
            logger.error("Upload media failed.")
        } else {
            weworkNotifyMediaMessage.receivers.forEach {
                val sendString = String.format(
                    """{
   "receiver":
   {
       "type": "%s",
       "id": "%s"
   },
   "msgtype": "%s",
   "%s" : {
        "media_id" : "%s"
   }
}""",
                    weworkNotifyMediaMessage.receiverType,
                    it,
                    weworkNotifyMediaMessage.mediaType,
                    weworkNotifyMediaMessage.mediaType,
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

    override fun sendTextMessage(weworkNotifyTextMessage: WeworkNotifyTextMessage): Boolean {

        weworkNotifyTextMessage.receivers.forEach {
            val sendString = String.format(
                """{
"receiver":
{
   "type": "%s",
   "id": "%s"
},
"msgtype": "%s",
"%s" : {
    "content" : "%s"
}
}""",
                weworkNotifyTextMessage.receiverType,
                it,
                weworkNotifyTextMessage.textType,
                weworkNotifyTextMessage.textType,
                StringEscapeUtils.escapeJson(weworkNotifyTextMessage.message)
            )
            val sendResult = wechatWorkService.sendMessage(sendString)
            if (sendResult) {
                logger.info("Send wework text success.")
            } else {
                logger.error("Send wework  text failed.")
            }
        }
        return true
    }
}
