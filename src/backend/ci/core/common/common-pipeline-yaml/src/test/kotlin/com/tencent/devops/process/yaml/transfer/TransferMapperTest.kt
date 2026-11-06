package com.tencent.devops.process.yaml.transfer

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.tencent.devops.process.yaml.pojo.TemplatePath
import com.tencent.devops.process.yaml.transfer.pojo.PublicVarGroupYamlParser
import com.tencent.devops.process.yaml.v3.models.IPreTemplateScriptBuildYamlParser
import com.tencent.devops.process.yaml.v3.models.ITemplateFilter
import com.tencent.devops.process.yaml.v3.parsers.template.YamlObjects
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * YAML 1.1 允许整数用 `_` 做分隔符，日期/编码类的值写成 2026_8_6 会被清成 202686。
 * 见 https://github.com/TencentBlueKing/bk-ci/issues/13618
 */
class TransferMapperTest {

    /**
     * 与 PipelineTransferYamlService.loadYaml 保持同一个入口类型，
     * 变量在这一步就落到 ITemplateFilter.variables 的 Map<String, Any> 上。
     */
    private fun loadYaml(yaml: String): ITemplateFilter {
        val parser = TransferMapper.getObjectMapper().readValue(
            yaml, object : TypeReference<IPreTemplateScriptBuildYamlParser>() {}
        )
        return parser as ITemplateFilter
    }

    @Suppress("UNCHECKED_CAST")
    private fun rawVariable(yaml: String, key: String): Map<String, Any> {
        return loadYaml(yaml).variables!![key] as Map<String, Any>
    }

    /**
     * 未注册下划线数字处理的对照 mapper。复用 TransferMapper 的 YAMLFactory，使两者差异严格
     * 收敛到该 module 一项（另建 YAMLFactory 会因 parser feature 默认值不同而产生噪音，
     * 例如空标量是 null 还是空串），所以除带下划线的数字外解析结果应完全一致。
     */
    private val baselineMapper = ObjectMapper(TransferMapper.getObjectMapper().factory)
        .registerKotlinModule()

    private fun baselineVariables(yaml: String): Map<String, Any>? {
        val parser = baselineMapper.readValue(
            yaml, object : TypeReference<IPreTemplateScriptBuildYamlParser>() {}
        )
        return (parser as ITemplateFilter).variables
    }

    private fun assertSameAsBaseline(yaml: String) {
        Assertions.assertEquals(baselineVariables(yaml), loadYaml(yaml).variables)
    }

    @Nested
    @DisplayName("带下划线的数字标量保留原文")
    inner class KeepUnderscoreNumber {

        @Test
        @DisplayName("variables 默认值不丢下划线")
        fun `variables value keeps underscore`() {
            val yaml = """
                version: v3.0
                variables:
                  v_date:
                    value: 2026_8_6
                    allow-modify-at-startup: false
                    props:
                      type: vuex-input
            """.trimIndent()

            Assertions.assertEquals("2026_8_6", rawVariable(yaml, "v_date")["value"])
        }

        @Test
        @DisplayName("转换为 Variable 后默认值仍是原始字符串")
        fun `getVariable keeps underscore`() {
            val yaml = """
                version: v3.0
                variables:
                  v_date:
                    value: 2026_8_6
                    props:
                      type: vuex-input
            """.trimIndent()

            val variable = YamlObjects.getVariable(
                fromPath = TemplatePath(".ci/pipeline.yml"),
                key = "v_date",
                variable = rawVariable(yaml, "v_date")
            )
            Assertions.assertEquals("2026_8_6", variable.value)
        }

        @Test
        @DisplayName("嵌套在 step.with 下的值也保留下划线")
        fun `nested step with keeps underscore`() {
            val yaml = """
                version: v3.0
                steps:
                  - uses: demo@1.*
                    with:
                      build_no: 2026_8_6
            """.trimIndent()

            val with = loadYaml(yaml).steps!![0]["with"] as Map<*, *>
            Assertions.assertEquals("2026_8_6", with["build_no"])
        }

        @Test
        @DisplayName("数组元素中的值也保留下划线")
        fun `underscore number in sequence keeps underscore`() {
            val yaml = """
                version: v3.0
                variables:
                  v_list:
                    value:
                      - 2026_8_6
                      - 2026_8_7
            """.trimIndent()

            Assertions.assertEquals(
                listOf("2026_8_6", "2026_8_7"),
                rawVariable(yaml, "v_list")["value"]
            )
        }

        @Test
        @DisplayName("带正负号的下划线数字保留原文")
        fun `signed underscore number keeps underscore`() {
            val yaml = """
                version: v3.0
                variables:
                  v_neg:
                    value: -2026_8_6
                  v_pos:
                    value: +2026_8_6
            """.trimIndent()

            Assertions.assertEquals("-2026_8_6", rawVariable(yaml, "v_neg")["value"])
            Assertions.assertEquals("+2026_8_6", rawVariable(yaml, "v_pos")["value"])
        }

        @Test
        @DisplayName("公共变量组直接反序列化 Variable 时同样保留下划线")
        fun `public var group keeps underscore`() {
            val yaml = """
                version: v3.0
                name: demo_group
                variables:
                  v_date:
                    value: 2026_8_6
                    props:
                      type: vuex-input
            """.trimIndent()

            val parser = TransferMapper.getObjectMapper().readValue(
                yaml, object : TypeReference<PublicVarGroupYamlParser>() {}
            )
            Assertions.assertEquals("2026_8_6", parser.variables["v_date"]!!.value)
        }
    }

    @Nested
    @DisplayName("其他标量类型保持原有解析结果")
    inner class PreserveOtherScalars {

        @Test
        @DisplayName("普通整数仍解析为数字")
        fun `plain integer stays number`() {
            val yaml = """
                version: v3.0
                variables:
                  v_int:
                    value: 123
            """.trimIndent()

            Assertions.assertEquals(123, rawVariable(yaml, "v_int")["value"])
        }

        @Test
        @DisplayName("浮点数仍解析为数字")
        fun `plain float stays number`() {
            val yaml = """
                version: v3.0
                variables:
                  v_float:
                    value: 1.5
            """.trimIndent()

            Assertions.assertEquals(1.5, rawVariable(yaml, "v_float")["value"])
        }

        @Test
        @DisplayName("布尔、字符串、空值与对照 mapper 结果一致")
        fun `other scalar types unchanged`() {
            val yaml = """
                version: v3.0
                variables:
                  v_bool:
                    value: true
                  v_str:
                    value: hello
                  v_empty:
                    value:
                  v_hex:
                    value: 0x1F
                  v_underscore_word:
                    value: _2026
            """.trimIndent()

            assertSameAsBaseline(yaml)
            Assertions.assertEquals(true, rawVariable(yaml, "v_bool")["value"])
            Assertions.assertEquals("hello", rawVariable(yaml, "v_str")["value"])
        }

        @Test
        @DisplayName("显式加引号的下划线数字仍是字符串")
        fun `quoted underscore number stays string`() {
            val yaml = """
                version: v3.0
                variables:
                  v_date:
                    value: "2026_8_6"
            """.trimIndent()

            Assertions.assertEquals("2026_8_6", rawVariable(yaml, "v_date")["value"])
        }

        @Test
        @DisplayName("嵌套结构与对照 mapper 结果一致")
        fun `nested container types unchanged`() {
            val yaml = """
                version: v3.0
                variables:
                  v_obj:
                    value:
                      repo-name: demo
                      items:
                        - 1
                        - 2
            """.trimIndent()

            assertSameAsBaseline(yaml)
            val value = rawVariable(yaml, "v_obj")["value"] as Map<*, *>
            Assertions.assertEquals("demo", value["repo-name"])
            Assertions.assertEquals(listOf(1, 2), value["items"])
        }

        @Test
        @DisplayName("对照 mapper 会丢下划线，确认差异只出在这一处")
        fun `baseline mapper still loses underscore`() {
            val yaml = """
                version: v3.0
                variables:
                  v_date:
                    value: 2026_8_6
            """.trimIndent()

            @Suppress("UNCHECKED_CAST")
            val baseline = baselineVariables(yaml)!!["v_date"] as Map<String, Any>
            Assertions.assertEquals(202686, baseline["value"])
            Assertions.assertEquals("2026_8_6", rawVariable(yaml, "v_date")["value"])
        }
    }

    /**
     * UI 模型转 YAML 时，多行脚本要能按行阅读，并且解析回来的字符串与原脚本一致。
     * 执行用的是解析后的脚本，不是 YAML 文本本身。
     */
    @Nested
    @DisplayName("多行脚本转成字面量块且内容不变")
    inner class MultilineScriptScalar {

        private fun assertSameScript(yaml: String, script: String) {
            val bySnake = TransferMapper.getYamlFactory().load(yaml) as Map<*, *>
            val byJackson = TransferMapper.getObjectMapper().readValue(
                yaml, object : TypeReference<Map<String, Any>>() {}
            )
            Assertions.assertEquals(script, bySnake["script"])
            Assertions.assertEquals(script, byJackson["script"])
        }

        private fun assertToYamlRoundTrip(script: String) {
            val yaml = TransferMapper.toYaml(mapOf("script" to script))
            assertSameScript(yaml, script)
            Assertions.assertEquals(yaml, TransferMapper.mergeYaml("", yaml))
        }

        @Test
        @DisplayName("带制表符的脚本按行展开，内容含制表符")
        fun `tab script is a literal block`() {
            val script = "echo hello\n\techo tab\n"
            val yaml = TransferMapper.toYaml(mapOf("script" to script))
            Assertions.assertTrue(yaml.contains("|"))
            Assertions.assertFalse(yaml.contains("\\n"))
            Assertions.assertTrue(yaml.contains("\t"))
            assertSameScript(yaml, script)
        }

        @Test
        @DisplayName("普通多行、引号、反斜杠、空行和中文原样往返")
        fun `common scripts round trip`() {
            assertToYamlRoundTrip("#!/bin/bash\necho \"hello: world\"\necho \${FOO}\n")
            assertToYamlRoundTrip("printf '%s\\n'\necho done\n")
            assertToYamlRoundTrip("echo hello\n\necho world\n")
            assertToYamlRoundTrip("echo hello\necho world")
            assertToYamlRoundTrip("echo 你好\n\n\n")
            assertToYamlRoundTrip("  echo hello\necho\n")
            assertToYamlRoundTrip("\necho hello\n")
            assertToYamlRoundTrip("echo 😀\nend\n")
        }

        @Test
        @DisplayName("回车换行不能写进字面量块，双引号结果仍是原字符串")
        fun `crlf script keeps carriage return`() {
            val script = "echo hello\r\necho world\r\n"
            assertToYamlRoundTrip(script)
        }

        @Test
        @DisplayName("其他控制字符保持原字符串")
        fun `control char script keeps original value`() {
            assertToYamlRoundTrip("echo\u0001x\nnext\n")
        }

        @Test
        @DisplayName("行尾空格仍按原有序列化去掉，制表符不去掉")
        fun `existing trailing space trim stays limited to spaces`() {
            val yaml = TransferMapper.toYaml(mapOf("script" to "echo hello \n\techo tab \n"))
            assertSameScript(yaml, "echo hello\n\techo tab\n")
        }

        @Test
        @DisplayName("已有双引号脚本展开后保留注释、锚点和行尾空格")
        fun `quoted yaml keeps comment anchor and trailing space`() {
            val commented = "script: \"echo hello\\necho world\\n\" # keep\n"
            val commentOut = TransferMapper.mergeYaml("", commented)
            Assertions.assertTrue(commentOut.contains("# keep"))
            assertSameScript(commentOut, "echo hello\necho world\n")

            val anchored = "script: &s \"echo hello\\necho world\\n\"\nalias: *s\n"
            val anchorOut = TransferMapper.mergeYaml("", anchored)
            Assertions.assertTrue(anchorOut.contains("&s"))
            val loaded = TransferMapper.getYamlFactory().load(anchorOut) as Map<*, *>
            Assertions.assertEquals("echo hello\necho world\n", loaded["script"])
            Assertions.assertEquals(loaded["script"], loaded["alias"])

            val spaced = "script: \"echo hello \\necho world\\n\"\n"
            assertSameScript(TransferMapper.mergeYaml("", spaced), "echo hello \necho world\n")
        }

        @Test
        @DisplayName("流式标量不改成字面量块，解析结果不变")
        fun `flow scalar stays quoted`() {
            val raw = "script: {text: \"echo hello\\necho world\\n\"}\n"
            val out = TransferMapper.mergeYaml("", raw)
            Assertions.assertTrue(out.contains("\\n"))
            val loaded = TransferMapper.getYamlFactory().load(out) as Map<*, *>
            val script = loaded["script"] as Map<*, *>
            Assertions.assertEquals("echo hello\necho world\n", script["text"])
        }
    }
}
