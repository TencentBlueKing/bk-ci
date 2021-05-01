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

package com.tencent.devops.common.ci

import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.PreTemplateScriptBuildYaml
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.ci.v2.utils.YamlTemplateUtils
import org.junit.Test

import org.springframework.core.io.ClassPathResource
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class YamlTemplateUtilsTest {
    // 综合测试
    val testYaml = "pipelineWithTemplate.yml"
    val templateYamlList = listOf(
        "templates/stages.yml",
        "templates/jobs.yml",
        "templates/steps.yml",
        "templates/pipeline.yml",
        "templates/variables.yml",
        "cyclic/variable/templates/variable1.yml",
        "cyclic/variable/templates/variable2.yml",
        "cyclic/stage/templates/stage1.yml",
        "cyclic/stage/templates/stage2.yml",
        "cyclic/job/templates/job1.yml",
        "cyclic/job/templates/job2.yml",
        "cyclic/job/templates/job3.yml",
        "cyclic/job/templates/job4.yml",
        "cyclic/job/templates/job5.yml",
        "cyclic/step/templates/step1.yml",
        "cyclic/step/templates/step2.yml",
        "cyclic/step/templates/step3.yml",
        "cyclic/step/templates/step1.yml",
        "cyclic/step/templates/step2.yml",
        "cyclic/step/templates/step3.yml",
        "cyclic/step/templates/step4.yml",
        "cyclic/step/templates/step5.yml",
        "cyclic/step/templates/step6.yml",
        "cyclic/step/templates/step7.yml",
        "cyclic/step/templates/step8.yml"
    )
//    // 测试step循环嵌套
//    val testYaml = "/cyclic/step/pipeline.yml"
//    val templateYamlList = listOf(
//        "cyclic/step/templates/step1.yml",
//        "cyclic/step/templates/step2.yml",
//        "cyclic/step/templates/step3.yml",
//        "cyclic/step/templates/step4.yml",
//        "cyclic/step/templates/step5.yml",
//        "cyclic/step/templates/step6.yml",
//        "cyclic/step/templates/step7.yml",
//        "cyclic/step/templates/step8.yml"
//    )
//    // 测试job循环嵌套
//    val testYaml = "/cyclic/job/pipeline.yml"
//    val templateYamlList = listOf(
//        "cyclic/job/templates/job1.yml",
//        "cyclic/job/templates/job2.yml",
//        "cyclic/job/templates/job3.yml",
//        "cyclic/job/templates/job4.yml",
//        "cyclic/job/templates/job5.yml",
//        "cyclic/step/templates/step1.yml",
//        "cyclic/step/templates/step2.yml",
//        "cyclic/step/templates/step3.yml"
//    )

    @Test
    fun test() {
        val classPathResource = ClassPathResource(testYaml)
        val inputStream: InputStream = classPathResource.inputStream
        val isReader = InputStreamReader(inputStream)

        val reader = BufferedReader(isReader)
        val sb = StringBuffer()
        var str: String?
        while (reader.readLine().also { str = it } != null) {
            sb.append(str).append("\n")
        }

        val yaml = ScriptYmlUtils.formatYaml(sb.toString())
        val preTemplateYamlObject = YamlUtil.getObjectMapper().readValue(yaml, PreTemplateScriptBuildYaml::class.java)

        println(
            YamlUtil.toYaml(
                YamlTemplateUtils(
                    yamlObject = preTemplateYamlObject,
                    templates = getAllTemplates(),
                    rootPath = testYaml
                ).replace()
            )
        )
    }

    private fun getAllTemplates(): Map<String, String?> {
        val pathList = templateYamlList
        val yamlList = mutableMapOf<String, String>()
        pathList.forEach {
            val classPathResource = ClassPathResource(it)
            val inputStream: InputStream = classPathResource.inputStream
            val isReader = InputStreamReader(inputStream)

            val reader = BufferedReader(isReader)
            val sb = StringBuffer()
            var str: String?
            while (reader.readLine().also { str = it } != null) {
                sb.append(str).append("\n")
            }
            yamlList[it] = sb.toString()
        }
        return yamlList
    }
}
