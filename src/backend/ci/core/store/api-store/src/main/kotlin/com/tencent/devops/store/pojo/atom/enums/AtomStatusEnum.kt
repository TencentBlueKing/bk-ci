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

package com.tencent.devops.store.pojo.atom.enums

import com.tencent.devops.common.api.util.MessageUtil

@Suppress("UNUSED")
enum class AtomStatusEnum(val status: Int) {
    INIT(0), // 初始化
    COMMITTING(1), // 提交中
    BUILDING(2), // 构建中
    BUILD_FAIL(3), // 构建失败
    TESTING(4), // 测试中
    AUDITING(5), // 审核中
    AUDIT_REJECT(6), // 审核驳回
    RELEASED(7), // 已发布
    GROUNDING_SUSPENSION(8), // 上架中止
    UNDERCARRIAGING(9), // 下架中
    UNDERCARRIAGED(10), // 已下架
    CODECCING(11), // 代码检查中
    CODECC_FAIL(12), // 代码检查失败
    TESTED(13); // 测试结束(仅分支测试使用)

    companion object {

        fun getAtomStatus(name: String): AtomStatusEnum? {
            values().forEach { enumObj ->
                if (enumObj.name == name) {
                    return enumObj
                }
            }
            return null
        }

        fun getAtomStatus(status: Int): String {
            values().forEach { enumObj ->
                if (enumObj.status == status) {
                    return enumObj.name
                }
            }
            return INIT.name
        }

        fun getProcessingStatusList(): List<Byte> {
            return listOf(
                INIT.status.toByte(),
                COMMITTING.status.toByte(),
                BUILDING.status.toByte(),
                BUILD_FAIL.status.toByte(),
                TESTING.status.toByte(),
                AUDITING.status.toByte(),
                CODECCING.status.toByte(),
                CODECC_FAIL.status.toByte()
            )
        }
    }

    fun getI18n(language: String): String {
        return MessageUtil.getMessageByLocale(
            messageCode = "STORE_ATOM_STATUS_${this.name}",
            language = language
        )
    }
}
