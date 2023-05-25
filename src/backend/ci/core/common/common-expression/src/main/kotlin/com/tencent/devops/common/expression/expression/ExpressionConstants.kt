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

package com.tencent.devops.common.expression.expression

import com.tencent.devops.common.expression.expression.functions.Contains
import com.tencent.devops.common.expression.expression.functions.EndsWith
import com.tencent.devops.common.expression.expression.functions.Format
import com.tencent.devops.common.expression.expression.functions.FromJson
import com.tencent.devops.common.expression.expression.functions.Join
import com.tencent.devops.common.expression.expression.functions.StartsWith
import com.tencent.devops.common.expression.expression.functions.StrToTime
import com.tencent.devops.common.expression.expression.sdk.Function
import java.util.TreeMap

object ExpressionConstants {
    val WELL_KNOWN_FUNCTIONS: TreeMap<String, IFunctionInfo> =
        TreeMap<String, IFunctionInfo>(String.CASE_INSENSITIVE_ORDER)

    init {
        addFunction(Contains.name, 2, 2, Contains())
        addFunction(EndsWith.name, 2, 2, EndsWith())
        addFunction(StartsWith.name, 2, 2, StartsWith())
        addFunction(FromJson.name, 1, 1, FromJson())
        addFunction(StrToTime.name, 1, 1, StrToTime())
        addFunction(Join.name, 1, 2, Join())
        addFunction(Format.name, 1, Byte.MAX_VALUE.toInt(), Format())
    }

    private fun addFunction(name: String, minParameters: Int, maxParameters: Int, f: Function) {
        WELL_KNOWN_FUNCTIONS[name] = FunctionInfo(name, minParameters, maxParameters, f)
    }

    const val MAX_DEEP = 50

    // 表达式最大长度，取决于85,000个大对象堆最大阈值
    // TODO 后续看java结构调整
    const val MAX_LENGTH = 21000

    const val INFINITY = "Infinity"
    const val NEGATIVE_INFINITY = "Infinity"
    const val NAN = "NaN"
    const val NULL = "null"
    const val TRUE = "true"
    const val FALSE = "false"

    // Punctuation
    const val START_GROUP = '(' // logical grouping
    const val START_INDEX = '['

    //    const val START_PARAMETERS = '('; // function call
    const val END_GROUP = ')' // logical grouping
    const val END_INDEX = ']'

    //    const val END_PARAMETERS = ')'; // function calll
    const val SEPARATOR = ','
    const val DEREFERENCE = '.'
    const val WILDCARD = '*'

    // Operators
    const val NOT = "!"
    const val NOT_EQUAL = "!="
    const val GREATER_THAN = ">"
    const val GREATER_THAN_OR_EQUAL = ">="
    const val LESS_THAN = "<"
    const val LESS_THAN_OR_EQUAL = "<="
    const val EQUAL = "=="
    const val AND = "&&"
    const val OR = "||"
}
