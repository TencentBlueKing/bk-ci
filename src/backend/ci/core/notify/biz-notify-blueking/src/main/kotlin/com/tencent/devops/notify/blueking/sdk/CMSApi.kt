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
package com.tencent.devops.notify.blueking.sdk

import com.tencent.devops.common.notify.pojo.EmailNotifyPost
import com.tencent.devops.common.notify.pojo.RtxNotifyPost
import com.tencent.devops.common.notify.pojo.SmsNotifyPost
import com.tencent.devops.common.notify.pojo.WechatNotifyPost
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.notify.blueking.sdk.pojo.ApiResp
import com.tencent.devops.notify.blueking.sdk.pojo.NocNoticeReq
import com.tencent.devops.notify.blueking.sdk.pojo.SendMailReq
import com.tencent.devops.notify.blueking.sdk.pojo.SendQyWxReq
import com.tencent.devops.notify.blueking.sdk.pojo.SendSmsReq
import com.tencent.devops.notify.blueking.sdk.pojo.SendWxReq
import com.tencent.devops.notify.blueking.sdk.utils.NotifyUtils
import com.tencent.devops.notify.blueking.utils.NotifyService.Companion.EMAIL_URL
import com.tencent.devops.notify.blueking.utils.NotifyService.Companion.NOC_NOTICE_URL
import com.tencent.devops.notify.blueking.utils.NotifyService.Companion.RTX_URL
import com.tencent.devops.notify.blueking.utils.NotifyService.Companion.SMS_URL
import com.tencent.devops.notify.blueking.utils.NotifyService.Companion.WECHAT_URL
import com.tencent.devops.notify.constant.NotifyMessageCode.BK_NOTIFY_MESSAGES
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CMSApi @Autowired constructor(
    private val notifyUtils: NotifyUtils
) {

    /**
     * 发送邮件
     */
    fun sendMail(email: EmailNotifyPost): ApiResp {
        val mailReq = with(email) {
            SendMailReq(
                    null, title, content, null, to, null, cc,
                    if (bodyFormat == 0) "Text" else "Html", null,
                    bk_username = from
            )
        }

        return notifyUtils.doPostRequest(EMAIL_URL, mailReq)
    }

    /**
     * 发送短信
     */
    fun sendSms(smsNotifyPost: SmsNotifyPost): ApiResp {
        val smsReq = with(smsNotifyPost) {
            SendSmsReq(
                    msgInfo, null, receiver, null,
                    bk_username = sender
            )
        }

        return notifyUtils.doPostRequest(SMS_URL, smsReq)
    }

    /**
     * 公共语音通知
     */
    @Suppress("UNUSED")
    fun nocNotice(esbReq: NocNoticeReq): ApiResp {

        return notifyUtils.doPostRequest(NOC_NOTICE_URL, esbReq)
    }

    /**
     * 发送企业微信
     */
    fun sendQyWeixin(rtxNotifyPost: RtxNotifyPost): ApiResp {
        val rtxReq = with(rtxNotifyPost) {
            SendQyWxReq(msgInfo, receiver, bk_username = sender)
        }
        return notifyUtils.doPostRequest(RTX_URL, rtxReq)
    }

    /**
     * 发送微信消息，支持微信公众号消息，及微信企业号消息
     */
    fun sendWeixin(wechatNotifyPost: WechatNotifyPost): ApiResp {
        val wechatReq = with(wechatNotifyPost) {
            SendWxReq(
                    null, receiver,
                    SendWxReq.Data(
                        heading = I18nUtil.getCodeLanMessage(
                            messageCode = BK_NOTIFY_MESSAGES,
                            language = I18nUtil.getLanguage(wechatNotifyPost.receiver)
                        ),
                        message = msgInfo
                    ),
                    bk_username = sender
            )
        }
        return notifyUtils.doPostRequest(WECHAT_URL, wechatReq)
    }
}
