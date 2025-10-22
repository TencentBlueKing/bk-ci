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

package com.tencent.devops.common.api.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@Suppress("ALL")
class TemplateFastReplaceUtilsTest {

    @Test
    fun replaceTemplate() {
        val map = mutableMapOf<String, String>()
        map["GDP"] = "\${{GDP}}"
        map["Peple"] = "\${{Peple}}"
        map["country"] = "\${country}"
        val template = "{\"GDP\": \"${map["GDP"]}亿\", \"Peple\": ${map["Peple"]} \"country\": \"${map["country"]}\"}"
        println("before=$template")

        map["GDP"] = "10000000"
        map["Peple"] = "1400000000"
        map["country"] = "中华人民共和国"
        val expect = "{\"GDP\": \"${map["GDP"]}亿\", \"Peple\": ${map["Peple"]} \"country\": \"${map["country"]}\"}"
        val replaceTemplate = TemplateFastReplaceUtils.replaceTemplate(template) { templateWord ->
            map[templateWord]
        }
        println("expect=$replaceTemplate")
        Assertions.assertEquals(expect, replaceTemplate)

        val dataSource = mapOf("variables.abc" to "v1", "variables.hello" to "v2", "ci.workspace" to "/data")
        val replacer: (String) -> String? = { dataSource[it] }

        val command1 = "hello \${{variables.abc}} world \${{ci.workspace}}" to "hello v1 world /data"
        Assertions.assertEquals(command1.second, TemplateFastReplaceUtils.replaceTemplate(command1.first, replacer))

        val command2 = "\${{variables.abc}}world" to "${dataSource["variables.abc"]}world"
        Assertions.assertEquals(command2.second, TemplateFastReplaceUtils.replaceTemplate(command2.first, replacer))

        val command3 = "hello\${{variables.abc}}" to "hello${dataSource["variables.abc"]}"
        Assertions.assertEquals(command3.second, TemplateFastReplaceUtils.replaceTemplate(command3.first, replacer))

        val command4 = "hello\${{variables.abc" to "hello\${{variables.abc"
        Assertions.assertEquals(command4.second, TemplateFastReplaceUtils.replaceTemplate(command4.first, replacer))

        val command5 = "hello\${{variables.abc}" to "hello\${{variables.abc}"
        Assertions.assertEquals(command5.second, TemplateFastReplaceUtils.replaceTemplate(command5.first, replacer))

        val command6 = "hello\${variables.abc}}" to "hello${dataSource["variables.abc"]}}"
        Assertions.assertEquals(command6.second, TemplateFastReplaceUtils.replaceTemplate(command6.first, replacer))

        val command7 = "hello\$variables.abc}}" to "hello\$variables.abc}}"
        Assertions.assertEquals(command7.second, TemplateFastReplaceUtils.replaceTemplate(command7.first, replacer))

        val command8 = "echo \${{ variables.hello }}" to "echo ${dataSource["variables.hello"]}"
        Assertions.assertEquals(command8.second, TemplateFastReplaceUtils.replaceTemplate(command8.first, replacer))

        val command9 = "echo \${{\${{ci.workspace }} \${\${variables.abc}\"" to
            "echo \${{${dataSource["ci.workspace"]} \${${dataSource["variables.abc"]}\""
        Assertions.assertEquals(command9.second, TemplateFastReplaceUtils.replaceTemplate(command9.first, replacer))

        val command10 = null to ""
        Assertions.assertEquals(command10.second, TemplateFastReplaceUtils.replaceTemplate(command10.first, replacer))
    }
}
