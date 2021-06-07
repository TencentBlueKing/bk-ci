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

package com.tencent.devops.common.api.util

import org.hashids.Hashids

/**
 *
 * Powered By Tencent
 */
object HashUtil {
    private val HASH_SALT = "jhy^3(@So0"
    private val hashids = Hashids(HASH_SALT, 8, "abcdefghijklmnopqrstuvwxyz")

    // 新增其他数据类型的HASH_ID实例，防止被爆破后相同盐值破解PROJECT_ID
    private val OTHER_HASH_SALT = "xlm&gst@Fami1y"
    private val otherHashIds = Hashids(OTHER_HASH_SALT, 4)

    fun encodeLongId(id: Long): String {
        return hashids.encode(id)
    }

    fun encodeIntId(id: Int): String {
        return hashids.encode(id.toLong())
    }

    fun decodeIdToLong(hash: String): Long {
        val ids = hashids.decode(hash)
        return if (ids == null || ids.isEmpty()) {
            0L
        } else {
            ids[0]
        }
    }

    fun decodeIdToInt(hash: String): Int {
        val ids = hashids.decode(hash)
        return if (ids == null || ids.isEmpty()) {
            0
        } else {
            ids[0].toInt()
        }
    }

    fun encodeOtherLongId(id: Long): String {
        return otherHashIds.encode(id)
    }

    fun encodeOtherIntId(id: Int): String {
        return otherHashIds.encode(id.toLong())
    }

    fun decodeOtherIdToLong(hash: String): Long {
        val ids = otherHashIds.decode(hash)
        return if (ids == null || ids.isEmpty()) {
            0L
        } else {
            ids[0]
        }
    }

    fun decodeOtherIdToInt(hash: String): Int {
        val ids = otherHashIds.decode(hash)
        return if (ids == null || ids.isEmpty()) {
            0
        } else {
            ids[0].toInt()
        }
    }
}
