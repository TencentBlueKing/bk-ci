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

package com.tencent.devops.common.api.util

import org.junit.Assert
import org.junit.Test

class ReplacementUtilsTest {
    class Replacement(
        private val data: Map<String, String>
    ) : ReplacementUtils.KeyReplacement {
        override fun getReplacement(key: String): String? = if (data[key] != null) {
            data[key]!!
        } else {
            "\${$key}"
        }
    }

    @Test
    fun replace() {
        val command1 = "hello \${{variables.abc}} world"
        val command2 = "\${{variables.abc}}world"
        val command3 = "hello\${{variables.abc}}"
        val command4 = "hello\${{variables.abc"
        val command5 = "hello\${{variables.abc}"
        val command6 = "hello\${variables.abc}}"
        val command7 = "hello\$variables.abc}}"
        val command8 = "echo \${{ variables.hello }}"
        val data = mapOf(
            "variables.abc" to "variables.value",
            "variables.hello" to "hahahahaha"
        )

        Assert.assertEquals("hello variables.value world", ReplacementUtils.replace(command1, Replacement(data)))
        Assert.assertEquals("variables.valueworld", ReplacementUtils.replace(command2, Replacement(data)))
        Assert.assertEquals("hellovariables.value", ReplacementUtils.replace(command3, Replacement(data)))
        Assert.assertEquals("hello\${{variables.abc", ReplacementUtils.replace(command4, Replacement(data)))
        Assert.assertEquals("hello\${{variables.abc}", ReplacementUtils.replace(command5, Replacement(data)))
        Assert.assertEquals("hellovariables.value}", ReplacementUtils.replace(command6, Replacement(data)))
        Assert.assertEquals("hello\$variables.abc}}", ReplacementUtils.replace(command7, Replacement(data)))
        Assert.assertEquals("echo hahahahaha", ReplacementUtils.replace(command8, Replacement(data)))
    }
}
