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

package com.tencent.devops.common.expression.expression.sdk.operators

import com.tencent.devops.common.expression.ContextNotFoundException
import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.context.ContextValueNode
import com.tencent.devops.common.expression.expression.EvaluationResult
import com.tencent.devops.common.expression.expression.ExpressionConstants
import com.tencent.devops.common.expression.expression.sdk.CollectionResult
import com.tencent.devops.common.expression.expression.sdk.Container
import com.tencent.devops.common.expression.expression.sdk.EvaluationContext
import com.tencent.devops.common.expression.expression.sdk.ExpressionNode
import com.tencent.devops.common.expression.expression.sdk.ExpressionUtility
import com.tencent.devops.common.expression.expression.sdk.IReadOnlyArray
import com.tencent.devops.common.expression.expression.sdk.IReadOnlyObject
import com.tencent.devops.common.expression.expression.sdk.Literal
import com.tencent.devops.common.expression.expression.sdk.MemoryCounter
import com.tencent.devops.common.expression.expression.sdk.ResultMemory
import com.tencent.devops.common.expression.expression.sdk.Wildcard
import kotlin.math.floor

@Suppress("NestedBlockDepth")
class Index : Container() {

    override val traceFullyRealized = true

    override val formatValue: String = ExpressionConstants.DEREFERENCE.toString()

    override fun convertToExpression(): String {
        // 验证我们是否可以简化表达式，我们宁愿返回 github.sha 然后 github['sha'] 所以我们检查这是否是一个简单的案例
        return if (parameters[1] is Literal &&
            (parameters[1] as Literal).value is String &&
            ExpressionUtility.isLegalKeyword((parameters[1] as Literal).value as String)
        ) {
            "${parameters[0].convertToExpression()}.${(parameters[1] as Literal).value as String}"
        } else {
            "${parameters[0].convertToExpression()}[${parameters[1].convertToExpression()}]"
        }
    }

    override fun convertToRealizedExpression(context: EvaluationContext): String {
        // Check if the result was stored
        val (re, result) = context.tryGetTraceResult(this)
        if (result) {
            return re!!
        }

        return parameters[0].convertToRealizedExpression(context) +
                "[${parameters[1].convertToRealizedExpression(context)}]"
    }

    override fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?> {
        val left = parameters[0].evaluate(context)
        // 如果有多个索引如 a.b.c 因为是递归计算，到 c的时候拿到的左参数一定是 a.b 中的 .，所以追踪左参数只追踪第一个
        if (parameters[0] !is Index) {
            context.options.contextNotNull.trace(parameters[0].format())
        }
        // Not a collection
        val (collection, ok) = left.tryGetCollectionInterface()
        if (!ok) {
            return Pair(
                null,
                if (parameters[1] is Wildcard) {
                    FilteredArray()
                } else {
                    null
                }
            )
        }
        // Filtered array
        else if (collection is FilteredArray) {
            val (mem, obj) = handleFilteredArray(context, collection)
            return Pair(mem, obj)
        }
        // Object
        else if (collection is IReadOnlyObject) {
            val (mem, obj) = handleObject(context, collection)
            return Pair(mem, obj)
        }
        // Array
        else if (collection is IReadOnlyArray<*>) {
            val (mem, obj) = handleArray(context, collection)
            return Pair(mem, obj)
        }

        return Pair(null, null)
    }

    // 对于索引的部分计算，如果最左的参数是已有的nameValued就进行计算，不存在的nameValue为空
    // 如果最左参数不是，就拼接回原本的表达式
    override fun subNameValueEvaluateCore(context: EvaluationContext): Pair<Any?, Boolean> {
        var left = parameters[0]
        while (left is Index) {
            left = left.parameters[0]
        }

        if (left !is ContextValueNode) {
            val leftV = parameters[0].subNameValueEvaluate(context).parseSubNameValueEvaluateResult()
            val value = if (parameters[1] is Literal && (parameters[1] as Literal).value is String &&
                ExpressionUtility.isLegalKeyword((parameters[1] as Literal).value as String)
            ) {
                "$leftV.${(parameters[1] as Literal).value as String}"
            } else {
                val rightV = parameters[1].subNameValueEvaluate(context).parseSubNameValueEvaluateResult()
                "$leftV[$rightV]"
            }
            return Pair(value, false)
        }

        if ((context.state as ExecutionContext).expressionValues[left.name] == null) {
            return Pair(convertToExpression(), false)
        }

        val result = evaluate(context).value

        // 对于表达式出来的替换不带有''
        return Pair(result, true)
    }

    @Suppress("ComplexMethod")
    private fun handleFilteredArray(
        context: EvaluationContext,
        filteredArray: FilteredArray
    ): Pair<ResultMemory, FilteredArray?> {
        val result = FilteredArray()
        val counter = MemoryCounter(this, context.options.maxMemory)

        val index = IndexHelper(context, parameters[1])

        filteredArray.forEach { item ->
            // Leverage the expression SDK to traverse the object
            val itemResult = EvaluationResult.createIntermediateResult(context, item)
            val (nestedCollection, ok) = itemResult.tryGetCollectionInterface()
            if (ok) {
                // Apply the index to each child object
                if (nestedCollection is IReadOnlyObject) {
                    // Wildcard
                    if (index.isWildcard) {
                        nestedCollection.values.forEach { value ->
                            result.add(value)
                            counter.add(Int.SIZE_BYTES)
                        }
                    }
                    // String
                    else if (index.hasStringIndex) {
                        val key = index.stringIndex!!
                        val nestedObjectValueRes = nestedCollection.getRes(key)
                        if (context.options.contextNotNull()) {
                            nestedObjectValueRes.throwIfNoKey()
                        }
                        if (!nestedObjectValueRes.noKey()) {
                            result.add(nestedObjectValueRes.value)
                            counter.add(Int.SIZE_BYTES)
                        }
                    }
                }
                // Apply the index to each child array
                else if (nestedCollection is IReadOnlyArray<*>) {
                    // Wildcard
                    if (index.isWildcard) {
                        nestedCollection.forEach { value ->
                            result.add(value)
                            counter.add(Int.SIZE_BYTES)
                        }
                    }
                    // Int
                    else if (index.hasIntegerIndex && index.integerIndex < nestedCollection.count) {
                        result.add(nestedCollection[index.integerIndex])
                        counter.add(Int.SIZE_BYTES)
                    }
                }
            }
        }
        return Pair(ResultMemory().also { it.bytes = counter.currentBytes }, result)
    }

    private fun handleObject(
        context: EvaluationContext,
        obj: IReadOnlyObject
    ): Pair<ResultMemory?, Any?> {
        val index = IndexHelper(context, parameters[1])

        // Wildcard
        if (index.isWildcard) {
            val filteredArray = FilteredArray()
            val counter = MemoryCounter(this, context.options.maxMemory)
            counter.addMinObjectSize()

            obj.values.forEach { value ->
                filteredArray.add(value)
                counter.add(Int.SIZE_BYTES)
            }

            return Pair(ResultMemory().also { it.bytes = counter.currentBytes }, filteredArray)
        }
        // String
        else {
            val key = index.stringIndex
            if (key == null && context.options.contextNotNull()) {
                throw ContextNotFoundException()
            }
            val result = obj.getRes(key ?: return Pair(null, null))
            if (context.options.contextNotNull()) {
                result.throwIfNoKey()
            }
            if (index.hasStringIndex && !result.noKey()) {
                return Pair(null, result.value)
            }
        }

        return Pair(null, null)
    }

    private fun handleArray(
        context: EvaluationContext,
        array: IReadOnlyArray<*>
    ): Pair<ResultMemory?, Any?> {
        val index = IndexHelper(context, parameters[1])

        // Wildcard
        if (index.isWildcard) {
            val filtered = FilteredArray()
            val counter = MemoryCounter(this, context.options.maxMemory)
            counter.addMinObjectSize()

            array.forEach { item ->
                filtered.add(item)
                counter.add(Int.SIZE_BYTES)
            }

            return Pair(ResultMemory().also { it.bytes = counter.currentBytes }, filtered)
        }
        // Integer
        else if (index.hasIntegerIndex && index.integerIndex < array.count) {
            return Pair(null, array[index.integerIndex])
        }
        if (context.options.contextNotNull()) {
            throw ContextNotFoundException()
        }

        return Pair(null, null)
    }

    private class FilteredArray(
        private val mList: MutableList<Any?> = mutableListOf()
    ) : IReadOnlyArray<Any?> {

        fun add(o: Any?) {
            mList.add(o)
        }

        override val count: Int
            get() = mList.count()

        override operator fun get(index: Int): Any? {
            return mList[index]
        }

        override fun getRes(index: Int): CollectionResult {
            if (index >= 0 && index <= mList.lastIndex) {
                return CollectionResult(mList[index])
            }
            return CollectionResult.noKey()
        }

        override fun iterator(): Iterator<Any?> {
            return mList.iterator()
        }
    }

    private class IndexHelper(context: EvaluationContext, val parameter: ExpressionNode) {
        val mResult: EvaluationResult
        private val mIntegerIndex: Lazy<Int?>
        private val mStringIndex: Lazy<String?>

        init {
            mResult = parameter.evaluate(context)

            // 追踪右参数
            context.options.contextNotNull.trace(parameter.format())

            mIntegerIndex = lazy {
                var doubleIndex = mResult.convertToNumber()
                if (doubleIndex.isNaN() || doubleIndex < 0.0) {
                    return@lazy null
                }

                doubleIndex = floor(doubleIndex)
                if (doubleIndex > Int.MAX_VALUE.toDouble()) {
                    return@lazy null
                }

                return@lazy doubleIndex.toInt()
            }

            mStringIndex = lazy {
                if (mResult.isPrimitive) {
                    mResult.convertToString()
                } else {
                    null
                }
            }
        }

        val hasIntegerIndex get() = mIntegerIndex.value != null

        val hasStringIndex get() = mStringIndex.value != null

        val isWildcard get() = parameter is Wildcard

        val integerIndex get() = mIntegerIndex.value ?: 0

        val stringIndex get() = mStringIndex.value
    }
}
