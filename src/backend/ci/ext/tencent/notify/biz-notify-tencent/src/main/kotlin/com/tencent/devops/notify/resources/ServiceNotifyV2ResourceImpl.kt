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
package com.tencent.devops.notify.resources

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.notify.api.ServiceNotifyV2Resource
import com.tencent.devops.notify.pojo.BaseMessage
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import com.tencent.devops.notify.service.EmailService
import com.tencent.devops.notify.service.RtxService
import com.tencent.devops.notify.util.MessageCheckUtil
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceNotifyV2ResourceImpl @Autowired constructor(
    private val emailService: EmailService,
    private val rtxService: RtxService
) : ServiceNotifyV2Resource {
    /**
     * 企业微信
     */
    override fun sendRtxNotify(message: RtxNotifyMessage): Result<Boolean> {
        MessageCheckUtil.checkRtxMessage(message)
        checkV2ExtInfo(message)

        if (message.getReceivers().isNotEmpty()) {
            rtxService.sendMqMsg(message)
        }

        return Result(true)
    }

    /**
     * 邮件
     */
    override fun sendEmailNotify(message: EmailNotifyMessage): Result<Boolean> {
        MessageCheckUtil.checkEmailMessage(message)
        checkV2ExtInfo(message)

        if (message.getReceivers().isNotEmpty()) {
            emailService.sendMqMsg(message)
        }

        return Result(true)
    }

    /**
     * 校验扩展信息，服务于TOF4
     */
    private fun checkV2ExtInfo(message: BaseMessage) {
        if (message.v2ExtInfo.isNullOrBlank()) {
            throw InvalidParamException(
                message = "invalid v2ExtInfo:${message.v2ExtInfo}",
                params = arrayOf("$message.v2ExtInfo")
            )
        }
    }
}
