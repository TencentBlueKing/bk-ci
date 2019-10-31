package com.tencent.devops.notify.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.notify.model.NotifyMessageCommonTemplate
import com.tencent.devops.notify.model.NotifyTemplateMessageRequest
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.notify.pojo.SubNotifyMessageTemplate

interface NotifyMessageTemplateService {
    /**
     * 查找消息模板子信息
     */
    fun getNotifyMessageTemplates(
        userId: String,
        templateId: String
    ): Result<Page<SubNotifyMessageTemplate>>

    /**
     * 查找消息模板公共信息
     */
    fun getCommonNotifyMessageTemplates(
        userId: String,
        templateCode: String?,
        templateName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<NotifyMessageCommonTemplate>>

    /**
     * 添加消息通知模板
     * @param userId 用户ID
     * @param addNotifyMessageTemplateRequest 消息通知新增请求报文体
     */
    fun addNotifyMessageTemplate(
        userId: String,
        addNotifyMessageTemplateRequest: NotifyTemplateMessageRequest
    ): Result<Boolean>

    /**
     * 更新消息通知模板
     * @param userId 用户ID
     * @param templateId 模板ID
     * @param notifyMessageTemplateRequest 消息通知更新请求报文体
     */
    fun updateNotifyMessageTemplate(
        userId: String,
        templateId: String,
        notifyMessageTemplateRequest: NotifyTemplateMessageRequest
    ): Result<Boolean>

    /**
     * 删除消息通知模板
     * @param templateId 模板ID
     */
    fun deleteNotifyMessageTemplate(templateId: String, notifyType: String): Result<Boolean>

    /**
     * 删除公共消息通知模板
     * @param templateId 模板ID
     */
    fun deleteCommonNotifyMessageTemplate(templateId: String): Result<Boolean>

    /**
     * 使用模板发送消息通知
     * @param sendNotifyMessageTemplateRequest 使用模板发送消息通知请求报文体
     */
    fun sendNotifyMessageByTemplate(sendNotifyMessageTemplateRequest: SendNotifyMessageTemplateRequest): Result<Boolean>
}