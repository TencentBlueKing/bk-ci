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

package com.tencent.devops.common.expression.expression.functions

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.DateTimeUtil.YYYY_MM_DD
import com.tencent.devops.common.api.util.DateTimeUtil.YYYY_MM_DD_HH_MM_SS
import com.tencent.devops.common.expression.FunctionFormatException
import com.tencent.devops.common.expression.expression.sdk.EvaluationContext
import com.tencent.devops.common.expression.expression.sdk.Function
import com.tencent.devops.common.expression.expression.sdk.ResultMemory

class StrToTime : Function() {
    companion object {
        const val name = "strToTime"
    }

    override fun createNode(): Function = StrToTime()

    override fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?> {
        val dateStr = parameters[0].evaluate(context).convertToString()
        val timestamp = when (dateStr.length) {
            10 -> DateTimeUtil.stringToTimestamp(dateStr, YYYY_MM_DD)
            19 -> DateTimeUtil.stringToTimestamp(dateStr, YYYY_MM_DD_HH_MM_SS)
            else -> throw FunctionFormatException.invalidFormatString(dateStr)
        }
        return Pair(null, timestamp)
    }

    override fun subNameValueEvaluateCore(context: EvaluationContext): Pair<Any?, Boolean> {
        val left = parameters[0].subNameValueEvaluate(context).parseSubNameValueEvaluateResult()
        return Pair("$name($left)", false)
    }
}
