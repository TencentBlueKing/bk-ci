package com.tencent.devops.common.notify.blueking.sdk

import com.tencent.devops.common.notify.blueking.NotifyService.Companion.EMAIL_URL
import com.tencent.devops.common.notify.blueking.NotifyService.Companion.NOC_NOTICE_URL
import com.tencent.devops.common.notify.blueking.NotifyService.Companion.RTX_URL
import com.tencent.devops.common.notify.blueking.NotifyService.Companion.SMS_URL
import com.tencent.devops.common.notify.blueking.NotifyService.Companion.WECHAT_URL
import com.tencent.devops.common.notify.blueking.pojo.EmailNotifyPost
import com.tencent.devops.common.notify.blueking.pojo.RtxNotifyPost
import com.tencent.devops.common.notify.blueking.pojo.SmsNotifyPost
import com.tencent.devops.common.notify.blueking.pojo.WechatNotifyPost
import com.tencent.devops.common.notify.blueking.sdk.pojo.ApiResp
import com.tencent.devops.common.notify.blueking.sdk.pojo.NocNoticeReq
import com.tencent.devops.common.notify.blueking.sdk.pojo.SendMailReq
import com.tencent.devops.common.notify.blueking.sdk.pojo.SendQyWxReq
import com.tencent.devops.common.notify.blueking.sdk.pojo.SendSmsReq
import com.tencent.devops.common.notify.blueking.sdk.pojo.SendWxReq
import com.tencent.devops.common.notify.blueking.sdk.utils.NotifyUtils
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
                    SendWxReq.Data(heading = "蓝鲸通知消息", message = msgInfo),
                    bk_username = sender
            )
        }
        return notifyUtils.doPostRequest(WECHAT_URL, wechatReq)
    }
}