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

package com.tencent.devops.process.yaml.parsers.template

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.process.yaml.v2.models.PreTemplateScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.ResourcesPools
import com.tencent.devops.process.yaml.v2.models.format
import com.tencent.devops.process.yaml.v2.parsers.template.YamlTemplate
import com.tencent.devops.process.yaml.v2.parsers.template.models.GetTemplateParam
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.process.yaml.v2.utils.YamlCommonUtils
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader

@Suppress("LoopWithTooManyJumpStatements")
class YamlTemplateTest {

    val sampleDir = "samples"

    @Test
    fun testAllTemplate() {
        val dir = "all"
        check("$sampleDir/$dir/all.yml")
    }

    @Test
    fun testStagesTemplate() {
        val dir = "stages"
        check("$sampleDir/$dir/stages.yml")
    }

    @Test
    fun testJobsTemplate() {
        val dir = "jobs"
        check("$sampleDir/$dir/jobs.yml")
    }

    @Test
    fun testStepsTemplate() {
        val dir = "steps"
        check("$sampleDir/$dir/stepss.yml")
    }

    @Test
    fun testExtendsTemplate() {
        val dir = "extends"
        check("$sampleDir/$dir/extends.yml")
    }

    @Test
    fun testSpecialsTemplate() {
        val dir = "specials"
        check("$sampleDir/$dir/longParametersTest.yml")

        check("$sampleDir/$dir/user.yml")

        // resource
        val resourceExt = mutableMapOf<String, ResourcesPools>()
        replace("$sampleDir/$dir/resource/resources.yml", resourceExt)
        assert(
            resourceExt.keys.equalss(
                mutableListOf(
                    "lawrenzhang_testgroup/int/test_ci_temp@rezbuild+rezbuild",
                    "xxxxx/int/test_ci_temp@rezbuild+rezbuild"
                )
            )
        )
        resourceExt.clear()
        replace("$sampleDir/$dir/resource/resource-remote.yml", resourceExt)
        assert(
            resourceExt.keys.equalss(
                mutableListOf(
                    "xxxxx/int/test_ci_temp@rezbuild+rezbuild"
                )
            )
        )
        resourceExt.clear()
        replace("$sampleDir/$dir/resource/resource-remote-mul.yml", resourceExt)
        assert(
            resourceExt.keys.equalss(
                mutableListOf(
                    "xxxxx/int/test_ci_temp@rezbuild1+rezbuild1",
                    "xxxxx/int/test_ci_temp@rezbuild+rezbuild"
                )
            )
        )
        resourceExt.clear()
    }

    @Test
    fun testParametersTemplate() {
        val dir = "parameters"
        check("$sampleDir/$dir/parameters.yml")
    }

    private fun check(file: String) {
        var flag = true
        val sample = BufferedReader(
            StringReader(
                replace(file)
            )
        )
        val compared = BufferedReader(
            StringReader(
                getStrFromResource("compared/${file.removePrefix("samples")}")
            )
        )
        var line = sample.readLine()
        var lineCompare = compared.readLine()
        while (line != null) {
            // 随机生成的id不计入比较
            if (line.trim().startsWith("id") || line.trim().startsWith("- id")) {
                line = sample.readLine()
                lineCompare = compared.readLine()
                continue
            }
            if (line != lineCompare) {
                println("$line != $lineCompare")
                flag = false
                break
            }
            line = sample.readLine()
            lineCompare = compared.readLine()
        }
        assert(flag)
    }

    private fun replace(testYaml: String, resourceExt: MutableMap<String, ResourcesPools>? = null): String {
        val sb = getStrFromResource(testYaml)

        val yaml = ScriptYmlUtils.formatYaml(sb)
        val preTemplateYamlObject = YamlUtil.getObjectMapper().readValue(yaml, PreTemplateScriptBuildYaml::class.java)
        preTemplateYamlObject.resources?.pools?.forEach { pool ->
            resourceExt?.put(pool.format(), pool)
        }
        val preScriptBuildYaml = YamlTemplate(
            filePath = testYaml,
            yamlObject = preTemplateYamlObject,
            extraParameters = null,
            getTemplateMethod = ::getTestTemplate,
            nowRepo = null,
            repo = null,
            resourcePoolMapExt = resourceExt
        ).replace()
        val (normalOb, trans) = ScriptYmlUtils.normalizeGitCiYaml(preScriptBuildYaml, "")
        val yamls = YamlUtil.toYaml(normalOb)
        println(YamlCommonUtils.toYamlNotNull(preScriptBuildYaml))
        println("------------------------")
        println(JsonUtil.toJson(trans))
        println("------------------------")
        println(yamls)
        return yamls
    }

    private fun getTestTemplate(
        param: GetTemplateParam<Any?>
    ): String {
        val newPath = if (param.targetRepo == null) {
            "templates/${param.path}"
        } else {
            "templates/${param.targetRepo!!.repository}/templates/${param.path}"
        }
        val sb = getStrFromResource(newPath)
        return ScriptYmlUtils.formatYaml(sb)
    }

    private fun getStrFromResource(testYaml: String): String {
        val classPathResource = ClassPathResource(testYaml)
        val inputStream: InputStream = classPathResource.inputStream
        val isReader = InputStreamReader(inputStream)

        val reader = BufferedReader(isReader)
        val sb = StringBuffer()
        var str: String?
        while (reader.readLine().also { str = it } != null) {
            sb.append(str).append("\n")
        }
        inputStream.close()
        return sb.toString()
    }

    private fun MutableSet<String>.equalss(new: MutableList<String>): Boolean {
        if (this.size != new.size) {
            return false
        }
        this.forEachIndexed { index, its ->
            if (its != new[index]) {
                return false
            }
        }
        return true
    }
}
