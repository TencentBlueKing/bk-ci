package com.tencent.devops.common.notify.blueking

import com.tencent.devops.common.notify.blueking.pojo.EmailNotifyPost
import com.tencent.devops.common.notify.blueking.pojo.RtxNotifyPost
import com.tencent.devops.common.notify.blueking.pojo.SmsNotifyPost
import com.tencent.devops.common.notify.blueking.pojo.WechatNotifyPost
import com.tencent.devops.common.notify.blueking.sdk.CMSApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 为与内部版本完全兼容，因此这里使用TOFServer命名
 */
@Service
class NotifyService @Autowired constructor(
        private val cmsApi: CMSApi
) {

    companion object {
        val EMAIL_URL = "/api/c/compapi/cmsi/send_mail/"
        val RTX_URL = "/api/c/compapi/cmsi/send_qy_weixin/"
        val SMS_URL = "/api/c/compapi/cmsi/send_sms/"
        val WECHAT_URL = "/api/c/compapi/cmsi/send_weixin/"
        val NOC_NOTICE_URL = "/api/c/compapi/cmsi/noc_notice/"
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
            else -> {
                throw RuntimeException("Unknown message type")
            }
        }
        return NotifyResult(apiResp.code!!, apiResp.code, apiResp.message ?: "", null, apiResp.data)
    }
}