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

package com.tencent.devops.common.api.enums

enum class PlatformEnum(
    val id: Int,
    val mean: String,
    val tails: List<String>
) {
    UNKNOWN(-1, "未知", emptyList()),

    ANDROID(1, "安卓", listOf(".apk")),

    IOS(2, "IOS", listOf(".ipa")),

    HAP(3, "鸿蒙", listOf(".hap")),

    WIN(4, "Windows", listOf(".zip"))

    ;

    companion object {
        /**
         * 根据尾巴获取平台
         */
        fun ofTail(path: String): PlatformEnum {
            for (platformEnum in values()) {
                for (tail in platformEnum.tails) {
                    if (path.endsWith(tail, true)) {
                        return platformEnum
                    }
                }
            }
            return UNKNOWN
        }

        /**
         * 根据名字获取平台
         */
        fun ofName(name: String): PlatformEnum {
            for (platformEnum in values()) {
                if (platformEnum.toString().equals(name, true)) {
                    return platformEnum
                }
            }
            return UNKNOWN
        }

        /**
         * 是否为安装包
         */
        fun isPackage(path: String): Boolean {
            return ofTail(path) != UNKNOWN
        }

        fun of(id: Int?): PlatformEnum? {
            if (null == id) {
                return null
            }

            values().forEach {
                if (it.id == id) {
                    return it
                }
            }

            return null
        }
    }
}
