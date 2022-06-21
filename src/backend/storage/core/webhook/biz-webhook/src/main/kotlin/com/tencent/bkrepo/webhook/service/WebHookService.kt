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

package com.tencent.bkrepo.webhook.service

import com.tencent.bkrepo.webhook.constant.AssociationType
import com.tencent.bkrepo.webhook.pojo.CreateWebHookRequest
import com.tencent.bkrepo.webhook.pojo.UpdateWebHookRequest
import com.tencent.bkrepo.webhook.pojo.WebHook
import com.tencent.bkrepo.webhook.pojo.WebHookLog

/**
 * WebHook服务接口
 */
interface WebHookService {

    /**
     * 创建WebHook
     */
    fun createWebHook(userId: String, request: CreateWebHookRequest): WebHook

    /**
     * 更新WebHook
     */
    fun updateWebHook(userId: String, request: UpdateWebHookRequest): WebHook

    /**
     * 删除WebHook
     */
    fun deleteWebHook(userId: String, id: String)

    /**
     * 获取WebHook
     */
    fun getWebHook(userId: String, id: String): WebHook

    /**
     * 根据关联对象类型[associationType]、关联对象id[associationId]获取WebHook列表
     */
    fun listWebHook(userId: String, associationType: AssociationType, associationId: String?): List<WebHook>

    /**
     * 测试WebHook
     */
    fun testWebHook(userId: String, id: String): WebHookLog

    /**
     * 重试WebHook请求
     */
    fun retryWebHookRequest(logId: String): WebHookLog
}
