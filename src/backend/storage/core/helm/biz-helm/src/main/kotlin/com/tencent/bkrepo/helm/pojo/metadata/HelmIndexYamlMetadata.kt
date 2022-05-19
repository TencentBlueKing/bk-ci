/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.helm.pojo.metadata

import java.util.SortedSet

data class HelmIndexYamlMetadata(
    val apiVersion: String,
    val entries: MutableMap<String, SortedSet<HelmChartMetadata>> = mutableMapOf(),
    var generated: String,
    val serverInfo: Map<String, Any> = emptyMap()
) {
    fun entriesSize(): Int {
        var count = 0
        entries.values.forEach {
            count += it.size
        }
        return count
    }

    fun parseMapForFilter(filter: String): Map<String, SortedSet<HelmChartMetadata>> {
        return if (this.entries.isEmpty()) {
            mutableMapOf()
        } else {
            entries.filterKeys { filterKey(it, filter) }
        }
    }

    /**
     * 通过indexof匹配想要查询的字符
     */
    fun filterKey(key: String, filters: String): Boolean {
        return key.indexOf(filters)> -1
    }
}
