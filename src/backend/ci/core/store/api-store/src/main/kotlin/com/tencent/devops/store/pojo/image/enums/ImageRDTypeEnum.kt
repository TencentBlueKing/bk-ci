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

package com.tencent.devops.store.pojo.image.enums

enum class ImageRDTypeEnum(val type: Int) {
    SELF_DEVELOPED(0), // 自研
    THIRD_PARTY(1); // 第三方

    override fun toString() = type.toString()

    companion object {
        fun getImageRDTypeByName(name: String): ImageRDTypeEnum {
            values().forEach {
                if (it.name.toLowerCase() == name.toLowerCase()) {
                    return it
                }
            }
            // 默认第三方
            return THIRD_PARTY
        }

        fun getImageRDType(type: Int): ImageRDTypeEnum {
            return when (type) {
                0 -> SELF_DEVELOPED
                1 -> THIRD_PARTY
                else -> THIRD_PARTY
            }
        }

        fun getImageRDTypeStr(type: Int?): String {
            return when (type) {
                0 -> SELF_DEVELOPED.name
                1 -> THIRD_PARTY.name
                // 默认第三方
                else -> THIRD_PARTY.name
            }
        }
    }
}
