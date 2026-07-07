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

package com.tencent.devops.remotedev.pojo

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class WorkspaceKind(
    @get:JsonValue
    val value: String
) {
    CVD_PERSONAL("cvd-personal"),
    CVD_TEAM("cvd-team");

    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun parse(value: String): WorkspaceKind {
            return values().find { it.value == value }
                ?: throw IllegalArgumentException("Unsupported workspaceKind: $value")
        }

        /**
         * JAX-RS 对枚举类型的 @QueryParam 转换会优先使用 fromString（存在时），
         * 否则按枚举名匹配。这里委托到 parse 以保证按对外值 cvd-personal/cvd-team 解析。
         */
        @JvmStatic
        fun fromString(value: String): WorkspaceKind = parse(value)

        fun fromDb(value: String?, ownerType: WorkspaceOwnerType): WorkspaceKind {
            return if (value.isNullOrBlank()) {
                defaultByOwnerType(ownerType)
            } else {
                parse(value)
            }
        }

        fun defaultByOwnerType(ownerType: WorkspaceOwnerType): WorkspaceKind {
            return if (ownerType.personalUse()) CVD_PERSONAL else CVD_TEAM
        }
    }
}
