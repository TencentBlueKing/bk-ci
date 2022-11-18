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
package com.tencent.devops.notify.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.notify.pojo.NotifyContext
import com.tencent.devops.notify.pojo.NotifyMessageCommonTemplate
import com.tencent.devops.notify.pojo.NotifyMessageContextRequest
import com.tencent.devops.notify.pojo.NotifyTemplateMessageRequest
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.notify.pojo.SubNotifyMessageTemplate

@Suppress("ALL")
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
     * 更新腾讯云ses模板id信息
     * @param templateId 模板ID
     */
    fun updateTXSESTemplateId(userId: String, templateId: String, sesTemplateId: Int?): Result<Boolean>

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
     * @param request 使用模板发送消息通知请求报文体
     */
    fun sendNotifyMessageByTemplate(request: SendNotifyMessageTemplateRequest): Result<Boolean>

    /**
     * 使用模板取消消息通知
     * @param request 使用模板发送消息通知请求报文体
     */
    fun completeNotifyMessageByTemplate(request: SendNotifyMessageTemplateRequest): Result<Boolean>

    /**
     * 使用模板发送消息通知
     * @param getNotifyMessageByTemplate 使用模板发送消息通知请求报文体
     */
    fun getNotifyMessageByTemplate(request: NotifyMessageContextRequest): Result<NotifyContext?>
}
