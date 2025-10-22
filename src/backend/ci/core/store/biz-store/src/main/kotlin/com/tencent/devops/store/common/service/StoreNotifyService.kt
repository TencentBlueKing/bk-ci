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

package com.tencent.devops.store.common.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum

interface StoreNotifyService {

    /**
     * 根据消息模板发送通知消息
     * @param templateCode 通知模板代码
     * @param sender 发送者
     * @param receivers 通知接收者
     * @param titleParams 标题动态参数
     * @param bodyParams 内容动态参数
     * @param cc 邮件抄送接收者
     * @param bcc 邮件密送接收者
     */
    @Suppress("ALL")
    fun sendNotifyMessage(
        templateCode: String,
        sender: String,
        receivers: MutableSet<String> = mutableSetOf(),
        titleParams: Map<String, String>? = null,
        bodyParams: Map<String, String>? = null,
        cc: MutableSet<String>? = null,
        bcc: MutableSet<String>? = null
    ): Result<Boolean>

    /**
     * 发送组件发布审核结果通知消息
     * @param storeId 组件ID
     * @param auditType 审核类型
     */
    fun sendStoreReleaseAuditNotifyMessage(storeId: String, auditType: AuditTypeEnum)
}
