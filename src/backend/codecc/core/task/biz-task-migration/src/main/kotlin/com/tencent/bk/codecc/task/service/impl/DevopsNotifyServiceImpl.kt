package com.tencent.bk.codecc.task.service.impl

import com.tencent.bk.codecc.task.service.DevopsNotifyService
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.notify.enums.EnumEmailType
import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.common.util.AESUtil
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import com.tencent.devops.notify.pojo.WechatNotifyMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class DevopsNotifyServiceImpl @Autowired constructor(
    private val client: Client
) : DevopsNotifyService {

    @Value("\${tof4.passId:#{null}}")
    private val tof4PassId: String? = null

    @Value("\${tof4.token:#{null}}")
    private val tof4Token: String? = null

    @Value("\${tof4.encryptKey:#{null}}")

    private val tof4EncryptKey: String? = null

    override fun sendMail(
        from: String,
        receivers: Set<String>,
        cc: Set<String>,
        bcc: Set<String>,
        title: String,
        content: String,
        priority: String,
        bodyFormat: String,
        attaches: Map<String, String>
    ) {
        val emailNotifyMessage = EmailNotifyMessage()
        with(emailNotifyMessage) {
            format = EnumEmailFormat.valueOf(bodyFormat)
            type = EnumEmailType.INNER_MAIL
            addAllReceivers(receivers)
            addAllCcs(cc)
            addAllBccs(bcc)
            body = content
            sender = from
            this.title = title
            this.priority = EnumNotifyPriority.parse(priority)
            this.codeccAttachFileContent = attaches
            client.getDevopsService(ServiceNotifyResource::class.java).sendEmailNotify(this)
        }
    }

    override fun sendRtx(
        receivers: Set<String>,
        body: String,
        from: String,
        title: String,
        priority: String
    ) {
        val rtxNotifyMessage = RtxNotifyMessage()
        with(rtxNotifyMessage) {
            this.body = body
            this.sender = from
            this.title = title
            this.priority = EnumNotifyPriority.parse(priority)
            addAllReceivers(receivers)
        }
        client.getDevopsService(ServiceNotifyResource::class.java).sendRtxNotify(rtxNotifyMessage)
    }

    override fun sendWeChat(
        body: String,
        priority: String,
        receivers: Set<String>,
        sender: String,
        source: Int
    ) {
        val weChatNotifyMessage = WechatNotifyMessage()
        with(weChatNotifyMessage) {
            this.body = body
            this.priority = EnumNotifyPriority.parse(priority)
            this.addAllReceivers(receivers)
            this.sender = sender
            this.source = EnumNotifySource.parse(source)!!
            client.getDevopsService(ServiceNotifyResource::class.java).sendWechatNotify(this)
        }
    }

    override fun sendMailWithTOF4(
        from: String,
        receivers: Set<String>,
        cc: Set<String>,
        bcc: Set<String>,
        title: String,
        content: String,
        priority: String,
        bodyFormat: String,
        attaches: Map<String, String>
    ) {
        // TODO("not implemented")
    }

    override fun sendRtxWithTOF4(receivers: Set<String>, body: String, from: String, title: String, priority: String) {
        // TODO("not implemented")
    }

    fun getTOF4ExtensionInfo() = AESUtil.encrypt(tof4EncryptKey!!, String.format("%s:%s", tof4PassId!!, tof4Token!!))
}