/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
package com.tencent.devops.notify.blueking.utils

import com.tencent.devops.common.notify.pojo.EmailNotifyPost
import com.tencent.devops.common.notify.pojo.RtxNotifyPost
import com.tencent.devops.common.notify.pojo.SmsNotifyPost
import com.tencent.devops.common.notify.pojo.VoiceNotifyPost
import com.tencent.devops.common.notify.pojo.WechatNotifyPost
import com.tencent.devops.notify.blueking.sdk.CMSApi

@Suppress("ALL")
class NotifyService(private val cmsApi: CMSApi) {

    companion object {
        const val EMAIL_URL = "/api/c/compapi/cmsi/send_mail/"
        const val RTX_URL = "/api/c/compapi/cmsi/send_qy_weixin/"
        const val SMS_URL = "/api/c/compapi/cmsi/send_sms/"
        const val WECHAT_URL = "/api/c/compapi/cmsi/send_weixin/"
        const val NOC_NOTICE_URL = "/api/c/compapi/cmsi/noc_notice/"
        const val VOICE_URL = "/api/c/compapi/cmsi/send_voice_msg/"
    }

    fun post(url: String, postData: Any, tofConf: Map<String, String>? = null): NotifyResult {

        val apiResp = when (url) {
            EMAIL_URL -> {
                val enp = postData as EmailNotifyPost
                cmsApi.sendMail(enp)
            }

            RTX_URL -> {
                cmsApi.sendQyWeixin(postData as RtxNotifyPost)
            }

            SMS_URL -> {
                cmsApi.sendSms(postData as SmsNotifyPost)
            }

            WECHAT_URL -> {
                cmsApi.sendWeixin(postData as WechatNotifyPost)
            }

            VOICE_URL -> {
                cmsApi.sendVoice(postData as VoiceNotifyPost)
            }

            else -> {
                throw IllegalArgumentException("Unknown message type")
            }
        }
        return NotifyResult(
            Ret = apiResp.code!!,
            ErrCode = apiResp.code,
            ErrMsg = apiResp.message ?: "",
            StackTrace = null,
            data = apiResp.data
        )
    }
}
