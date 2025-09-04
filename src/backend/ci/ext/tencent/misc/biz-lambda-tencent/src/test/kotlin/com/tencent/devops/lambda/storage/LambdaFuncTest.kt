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
package com.tencent.devops.lambda.storage

import java.util.regex.Pattern
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@Suppress("ComplexCondition", "NestedBlockDepth")
class LambdaFuncTest {
    companion object {
        private val pattern = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9\\.\\-_]*[a-zA-Z0-9\\-_]\$")
    }

    @Test
    fun variablesMatchTest() {
        Assertions.assertEquals(true, pattern.matcher("variables.abc").find())
        Assertions.assertEquals(true, pattern.matcher("a123-abc.456-EDF_789").find())
        Assertions.assertEquals(false, pattern.matcher("123a-abc.456-EDF_789").find())
        Assertions.assertEquals(false, pattern.matcher("variables.这是错的变量").find())
        Assertions.assertEquals(false, pattern.matcher(".variables.abc.").find())
    }

    @Test
    fun variablesExpressionTest() {
        val variables = mapOf(
            "jobs..os" to "666",
            "abc" to "555",
            "variables.abc" to "555",
            "a.b.c" to "1",
            "a.b.d" to "1",
            "a.b" to "2"
        )

        val invalidKeyList = mutableSetOf<String>()
        variables.forEach { (key, _) ->
            if (!pattern.matcher(key).find()) invalidKeyList.add(key)
        }
        if (invalidKeyList.isEmpty()) {
            variables.forEach { (key, _) ->
                variables.forEach { (another, _) ->
                    if (
                        key != another && !(invalidKeyList.contains(key) && invalidKeyList.contains(another)) &&
                        (
                            key.startsWith(another) && key.removePrefix(another).startsWith('.') ||
                                another.startsWith(key) && another.removePrefix(key).startsWith('.')
                            )
                    ) {
                        invalidKeyList.add(key)
                        invalidKeyList.add(another)
                    }
                }
            }
        }
        Assertions.assertEquals(listOf("a.b.c", "a.b", "a.b.d"), invalidKeyList.toList())
    }
}
