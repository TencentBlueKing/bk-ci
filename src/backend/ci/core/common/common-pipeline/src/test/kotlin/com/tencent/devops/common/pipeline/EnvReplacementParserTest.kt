package com.tencent.devops.common.pipeline

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@Suppress("ALL")
internal class EnvReplacementParserTest {

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
        val buff = EnvReplacementParser.parse(template2, data)
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
        val buff = EnvReplacementParser.parse(template, contextMap.plus(data))
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
        // 与EnvUtils的差异点：不支持传可空对象
//        Assertions.assertEquals("", EnvReplacementParser.parse(null, data))
        Assertions.assertEquals("", EnvReplacementParser.parse("", data))
        Assertions.assertNull(EnvReplacementParser.parse(null, data))

        Assertions.assertEquals(
            "hello variables.value world",
            EnvReplacementParser.parse(command1, data)
        )
        Assertions.assertEquals(
            "variables.valueworld",
            EnvReplacementParser.parse(command2, data)
        )
        Assertions.assertEquals(
            "hellovariables.value",
            EnvReplacementParser.parse(command3, data)
        )
        Assertions.assertEquals(
            "hello\${{variables.abc",
            EnvReplacementParser.parse(command4, data)
        )
        Assertions.assertEquals(
            "hello\${{variables.abc}",
            EnvReplacementParser.parse(command5, data)
        )
        Assertions.assertEquals(
            "hellovariables.value}",
            EnvReplacementParser.parse(command6, data)
        )
        Assertions.assertEquals(
            "hello\$variables.abc}}",
            EnvReplacementParser.parse(command7, data)
        )
        Assertions.assertEquals(
            "echo hahahahaha",
            EnvReplacementParser.parse(command8, data)
        )
        Assertions.assertEquals(
            "echo /data/landun/workspace",
            EnvReplacementParser.parse(
                obj = command9,
                contextMap = map.plus("ci.workspace" to "/data/landun/workspace")
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
            EnvReplacementParser.parse(command, map)
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
            "variables.hello" to "hahahahaha",
            "{variables.abc" to "jacky"
        )

        Assertions.assertEquals("hello variables.value world", EnvReplacementParser.parse(command1, data))
        Assertions.assertEquals("variables.valueworld", EnvReplacementParser.parse(command2, data))
        Assertions.assertEquals("hellovariables.value", EnvReplacementParser.parse(command3, data))
        Assertions.assertEquals("hello\${{variables.abc", EnvReplacementParser.parse(command4, data))
        Assertions.assertEquals("hellojacky", EnvReplacementParser.parse(command5, data))
        Assertions.assertEquals("hellovariables.value}", EnvReplacementParser.parse(command6, data))
        Assertions.assertEquals("hello\$variables.abc}}", EnvReplacementParser.parse(command7, data))
        Assertions.assertEquals("echo hahahahaha", EnvReplacementParser.parse(command8, data))
        Assertions.assertEquals(
            "echo /data/landun/workspace || hahahahaha",
            EnvReplacementParser.parse(
                obj = command9,
                contextMap = data.plus("ci.workspace" to "/data/landun/workspace")
            )
        )
    }

    @Test
    fun parseExpressionTestData() {
        val map = mutableMapOf<String, String>()
        map["age"] = ""
        map["name"] = "jacky"
        val command = "{\"age\": \${age} , \"sex\": \"boy\", \"name\": \${name}}"
        println("parseEnvTestData $command")
        Assertions.assertEquals(
            "{\"age\": ${map["age"]} , \"sex\": \"boy\", \"name\": ${map["name"]}}",
            EnvReplacementParser.parse(command, map, true)
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
        val command10 = "echo \${{ ci.xyz == 'zzzz' }}"
        val command11 = "echo \${{ variables.xyz == 'zzzz' }}"
        val data = mapOf(
            "variables.abc" to "variables.value",
            "variables.xyz" to "zzzz",
            "variables.hello" to "hahahahaha",
            "{variables.abc" to "jacky"
        )

        Assertions.assertEquals("hello variables.value world", EnvReplacementParser.parse(command1, data, true))
        Assertions.assertEquals("variables.valueworld", EnvReplacementParser.parse(command2, data, true))
        Assertions.assertEquals("hellovariables.value", EnvReplacementParser.parse(command3, data, true))
        Assertions.assertEquals("hello\${{variables.abc", EnvReplacementParser.parse(command4, data, true))
        Assertions.assertEquals("hellojacky", EnvReplacementParser.parse(command5, data, true))
        Assertions.assertEquals("hellovariables.value}", EnvReplacementParser.parse(command6, data, true))
        Assertions.assertEquals("hello\$variables.abc}}", EnvReplacementParser.parse(command7, data, true))
        Assertions.assertEquals("echo hahahahaha", EnvReplacementParser.parse(command8, data, true))
        Assertions.assertEquals(
            "echo /data/landun/workspace || hahahahaha",
            EnvReplacementParser.parse(
                obj = command9,
                contextMap = data.plus("ci.workspace" to "/data/landun/workspace"),
                onlyExpression = true
            )
        )
        Assertions.assertEquals("echo \${{ ci.xyz == 'zzzz' }}", EnvReplacementParser.parse(command10, data, true))
        Assertions.assertEquals("echo true", EnvReplacementParser.parse(command11, data, true))
    }
}
