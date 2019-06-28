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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.pojo.atom.enums

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
    UNDERCARRIAGED(10); // 已下架

    companion object {

        fun getAtomStatus(name: String): AtomStatusEnum? {
            AtomStatusEnum.values().forEach { enumObj ->
                if (enumObj.name == name) {
                    return enumObj
                }
            }
            return null
        }

        fun getAtomStatus(status: Int): String {
            return when (status) {
                0 -> INIT.name
                1 -> COMMITTING.name
                2 -> BUILDING.name
                3 -> BUILD_FAIL.name
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
    }
}
