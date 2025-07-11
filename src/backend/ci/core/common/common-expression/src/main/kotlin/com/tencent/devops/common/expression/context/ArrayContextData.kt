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

import com.fasterxml.jackson.databind.JsonNode
import com.tencent.devops.common.expression.expression.sdk.CollectionPipelineResult
import com.tencent.devops.common.expression.expression.sdk.IReadOnlyArray
import com.tencent.devops.common.expression.utils.ExpressionJsonUtil

class ArrayContextData : PipelineContextData(PipelineContextDataType.ARRAY), IReadOnlyArray<PipelineContextData?> {

    private var mItems = mutableListOf<PipelineContextData?>()

    override fun iterator(): Iterator<PipelineContextData?> = mItems.iterator()

    override val count: Int
        get() = mItems.count()

    override operator fun get(index: Int): PipelineContextData? = mItems[index]

    override fun getRes(index: Int): CollectionPipelineResult {
        if (index >= 0 && index <= mItems.lastIndex) {
            return CollectionPipelineResult(mItems[index])
        }
        return CollectionPipelineResult.noKey()
    }

    override fun clone(): PipelineContextData {
        val result = ArrayContextData()
        if (mItems.isNotEmpty()) {
            result.mItems = mutableListOf()
            mItems.forEach {
                result.mItems.add(it)
            }
        }
        return result
    }

    override fun toJson(): JsonNode {
        val json = ExpressionJsonUtil.createArrayNode()
        if (mItems.isNotEmpty()) {
            mItems.forEach {
                json.add(it?.toJson())
            }
        }
        return json
    }

    override fun fetchValue(): List<Any> {
        val list = mutableListOf<Any>()
        if (mItems.isNotEmpty()) {
            mItems.forEach {
                if (it is DictionaryContextDataWithVal) {
                    list.add(it.fetchValueNative())
                    return@forEach
                }
                list.add(it?.fetchValue() ?: return@forEach)
            }
        }
        return list
    }

    fun add(item: PipelineContextData?) {
        mItems.add(item)
    }
}
