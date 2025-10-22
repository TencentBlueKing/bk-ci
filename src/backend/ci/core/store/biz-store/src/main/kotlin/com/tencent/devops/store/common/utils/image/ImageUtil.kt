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

package com.tencent.devops.store.common.utils.image

import java.lang.Integer.min
import org.jooq.tools.StringUtils

object ImageUtil {

    @Suppress("ALL")
    fun compareVersion(version1: String?, version2: String?): Int {
        if (null == version1 && null == version2) {
            return 0
        } else if (null == version1) {
            return -1
        } else if (null == version2) {
            return 1
        }
        val arr1 = version1.split(".").filter { !StringUtils.isBlank(it) }
        val arr2 = version2.split(".").filter { !StringUtils.isBlank(it) }
        val shortLen = min(arr1.size, arr2.size)
        for (i in 0 until shortLen) {
            if (arr1[i].toInt() < arr2[i].toInt()) {
                return -1
            } else if (arr1[i].toInt() > arr2[i].toInt()) {
                return 1
            }
        }
        if (arr1.size < arr2.size) {
            return -1
        } else if (arr1.size > arr2.size) {
            return 1
        }
        return 0
    }
}
