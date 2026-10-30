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

package com.tencent.devops.process.yaml.parsers.utils

import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.process.yaml.v2.models.PreScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.PreTemplateScriptBuildYaml
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.process.yaml.v3.models.PreTemplateScriptBuildYamlV3Parser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

class ScriptYmlUtilsTest {

    @Test
    fun formatYaml() {
        val classPathResource = ClassPathResource("test.yml")
        val inputStream: InputStream = classPathResource.inputStream
        val isReader = InputStreamReader(inputStream)

        val reader = BufferedReader(isReader)
        val sb = StringBuffer()
        var str: String?
        while (reader.readLine().also { str = it } != null) {
            sb.append(str).append("\n")
        }
        val formatStr = ScriptYmlUtils.formatYaml(sb.toString())
        YamlUtil.getObjectMapper().readValue(formatStr, PreTemplateScriptBuildYaml::class.java)
    }

    @Test
    fun isV2Version() {
        val classPathResource = ClassPathResource("Sample1.yml")
        val inputStream: InputStream = classPathResource.inputStream
        val isReader = InputStreamReader(inputStream)

        val reader = BufferedReader(isReader)
        val sb = StringBuffer()
        var str: String?
        while (reader.readLine().also { str = it } != null) {
            sb.append(str).append("\n")
        }
        Assertions.assertEquals(ScriptYmlUtils.isV2Version(sb.toString()), true)
    }

    @Test
    fun variableTest() {
        val classPathResource = ClassPathResource("Sample1.yml")
        val inputStream: InputStream = classPathResource.inputStream
        val isReader = InputStreamReader(inputStream)

        val reader = BufferedReader(isReader)
        val sb = StringBuffer()
        var str: String?
        while (reader.readLine().also { str = it } != null) {
            sb.append(str).append("\n")
        }

        val obj = YamlUtil.getObjectMapper().readValue(
            ScriptYmlUtils.formatYaml(sb.toString()),
            PreScriptBuildYaml::class.java
        )

        obj.variables!!.forEach { t, u ->
            println("1111" + u.value)
            val settingMap = mapOf("sss" to "123", "approve22" to "ssdsdsd")
            println(formatVariablesValue(u.value!!, settingMap))
        }
        println(obj.variables)
    }

    private fun formatVariablesValue(value: String, settingMap: Map<String, String>): String {
        var newValue = value
        val pattern = Pattern.compile("\\$\\{\\{([^{}]+?)}}")
        val matcher = pattern.matcher(value)
        while (matcher.find()) {
            println("2222" + matcher.group(0))
            println("2222" + matcher.group(1))
            val realValue = settingMap[matcher.group(1).trim()]
            newValue = newValue.replace(matcher.group(), realValue!!)
        }
        return newValue
    }

    @Test
    fun preExtend2ExtendKeepVariableRawValue() {
        val classPathResource = ClassPathResource("Extend.yaml")
        val sb = StringBuffer()
        BufferedReader(InputStreamReader(classPathResource.inputStream)).use { reader ->
            var str: String?
            while (reader.readLine().also { str = it } != null) {
                sb.append(str).append("\n")
            }
        }

        val preYaml = YamlUtil.getObjectMapper().readValue(
            sb.toString(),
            PreTemplateScriptBuildYamlV3Parser::class.java
        )

        val extends = com.tencent.devops.process.yaml.v3.utils.ScriptYmlUtils
            .preExtend2Extend(preYaml.extends)

        // 模板实例变量:各类型都应保留原始类型,不被强制 toString
        val variables = extends?.template?.variables
        Assertions.assertNotNull(variables)
        // 核心用例:复选框数组值应保留为 List,而不是被 toString 破坏成 "[]"
        val checkbox = variables!!["v_checkbox"]
        Assertions.assertNotNull(checkbox)
        Assertions.assertTrue(checkbox!!.value is List<*>)
        Assertions.assertEquals(listOf(""), checkbox.value)
        Assertions.assertEquals("dd", variables["v_text"]?.value)
        Assertions.assertEquals("", variables["v_selector"]?.value)
        Assertions.assertEquals(true, variables["v_bool"]?.value)
        Assertions.assertTrue(variables["v_repo_ref"]?.value is Map<*, *>)

        // 定时任务(trigger-conf)的启动参数也应保留原始值
        val triggerConfig = extends?.template?.triggerConfig
        Assertions.assertNotNull(triggerConfig)
        val timer = triggerConfig!!["y9i5eU"]
        Assertions.assertNotNull(timer)
        Assertions.assertEquals(false, timer!!.disabled)
        Assertions.assertEquals("0 0 1 * *", timer.cron)
        val timerVariables = timer.variables
        Assertions.assertNotNull(timerVariables)
        Assertions.assertEquals("dd", timerVariables!!["v_text"])
        Assertions.assertEquals("1,2,3", timerVariables["v_checkbox"])
        Assertions.assertEquals("master", timerVariables["v_git_ref"])
    }
}
