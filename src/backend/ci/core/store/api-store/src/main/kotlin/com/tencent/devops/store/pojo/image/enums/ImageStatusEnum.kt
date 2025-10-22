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
package com.tencent.devops.store.pojo.image.enums

enum class ImageStatusEnum(val status: Int) {
    INIT(0), // 初始化
    COMMITTING(1), // 提交中
    CHECKING(2), // 验证中
    CHECK_FAIL(3), // 验证失败
    TESTING(4), // 测试中
    AUDITING(5), // 审核中
    AUDIT_REJECT(6), // 审核驳回
    RELEASED(7), // 已发布
    GROUNDING_SUSPENSION(8), // 上架中止
    UNDERCARRIAGING(9), // 下架中
    UNDERCARRIAGED(10); // 已下架

    companion object {

        fun getImageStatus(name: String): ImageStatusEnum? {
            values().forEach { enumObj ->
                if (enumObj.name == name) {
                    return enumObj
                }
            }
            return null
        }

        fun getImageStatus(status: Int): String {
            return when (status) {
                0 -> INIT.name
                1 -> COMMITTING.name
                2 -> CHECKING.name
                3 -> CHECK_FAIL.name
                4 -> TESTING.name
                5 -> AUDITING.name
                6 -> AUDIT_REJECT.name
                7 -> RELEASED.name
                8 -> GROUNDING_SUSPENSION.name
                9 -> UNDERCARRIAGING.name
                10 -> UNDERCARRIAGED.name
                else -> INIT.name
            }
        }

        /**
         * 获取处于非终止态的所有状态
         */
        fun getInprocessStatusSet(): Set<Int> {
            return setOf(
                INIT.status,
                COMMITTING.status,
                CHECKING.status,
                TESTING.status,
                // 上架中止应当属于非终止态，需将数据回显至下一次上架
                GROUNDING_SUSPENSION.status,
                AUDITING.status,
                UNDERCARRIAGING.status
            )
        }

        /**
         * 获取处于终止态的所有状态
         */
        fun getFinishedStatusSet(): Set<Int> {
            return setOf(
                CHECK_FAIL.status,
                AUDIT_REJECT.status,
                RELEASED.status,
                UNDERCARRIAGED.status
            )
        }
    }
}
