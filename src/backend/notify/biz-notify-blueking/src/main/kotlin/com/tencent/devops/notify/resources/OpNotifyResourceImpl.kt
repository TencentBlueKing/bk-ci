package com.tencent.devops.notify.resources

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.notify.api.op.OpNotifyResource
import com.tencent.devops.notify.pojo.*
import com.tencent.devops.notify.service.EmailService
import com.tencent.devops.notify.service.RtxService
import com.tencent.devops.notify.service.SmsService
import com.tencent.devops.notify.service.WechatService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpNotifyResourceImpl @Autowired constructor(
    private val emailService: EmailService,
    private val rtxService: RtxService,
    private val smsService: SmsService,
    private val wechatService: WechatService
) : OpNotifyResource {

    override fun sendRtxNotify(message: RtxNotifyMessage): Result<Boolean> {
        if (message.title.isEmpty()) {
            throw ParamBlankException("无效的标题")
        }
        if (message.body.isEmpty()) {
            throw ParamBlankException("无效的内容")
        }
        if (message.isReceiversEmpty()) {
            throw ParamBlankException("无效的接收者")
        }
        rtxService.sendMqMsg(message)
        return Result(true)
    }

    override fun sendEmailNotify(message: EmailNotifyMessage): Result<Boolean> {
        if (message.title.isEmpty()) {
            throw ParamBlankException("无效的标题")
        }
        if (message.body.isEmpty()) {
            throw ParamBlankException("无效的内容")
        }
        if (message.isReceiversEmpty()) {
            throw ParamBlankException("无效的接收者")
        }
        emailService.sendMqMsg(message)
        return Result(true)
    }

    override fun sendWechatNotify(message: WechatNotifyMessage): Result<Boolean> {
        if (message.body.isEmpty()) {
            throw ParamBlankException("无效的内容")
        }
        if (message.isReceiversEmpty()) {
            throw ParamBlankException("无效的接收者")
        }
        wechatService.sendMqMsg(message)
        return Result(true)
    }

    override fun sendSmsNotify(message: SmsNotifyMessage): Result<Boolean> {
        if (message.body.isEmpty()) {
            throw ParamBlankException("无效的内容")
        }
        if (message.isReceiversEmpty()) {
            throw ParamBlankException("无效的接收者")
        }
        smsService.sendMqMsg(message)
        return Result(true)
    }

    override fun listNotifications(type: String?, page: Int, pageSize: Int, success: Boolean?, fromSysId: String?, createdTimeSortOrder: String?): Result<NotificationResponseWithPage<BaseMessage>?> {
        if (type.isNullOrEmpty() || !(type == "rtx" || type == "email" || type == "wechat" || type == "sms")) {
            throw InvalidParamException("无效的通知方式参数type")
        }
        when (type) {
            "rtx" -> return Result(rtxService.listByCreatedTime(page, pageSize, success, fromSysId, createdTimeSortOrder))
            "email" -> return Result(emailService.listByCreatedTime(page, pageSize, success, fromSysId, createdTimeSortOrder))
            "wechat" -> return Result(wechatService.listByCreatedTime(page, pageSize, success, fromSysId, createdTimeSortOrder))
            "sms" -> return Result(smsService.listByCreatedTime(page, pageSize, success, fromSysId, createdTimeSortOrder))
        }
        return Result(0, "")
    }
}