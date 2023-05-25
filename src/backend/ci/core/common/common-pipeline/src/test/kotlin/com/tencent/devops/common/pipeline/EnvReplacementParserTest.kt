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
        Assertions.assertEquals("", EnvReplacementParser.parse(null, data))

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
                value = command9,
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
            "variables.hello" to "hahahahaha"
        )

        Assertions.assertEquals("hello variables.value world", EnvReplacementParser.parse(command1, data))
        Assertions.assertEquals("variables.valueworld", EnvReplacementParser.parse(command2, data))
        Assertions.assertEquals("hellovariables.value", EnvReplacementParser.parse(command3, data))
        Assertions.assertEquals("hello\${{variables.abc", EnvReplacementParser.parse(command4, data))
        Assertions.assertEquals("hello\${{variables.abc}", EnvReplacementParser.parse(command5, data))
        Assertions.assertEquals("hellovariables.value}", EnvReplacementParser.parse(command6, data))
        Assertions.assertEquals("hello\$variables.abc}}", EnvReplacementParser.parse(command7, data))
        Assertions.assertEquals("echo hahahahaha", EnvReplacementParser.parse(command8, data))
        Assertions.assertEquals(
            "echo /data/landun/workspace || hahahahaha",
            EnvReplacementParser.parse(
                value = command9,
                contextMap = data.plus("ci.workspace" to "/data/landun/workspace")
            )
        )
    }

    @Test
    fun parseExpressionTwice() {
        val map = mutableMapOf<String, String>()
        map["GDP"] = "10000000"
        map["People"] = "1400000000"
        map["Country"] = "中华人民共和国"
        map["twice"] = "\${{Country}}" // twice 二次解析
        map["variables.TXT"] = "txt"
        map["variables.TXT2"] = "txt2"

        parseAndEquals(
            data = map,
            template = "\${{ (variables.TXT == 'txt') }}",
            expect = true.toString(),
            onlyExpression = true
        )

        parseAndEquals(
            data = map,
            template = "\${{ (variables.TXT2 == 'txt2') }}",
            expect = true.toString(),
            onlyExpression = true
        )

        parseAndEquals(
            data = map,
            template = "\${{ ((variables.TXT == 'txt') && (variables.TXT2 == 'txt2')) }}",
            expect = true.toString(),
            onlyExpression = true
        )

        parseAndEquals(
            data = map,
            template = "echo \" 这可是来自master的改动 \"\n" +
                "          echo \"  \${{ ('txt' == 'txt') }}  \"\n" +
                "          echo \"  \${{ (variables.TXT2 == 'txt2') }}  \"\n" +
                "          echo \"  \${{ (('txt' == 'txt') && (variables.TXT2 == 'txt2')) }}  \"",
            expect = "echo \" 这可是来自master的改动 \"\n" +
                "          echo \"  true  \"\n" +
                "          echo \"  true  \"\n" +
                "          echo \"  true  \"",
            onlyExpression = true
        )

        parseAndEquals(
            data = map,
            template = "echo \" 这可是来自master的改动 \"\n" +
                "echo \"  \${{ (\${{ variables.TXT2 }} == 'txt') }}  \"\n" +
                "echo \"  \${{ (variables.TXT2 == \${{ variables.TXT }}) }}  \"\n" +
                "echo \"  \${{ ((\${{ variables.TXT2 }} == 'txt') && (variables.TXT2 == \${{ variables.TXT }})) }}  \"",
            expect = "echo \" 这可是来自master的改动 \"\n" +
                "echo \"  false  \"\n" +
                "echo \"  false  \"\n" +
                "echo \"  false  \"",
            onlyExpression = true
        )

        parseAndEquals(
            data = map,
            template = "{\"GDP\": \"\${{GDP}}亿\", \"People\": \${{People}} \"Country\": \"\${{twice}}\"}",
            expect = "{\"GDP\": \"${map["GDP"]}亿\", \"People\": ${map["People"]} \"Country\": \"${map["Country"]}\"}",
            onlyExpression = true
        )

        val data = HashMap<String, String>()
        data["ab3c"] = "123"
        data["ab.cd"] = "5678"
        data["t.cd"] = "\${{ab.cd}}"

        val template2 = "abcd_\$abc}_ffs_\${{\${{ce}}_\${{ab.c}_ end"
        val buff = EnvReplacementParser.parse(template2, data, true)
        Assertions.assertEquals(template2, buff)

        parseAndEquals(
            data = data,
            template = "中国\$abc}_ffs_\${{\${{ce}}_\${{ab.c}_ end",
            expect = "中国\$abc}_ffs_\${{\${{ce}}_\${{ab.c}_ end",
            onlyExpression = true
        )

        parseAndEquals(
            data = data,
            template = "abcd_\${abc}_ffs_\${{ce}}_\${{t.cd}}_ end结束%\n # 这是注释行a1\$ab_^%!#@",
            expect = "abcd_\${abc}_ffs_twice_${data["ab.cd"]}_ end结束%\n # 这是注释行a1\$ab_^%!#@",
            contextMap = mapOf("ce" to "twice"),
            onlyExpression = true
        )

        data["c_e"] = "\${none}"
        parseAndEquals(
            data = data,
            template = "abcd_\${abc}_ffs_\${{c_e}}_\${{t.cd}}_ end",
            expect = "abcd_\${abc}_ffs_\${none}_${data["ab.cd"]}_ end",
            onlyExpression = true
        )

        data["center中"] = "中国"
        // 新表达式中中文不支持作为key
        parseAndEquals(
            data = data,
            template = "abcd_\${{center中}}_ffs",
            expect = "abcd_\${{center中}}_ffs",
            onlyExpression = true
        )

        data["blank"] = ""
        parseAndEquals(
            data = data,
            template = "\${{blank}}",
            expect = "",
            onlyExpression = true
        )

        data["all"] = "hello"
        parseAndEquals(
            data = data,
            template = "\${{all}}",
            expect = "hello",
            onlyExpression = true
        )
    }

    @Test
    fun complexExpressionTest() {
        val contextMap = HashMap<String, String>()
        contextMap["envs.env_a"] = "a"
        contextMap["envs.env_b"] = "b"
        contextMap["envs.env_c"] = "c"
        contextMap["envs.env_d"] = "d"
        contextMap["envs.env_e"] = "e"
        parseAndEquals(
            data = contextMap,
            template = "env\n" +
                "echo 引用不存在的系统变量 \\\${bcz}=\${bcz}\n" +
                "echo 引用不存在的蓝盾变量\\\\$\\{\\{bczd}}=\${{ bczd }}\n" +
                "echo \${{ ci.workspace }}\n" +
                "echo envs.env_a=\${{ envs.env_a }}, env_a=\$env_a\n" +
                "echo envs.env_b=\${{ envs.env_b }}, env_b=\$env_b\n" +
                "echo envs.env_c=\${{ envs.env_c }}, env_c=\$env_c\n" +
                "echo envs.env_d=\${{ envs.env_d }}, env_d=\$env_d\n" +
                "echo envs.env_e=\${{ envs.env_e }}, env_e=\$env_e\n" +
                "echo envs.a=\${{ envs.a }}, a=\$a\n" +
                "echo settings.sensitive.password=\${{ settings.sensitive.password }}\n" +
                "echo ::set-output name=a::i am a at step_1",
            expect = "env\n" +
                "echo 引用不存在的系统变量 \\\${bcz}=\${bcz}\n" +
                "echo 引用不存在的蓝盾变量\\\\\$\\{\\{bczd}}=\${{ bczd }}\n" +
                "echo \${{ ci.workspace }}\n" +
                "echo envs.env_a=a, env_a=\$env_a\n" +
                "echo envs.env_b=b, env_b=\$env_b\n" +
                "echo envs.env_c=c, env_c=\$env_c\n" +
                "echo envs.env_d=d, env_d=\$env_d\n" +
                "echo envs.env_e=e, env_e=\$env_e\n" +
                "echo envs.a=, a=\$a\n" +
                "echo settings.sensitive.password=\${{ settings.sensitive.password }}\n" +
                "echo ::set-output name=a::i am a at step_1",
            onlyExpression = true
        )
    }

    @Test
    fun parseExpressionTestContextMap() {
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
        Assertions.assertEquals("", EnvReplacementParser.parse("", data, true))
        Assertions.assertEquals("", EnvReplacementParser.parse(null, data, true))

        Assertions.assertEquals(
            "hello variables.value world",
            EnvReplacementParser.parse(command1, data, true)
        )
        Assertions.assertEquals(
            "variables.valueworld",
            EnvReplacementParser.parse(command2, data, true)
        )
        Assertions.assertEquals(
            "hellovariables.value",
            EnvReplacementParser.parse(command3, data, true)
        )
        Assertions.assertEquals(
            "hello\${{variables.abc",
            EnvReplacementParser.parse(command4, data, true)
        )
        Assertions.assertEquals(
            "hello\${{variables.abc}",
            EnvReplacementParser.parse(command5, data, true)
        )
        Assertions.assertEquals(
            command6,
            EnvReplacementParser.parse(command6, data, true)
        )
        Assertions.assertEquals(
            "hello\$variables.abc}}",
            EnvReplacementParser.parse(command7, data, true)
        )
        Assertions.assertEquals(
            "echo hahahahaha",
            EnvReplacementParser.parse(command8, data, true)
        )
        Assertions.assertEquals(
            "echo /data/landun/workspace",
            EnvReplacementParser.parse(
                value = command9,
                contextMap = map.plus("ci.workspace" to "/data/landun/workspace"),
                onlyExpression = true
            )
        )
    }

    @Test
    fun parseExpressionTestData() {
        val map = mutableMapOf<String, String>()
        map["age"] = ""
        map["name"] = "jacky"
        val command = "{\"age\": \${{age}} , \"sex\": \"boy\", \"name\": \${{name}}}"
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
        val command12 = "echo \${{ strToTime(variables.date) > strToTime('2023-03-16 12:06:21') }}"
        val command13 = "echo \${{ strToTime(variables.date) > strToTime('2023-03-14 12:06:21') }}"
        val command14 = "\${{ strToTime(\${{variables.date}}) > strToTime('2023-03-14 12:06:21') }}"
        val data = mapOf(
            "variables.abc" to "variables.value",
            "variables.xyz" to "zzzz",
            "variables.date" to "2023-03-15 12:06:22",
            "variables.hello" to "hahahahaha",
            "{variables.abc" to "jacky"
        )

        Assertions.assertEquals("hello variables.value world", EnvReplacementParser.parse(command1, data, true))
        Assertions.assertEquals("variables.valueworld", EnvReplacementParser.parse(command2, data, true))
        Assertions.assertEquals("hellovariables.value", EnvReplacementParser.parse(command3, data, true))
        Assertions.assertEquals("hello\${{variables.abc", EnvReplacementParser.parse(command4, data, true))
        Assertions.assertEquals("hello\${{variables.abc}", EnvReplacementParser.parse(command5, data, true))
        Assertions.assertEquals("hello\${variables.abc}}", EnvReplacementParser.parse(command6, data, true))
        Assertions.assertEquals("hello\$variables.abc}}", EnvReplacementParser.parse(command7, data, true))
        Assertions.assertEquals("echo hahahahaha", EnvReplacementParser.parse(command8, data, true))
        Assertions.assertEquals(
            "echo /data/landun/workspace || hahahahaha",
            EnvReplacementParser.parse(
                value = command9,
                contextMap = data.plus("ci.workspace" to "/data/landun/workspace"),
                onlyExpression = true
            )
        )
        Assertions.assertEquals("echo \${{ ci.xyz == 'zzzz' }}", EnvReplacementParser.parse(command10, data, true))
        Assertions.assertEquals("echo true", EnvReplacementParser.parse(command11, data, true))
        Assertions.assertEquals("echo false", EnvReplacementParser.parse(command12, data, true))
        Assertions.assertEquals("echo true", EnvReplacementParser.parse(command13, data, true))
        Assertions.assertEquals("true", EnvReplacementParser.parse(command14, data, true))
    }

    @Test
    fun parseExpressionTestData1() {
        val command1 = """
let variables = {
  "is_lint": ${'$'}{{ variables.is_lint }},
  "is_build": ${'$'}{{ variables.is_build }}
}
// 如果没有设置相关变量
for (const key in variables) {
  variables[key] = variables[key].trim()
  if (variables[key].includes("${'$'}{{")) variables[key] = ""
  if (variables[key] == "none") variables[key] = ""
}

console.log("全局配置", variables)
let branch = ${'$'}{{ ci.branch }}
let branch1 = ${'$'}{{ ci.branch1 }}
let branchs = branch.split("/")"""
        val data = mapOf(
            "variables.is_lint" to "true",
            "variables.is_build" to "false",
            "ci.branch" to "master"
        )
        val result = """
let variables = {
  "is_lint": true,
  "is_build": false
}
// 如果没有设置相关变量
for (const key in variables) {
  variables[key] = variables[key].trim()
  if (variables[key].includes("${'$'}{{")) variables[key] = ""
  if (variables[key] == "none") variables[key] = ""
}

console.log("全局配置", variables)
let branch = master
let branch1 = 
let branchs = branch.split("/")"""
        Assertions.assertEquals(result, EnvReplacementParser.parse(command1, data, true))
    }

    private fun parseAndEquals(
        data: Map<String, String>,
        template: String,
        expect: String,
        contextMap: Map<String, String> = emptyMap(),
        onlyExpression: Boolean? = false
    ) {
        val buff = EnvReplacementParser.parse(template, contextMap.plus(data), onlyExpression)
        println("template=$template\nreplaced=$buff\n")
        Assertions.assertEquals(expect, buff)
    }
}
