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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class EnvUtilTest {

    @Test
    fun parseEnvNested() {
        val data = HashMap<String, String>()
        data["Nested"] = "first"
        data["first"] = "hello"
        parseAndEquals(data = data, template = "\${\${Nested}}.html", expect = "hello.html")
    }

    @Test
    fun parseEnvTwice() {
        val map = mutableMapOf<String, String>()
        map["GDP"] = "10000000"
        map["People"] = "1400000000"
        map["Country"] = "中华人民共和国"
        map["twice"] = "\${Country}" // twice 二次解析

        parseAndEquals(
            data = map,
            template = "{\"GDP\": \"\${{GDP}}亿\", \"People\": \${{People}} \"Country\": \"\${twice}\"}",
            expect = "{\"GDP\": \"${map["GDP"]}亿\", \"People\": ${map["People"]} \"Country\": \"${map["Country"]}\"}"
        )

        val data = HashMap<String, String>()
        data["ab3c"] = "123"
        data["ab.cd"] = "5678"
        data["t.cd"] = "\${ab.cd}"

        val template2 = "abcd_\$abc}_ffs_\${{\${{ce}}_\${{ab.c}_ end"
        val buff = EnvUtils.parseEnv(template2, data)
        Assertions.assertEquals(template2, buff)

        parseAndEquals(
            data = data,
            template = "中国\$abc}_ffs_\${{\${{ce}}_\${{ab.c}_ end",
            expect = "中国\$abc}_ffs_\${{\${{ce}}_\${{ab.c}_ end"
        )

        parseAndEquals(
            data = data,
            template = "abcd_\${abc}_ffs_\${{ce}}_\${{t.cd}}_ end结束%\n # 这是注释行a1\$ab_^%!#@",
            expect = "abcd_\${abc}_ffs_twice_${data["ab.cd"]}_ end结束%\n # 这是注释行a1\$ab_^%!#@",
            contextMap = mapOf("ce" to "twice")
        )

        data["c_e"] = "\${none}"
        parseAndEquals(
            data = data,
            template = "abcd_\${abc}_ffs_\${{c_e}}_\${{t.cd}}_ end",
            expect = "abcd_\${abc}_ffs_\${none}_${data["ab.cd"]}_ end"
        )

        data["center中"] = "中国"
        parseAndEquals(data = data, template = "abcd_\${center中}_ffs", expect = "abcd_中国_ffs")

        data["blank"] = ""
        parseAndEquals(data = data, template = "\${blank}", expect = "")

        data["all"] = "hello"
        parseAndEquals(data = data, template = "\${all}", expect = "hello")
    }

    private fun parseAndEquals(
        data: Map<String, String>,
        template: String,
        expect: String,
        contextMap: Map<String, String> = emptyMap()
    ) {
        val buff = EnvUtils.parseEnv(template, data, contextMap = contextMap)
        println("template=$template\nreplaced=$buff\n")
        Assertions.assertEquals(expect, buff)
    }

    @Test
    fun parseEnvTestContextMap() {
        val map = mutableMapOf<String, String>()
        map["age"] = "1"
        map["name"] = "jacky"

        val command1 = "hello \${{variables.abc}} world"
        val command2 = "\${{variables.abc}}world"
        val command3 = "hello\${{variables.abc}}"
        val command4 = "hello\${{variables.abc"
        val command5 = "hello\${{variables.abc}"
        val command6 = "hello\${variables.abc}}"
        val command7 = "hello\$variables.abc}}"
        val command8 = "echo \${{ variables.hello }}"

        val command9 = "echo \${{ ci.workspace }}"

        val data = mapOf(
            "variables.abc" to "variables.value",
            "variables.hello" to "hahahahaha"
        )

        Assertions.assertEquals("", EnvUtils.parseEnv(null, emptyMap(), contextMap = data))
        Assertions.assertEquals("", EnvUtils.parseEnv("", emptyMap(), contextMap = data))

        Assertions.assertEquals(
            "hello variables.value world",
            EnvUtils.parseEnv(command1, emptyMap(), contextMap = data)
        )
        Assertions.assertEquals(
            "variables.valueworld",
            EnvUtils.parseEnv(command2, emptyMap(), contextMap = data)
        )
        Assertions.assertEquals(
            "hellovariables.value",
            EnvUtils.parseEnv(command3, emptyMap(), contextMap = data)
        )
        Assertions.assertEquals(
            "hello\${{variables.abc",
            EnvUtils.parseEnv(command4, emptyMap(), contextMap = data)
        )
        Assertions.assertEquals(
            "hello\${{variables.abc}",
            EnvUtils.parseEnv(command5, emptyMap(), contextMap = data)
        )
        Assertions.assertEquals(
            "hellovariables.value}",
            EnvUtils.parseEnv(command6, emptyMap(), contextMap = data)
        )
        Assertions.assertEquals(
            "hello\$variables.abc}}",
            EnvUtils.parseEnv(command7, emptyMap(), contextMap = data)
        )
        Assertions.assertEquals(
            "echo hahahahaha",
            EnvUtils.parseEnv(command8, emptyMap(), contextMap = data)
        )
        Assertions.assertEquals(
            "echo /data/landun/workspace",
            EnvUtils.parseEnv(
                command = command9,
                data = map,
                isEscape = true,
                contextMap = mapOf("ci.workspace" to "/data/landun/workspace")
            )
        )
    }

    @Test
    fun parseEnvTestData() {
        val map = mutableMapOf<String, String>()
        map["age"] = ""
        map["name"] = "jacky"
        val command = "{\"age\": \${age} , \"sex\": \"boy\", \"name\": \${name}}"
        println("parseEnvTestData $command")
        Assertions.assertEquals(
            "{\"age\": ${map["age"]} , \"sex\": \"boy\", \"name\": ${map["name"]}}",
            EnvUtils.parseEnv(command, map)
        )

        val command1 = "hello \${{variables.abc}} world"
        val command2 = "\${{variables.abc}}world"
        val command3 = "hello\${{variables.abc}}"
        val command4 = "hello\${{variables.abc"
        val command5 = "hello\${{variables.abc}"
        val command6 = "hello\${variables.abc}}"
        val command7 = "hello\$variables.abc}}"
        val command8 = "echo \${{ variables.hello }}"

        val command9 = "echo \${{ ci.workspace }} || \${{variables.hello}}"

        val data = mapOf(
            "variables.abc" to "variables.value",
            "variables.hello" to "hahahahaha"
        )

        Assertions.assertEquals("hello variables.value world", EnvUtils.parseEnv(command1, data))
        Assertions.assertEquals("variables.valueworld", EnvUtils.parseEnv(command2, data))
        Assertions.assertEquals("hellovariables.value", EnvUtils.parseEnv(command3, data))
        Assertions.assertEquals("hello\${{variables.abc", EnvUtils.parseEnv(command4, data))
        Assertions.assertEquals("hello\${{variables.abc}", EnvUtils.parseEnv(command5, data))
        Assertions.assertEquals("hellovariables.value}", EnvUtils.parseEnv(command6, data))
        Assertions.assertEquals("hello\$variables.abc}}", EnvUtils.parseEnv(command7, data))
        Assertions.assertEquals("echo hahahahaha", EnvUtils.parseEnv(command8, data))
        Assertions.assertEquals(
            "echo /data/landun/workspace || hahahahaha",
            EnvUtils.parseEnv(
                command = command9,
                data = data,
                replaceWithEmpty = false,
                isEscape = false,
                contextMap = mapOf("ci.workspace" to "/data/landun/workspace")
            )
        )
    }
}
