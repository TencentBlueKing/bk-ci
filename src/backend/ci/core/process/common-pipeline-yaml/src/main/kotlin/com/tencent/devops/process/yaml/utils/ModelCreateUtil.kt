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

package com.tencent.devops.process.yaml.utils

import java.lang.StringBuilder
import java.util.ArrayDeque

/**
 * Model生成中的一些通用逻辑
 */
@Suppress("ComplexMethod", "ComplexCondition")
object ModelCreateUtil {

    /**
     * 去掉IF表达式中的 ${{}} 嵌套表达式只去掉最外层
     */
    fun removeIfBrackets(exp: String): String {
        val inBracketStack = ArrayDeque<Int>()
        val bracketIndexes = mutableListOf<Pair<Int, Int>>()

        val expChars = exp.toCharArray()

        var i = 0
        while (i < expChars.size) {
            // 寻找 ${{
            if (expChars[i] == '$' && i + 2 < expChars.size && expChars[i + 1] == '{' && expChars[i + 2] == '{') {
                inBracketStack.push(i)
                i += 3
                continue
            }

            // 寻找 }}
            if (expChars[i] == '}' && i + 1 < expChars.size && expChars[i + 1] == '}') {
                if (inBracketStack.isEmpty()) {
                    i += 1
                    continue
                }
                val inIndex = inBracketStack.pop()
                // 当栈都空了说明找到了最外层的${{}}
                if (inBracketStack.isEmpty()) {
                    bracketIndexes.add(Pair(inIndex, i))
                }
                i += 2
                continue
            }

            i += 1
        }

        if (bracketIndexes.isEmpty()) {
            return expChars.concatToString()
        }

        val result = StringBuilder()
        var lastIndex = 0
        bracketIndexes.forEach { (inIndex, outIndex) ->
            result.append(exp.substring(lastIndex, inIndex))
            lastIndex = inIndex + 3
            result.append(exp.substring(lastIndex, outIndex))
            lastIndex = outIndex + 2
        }
        if (lastIndex < exp.length) {
            result.append(exp.substring(lastIndex, exp.length))
        }

        return result.toString()
    }
}
