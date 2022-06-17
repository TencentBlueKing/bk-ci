package com.tencent.devops.common.expression.expression

import com.tencent.devops.common.expression.expression.functions.Contains
import com.tencent.devops.common.expression.expression.functions.EndsWith
import com.tencent.devops.common.expression.expression.functions.FromJson
import com.tencent.devops.common.expression.expression.functions.StartsWith
import com.tencent.devops.common.expression.expression.sdk.Function
import java.util.TreeMap

object ExpressionConstants {
    val WELL_KNOWN_FUNCTIONS: TreeMap<String, IFunctionInfo> =
        TreeMap<String, IFunctionInfo>(String.CASE_INSENSITIVE_ORDER)

    init {
        addFunction("contains", 2, 2, Contains())
        addFunction("endsWith", 2, 2, EndsWith())
        addFunction("startsWith", 2, 2, StartsWith())
        addFunction("fromJson", 1, 1, FromJson())
    }

    private fun addFunction(name: String, minParameters: Int, maxParameters: Int, f: Function) {
        WELL_KNOWN_FUNCTIONS[name] = FunctionInfo(name, minParameters, maxParameters, f)
    }

    const val MAX_DEEP = 50

    // 表达式最大长度，取决于85,000个大对象堆最大阈值
    // TODO: 后续看java结构调整
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
