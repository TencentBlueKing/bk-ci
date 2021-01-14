package com.tencent.bk.codecc.task.service.impl

import com.tencent.bk.codecc.task.service.DevopsNotifyService
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.notify.enums.EnumEmailType
import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DevopsNotifyServiceImpl @Autowired constructor(
    private val client: Client
) : DevopsNotifyService{

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
        body : String,
        from : String,
        title : String,
        priority: String
    ){
        val rtxNotifyMessage = RtxNotifyMessage()
        with(rtxNotifyMessage){
            this.body = body
            this.sender = from
            this.title = title
            this.priority = EnumNotifyPriority.parse(priority)
            addAllReceivers(receivers)
        }
        client.getDevopsService(ServiceNotifyResource::class.java).sendRtxNotify(rtxNotifyMessage)
    }
}