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

import com.tencent.devops.common.expression.ContextDataRuntimeException
import com.tencent.devops.common.expression.expression.sdk.CollectionPipelineResult
import java.lang.Exception
import java.util.TreeMap

interface RuntimeNamedValue {
    val key: String
    fun getValue(key: String): PipelineContextData?
}

/**
 * 通过函数参数在在运行时动态的获取value的Dict上下文
 * 例如：凭据
 * @throws ContextDataRuntimeException
 */
@Suppress("TooManyFunctions", "ReturnCount")
class RuntimeDictionaryContextData(private val runtimeNamedValue: RuntimeNamedValue) : DictionaryContextData() {
    override var mIndexLookup: TreeMap<String, Int>? = null
    override var mList: MutableList<DictionaryContextDataPair> = mutableListOf()

    override operator fun get(key: String): PipelineContextData? {
        return getRes(key).value
    }

    override fun getRes(key: String): CollectionPipelineResult {
        if (containsKey(key)) {
            return CollectionPipelineResult(mList.getOrNull(indexLookup[key]!!)?.value)
        }

        // 对象中没有则去请求一次
        // 暂时全部按照无KEY处理，上线后看看情况
        val value = requestAndSaveValue(key) ?: return CollectionPipelineResult.noKey()

        return CollectionPipelineResult(value)
    }

    override fun clone(): PipelineContextData {
        val result = RuntimeDictionaryContextData(runtimeNamedValue)

        if (mList.isNotEmpty()) {
            result.mList = mutableListOf()
            mList.forEach {
                result.mList.add(DictionaryContextDataPair(it.key, it.value?.clone()))
            }
        }

        return result
    }

    private fun requestAndSaveValue(key: String): PipelineContextData? {
        return try {
            val value = runtimeNamedValue.getValue(key)
            if (value != null) {
                set(key, value)
            }
            value
        } catch (ignore: Exception) {
            throw ContextDataRuntimeException("RuntimeDictionaryContextData request key:$key 's value error")
        }
    }
}
