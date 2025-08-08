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

package com.tencent.devops.common.expression.context

import java.util.TreeMap

open class DictionaryContextData : AbsDictionaryContextData() {
    override var mIndexLookup: TreeMap<String, Int>? = null
    override var mList: MutableList<DictionaryContextDataPair> = mutableListOf()

    override fun clone(): PipelineContextData {
        val result = DictionaryContextData()

        if (mList.isNotEmpty()) {
            result.mList = mutableListOf()
            mList.forEach {
                result.mList.add(DictionaryContextDataPair(it.key, it.value?.clone()))
            }
        }

        return result
    }

    override fun fetchValue(): Any {
        val map = mutableMapOf<String, Any>()
        if (mList.isNotEmpty()) {
            mList.forEach {
                if (it.value is DictionaryContextDataWithVal) {
                    map[it.key] = it.value.fetchValueNative()
                    return@forEach
                }
                map[it.key] = it.value?.fetchValue() ?: ""
            }
        }
        return map
    }
}