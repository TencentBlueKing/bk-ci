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
 *
 */

package com.tencent.devops.repository.pojo.enums

/**
 * github权限枚举
 */
enum class GithubAccessLevelEnum(val level: Int) {
    GUEST(10),
    READ(15),
    TRIAGE(20),
    WRITE(30),
    MAINTAIN(40),
    ADMIN(50);

    companion object {
        fun getGithubAccessLevel(level: Int): GithubAccessLevelEnum {
            return when (level) {
                10 -> GUEST
                15 -> READ
                20 -> TRIAGE
                30 -> WRITE
                40 -> MAINTAIN
                50 -> ADMIN
                else -> GUEST
            }
        }

        fun getGithubAccessLevel(permission: String?): GithubAccessLevelEnum {
            return when (permission) {
                "read" -> READ
                "triage" -> TRIAGE
                "write" -> WRITE
                "maintain" -> MAINTAIN
                "admin" -> ADMIN
                else -> GUEST
            }
        }
    }
}
