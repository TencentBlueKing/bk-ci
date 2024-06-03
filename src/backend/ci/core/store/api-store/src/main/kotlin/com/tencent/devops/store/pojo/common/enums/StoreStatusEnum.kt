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

package com.tencent.devops.store.pojo.common.enums

import com.tencent.devops.common.api.util.MessageUtil

enum class StoreStatusEnum {
    INIT, // 初始化
    COMMITTING, // 提交中
    BUILDING, // 构建中
    BUILD_FAIL, // 构建失败
    CHECKING, // 验证中
    CHECK_FAIL, // 验证失败
    TESTING, // 测试中
    AUDITING, // 审核中
    AUDIT_REJECT, // 审核驳回
    RELEASED, // 已发布
    GROUNDING_SUSPENSION, // 上架中止
    UNDERCARRIAGING, // 下架中
    UNDERCARRIAGED, // 已下架
    TESTED; // 测试结束(仅分支测试使用)

    fun getI18n(language: String): String {
        return MessageUtil.getMessageByLocale(
            messageCode = "STORE_BASE_STATUS_${this.name}",
            language = language
        )
    }

    companion object {

        fun getProcessingStatusList(): List<String> {
            return listOf(
                INIT.name,
                COMMITTING.name,
                BUILDING.name,
                BUILD_FAIL.name,
                CHECKING.name,
                CHECK_FAIL.name,
                TESTING.name,
                AUDITING.name
            )
        }
    }
}
