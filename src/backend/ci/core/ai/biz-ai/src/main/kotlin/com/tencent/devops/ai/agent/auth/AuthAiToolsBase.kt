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

package com.tencent.devops.ai.agent.auth

import com.tencent.devops.ai.agent.BaseTools
import com.tencent.devops.auth.api.service.ServiceAuthAiResource
import com.tencent.devops.common.client.Client
import java.util.function.Supplier

/**
 * 权限相关 Agent 工具集的公共基类：Auth AI 客户端、文案与分页/过期等常量。
 */
abstract class AuthAiToolsBase(
    client: Client,
    userIdSupplier: Supplier<String>
) : BaseTools(client, userIdSupplier) {

    protected fun authAiResource(): ServiceAuthAiResource = service(ServiceAuthAiResource::class)

    protected fun translateAuthType(authType: String): String = when (authType) {
        "pipeline" -> "流水线授权"
        "repertory" -> "代码库授权"
        "envNode" -> "环境节点授权"
        "uniqueManagerGroup" -> "唯一管理员组"
        else -> authType
    }

    /**
     * 根据过期状态筛选解析为 [minExpiredAt, maxExpiredAt]（毫秒时间戳，含 null 表示无界）。
     */
    protected fun expiredAtMillisRange(
        expiredStatus: String?,
        expireWithinDays: Int?
    ): Pair<Long?, Long?> {
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val days = expireWithinDays ?: DEFAULT_EXPIRE_WITHIN_DAYS
        return when (expiredStatus?.lowercase()) {
            "expired" -> Pair(null, now)
            "expiring_soon" -> Pair(now, now + days * oneDayMs)
            "valid" -> Pair(now, null)
            else -> Pair(null, null)
        }
    }

    companion object {
        internal const val DEFAULT_PAGE_SIZE = 20
        internal const val MAX_PAGE_SIZE = 50
        internal const val DEFAULT_EXPIRE_WITHIN_DAYS = 30
        internal const val DEFAULT_EXPIRED_DAYS = 365L
        internal const val DEFAULT_APPLY_DAYS = 180
    }
}
