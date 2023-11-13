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

package com.tencent.devops.process.yaml.parsers.utils

import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.process.yaml.v2.models.PreScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.PreTemplateScriptBuildYaml
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
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
}
