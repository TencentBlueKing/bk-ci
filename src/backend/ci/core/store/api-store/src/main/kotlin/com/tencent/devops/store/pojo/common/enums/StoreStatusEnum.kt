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

package com.tencent.devops.store.pojo.common.enums

import com.tencent.devops.common.api.util.MessageUtil

enum class StoreStatusEnum(val isProcessing: Boolean) {
    INIT(true), // 初始化
    COMMITTING(true), // 提交中
    BUILDING(true), // 构建中
    BUILD_FAIL(true), // 构建失败
    CHECKING(true), // 验证中
    CHECK_FAIL(true), // 验证失败
    TESTING(true), // 测试中
    EDITING(true), // 填写信息中
    AUDITING(true), // 审核中
    AUDIT_REJECT(false), // 审核驳回
    RELEASED(false), // 已发布
    GROUNDING_SUSPENSION(false), // 上架中止
    UNDERCARRIAGING(false), // 下架中
    UNDERCARRIAGED(false), // 已下架
    TESTED(false); // 测试结束(仅分支测试使用)

    fun getI18n(language: String): String {
        return MessageUtil.getMessageByLocale(
            messageCode = "STORE_BASE_STATUS_${this.name}",
            language = language
        )
    }

    companion object {

        fun getProcessingStatusList(): List<String> {
            return values().filter { it.isProcessing }.map { it.name }
        }

        fun getTestStatusList(): List<String> {
            return listOf(
                TESTING.name,
                EDITING.name,
                AUDITING.name
            )
        }

        fun getStoreFinalStatusList(): List<String> {
            return mutableListOf(
                AUDIT_REJECT.name,
                RELEASED.name,
                GROUNDING_SUSPENSION.name,
                UNDERCARRIAGED.name
            )
        }
    }
}
