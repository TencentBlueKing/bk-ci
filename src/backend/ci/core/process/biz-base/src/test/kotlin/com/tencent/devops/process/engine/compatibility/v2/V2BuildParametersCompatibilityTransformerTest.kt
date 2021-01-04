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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.compatibility.v2

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.process.util.PswParameterUtils
import com.tencent.devops.process.utils.FIXVERSION
import com.tencent.devops.process.utils.MAJORVERSION
import com.tencent.devops.process.utils.MINORVERSION
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.process.utils.PipelineVarUtil
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class V2BuildParametersCompatibilityTransformerTest {

    private val pswParameterUtils: PswParameterUtils = mock()

    private val buildParametersCompatibilityTransformer =
        V2BuildParametersCompatibilityTransformer(pswParameterUtils)

    private val targetMajorValue = "1"
    private val targetMinorValue = "2"
    private val targetFixValue = "3"

    @Before
    fun setUp() {
        whenever(pswParameterUtils.decrypt("default")).thenReturn("fail")
        whenever(pswParameterUtils.decrypt("fake")).thenReturn("success")
    }

    @Test
    fun transform() {

        // user input first
        // old var will be transform
        var inputParams = listOf(
            BuildParameters(key = "MajorVersion", value = targetMajorValue, valueType = BuildFormPropertyType.STRING),
            BuildParameters(key = "MinorVersion", value = targetMinorValue, valueType = BuildFormPropertyType.STRING),
            BuildParameters(key = "FixVersion", value = targetFixValue, valueType = BuildFormPropertyType.STRING)
        )
        // default second will be replace by old var
        var triggerContainerParam = listOf(
            BuildParameters(key = MAJORVERSION, value = "0", valueType = BuildFormPropertyType.STRING),
            BuildParameters(key = MINORVERSION, value = "0", valueType = BuildFormPropertyType.STRING),
            BuildParameters(key = FIXVERSION, value = "0", valueType = BuildFormPropertyType.STRING)
        )

        val oldReplaceNew = buildParametersCompatibilityTransformer.transform(inputParams, triggerContainerParam)
        check(oldReplaceNew)

        inputParams = listOf(
            BuildParameters(key = MAJORVERSION, value = targetMajorValue, valueType = BuildFormPropertyType.STRING),
            BuildParameters(key = MINORVERSION, value = targetMinorValue, valueType = BuildFormPropertyType.STRING),
            BuildParameters(key = FIXVERSION, value = targetFixValue, valueType = BuildFormPropertyType.STRING)
        )

        triggerContainerParam = listOf(
            BuildParameters(key = "MajorVersion", value = "0", valueType = BuildFormPropertyType.STRING),
            BuildParameters(key = "MinorVersion", value = "0", valueType = BuildFormPropertyType.STRING),
            BuildParameters(key = "FixVersion", value = "0", valueType = BuildFormPropertyType.STRING)
        )

        val newCoverOld = buildParametersCompatibilityTransformer.transform(inputParams, triggerContainerParam)
        check(newCoverOld)

        inputParams = listOf(
            BuildParameters(key = MAJORVERSION, value = targetMajorValue, valueType = BuildFormPropertyType.STRING),
            BuildParameters(key = MINORVERSION, value = targetMinorValue, valueType = BuildFormPropertyType.STRING),
            BuildParameters(key = FIXVERSION, value = targetFixValue, valueType = BuildFormPropertyType.STRING)
        )

        triggerContainerParam = listOf(
            BuildParameters(key = MAJORVERSION, value = "0", valueType = BuildFormPropertyType.STRING),
            BuildParameters(key = MINORVERSION, value = "0", valueType = BuildFormPropertyType.STRING),
            BuildParameters(key = FIXVERSION, value = "0", valueType = BuildFormPropertyType.STRING)
        )

        val userReplace = buildParametersCompatibilityTransformer.transform(inputParams, triggerContainerParam)
        check(userReplace)
    }

    private fun check(transform: List<BuildParameters>) {
        assertEquals(3, transform.size)
        val checkMap = mutableMapOf<String, BuildParameters>()
        transform.forEach {
            checkMap[it.key] = it
        }
        val majorVersionParam = checkMap[MAJORVERSION]
        assertNotNull(majorVersionParam)
        assertEquals(MAJORVERSION, majorVersionParam!!.key)
        assertEquals(targetMajorValue, majorVersionParam.value)
        assertEquals(BuildFormPropertyType.STRING, majorVersionParam.valueType)

        val minorVersionParam = checkMap[MINORVERSION]
        assertNotNull(minorVersionParam)
        assertEquals(MINORVERSION, minorVersionParam!!.key)
        assertEquals(targetMinorValue, minorVersionParam.value)
        assertEquals(BuildFormPropertyType.STRING, minorVersionParam.valueType)

        val fixVersionParam = checkMap[FIXVERSION]
        assertNotNull(fixVersionParam)
        assertEquals(FIXVERSION, fixVersionParam!!.key)
        assertEquals(targetFixValue, fixVersionParam.value)
        assertEquals(BuildFormPropertyType.STRING, fixVersionParam.valueType)
    }

    private val paramProperties = listOf(
        BuildFormProperty(
            id = "repoName",
            required = true,
            type = BuildFormPropertyType.CODE_LIB,
            defaultValue = "hello/bk-ci",
            options = null,
            desc = null,
            repoHashId = null,
            relativePath = null,
            scmType = null,
            containerType = null,
            glob = null,
            properties = null
        ),
        BuildFormProperty(
            id = "hookBranch",
            required = false,
            type = BuildFormPropertyType.STRING,
            defaultValue = "dev",
            options = null,
            desc = null,
            repoHashId = null,
            relativePath = null,
            scmType = null,
            containerType = null,
            glob = null,
            properties = null
        ),
        BuildFormProperty(
            id = "password",
            required = true,
            type = BuildFormPropertyType.PASSWORD,
            defaultValue = "default",
            options = null,
            desc = null,
            repoHashId = null,
            relativePath = null,
            scmType = null,
            containerType = null,
            glob = null,
            properties = null
        )
    )

    @Test
    fun parseStartBuildParameter() {

        val paramValues = mutableMapOf(
            "repoName" to "Tencent/bk-ci",
            PIPELINE_WEBHOOK_BRANCH to "master",
            "password" to "fake",
            "illegalStartParam" to "i will be delete after call it"
        )
        val startBuildParameter = buildParametersCompatibilityTransformer.parseManualStartParam(paramProperties, paramValues)
        assertEquals(3, startBuildParameter.size)
        val map = mutableMapOf<String, BuildParameters>()
        startBuildParameter.forEach {
            map[it.key] = it
            println("${it.key}=$it")
        }
        Assert.assertNull(map["illegalStartParam"]) // 非启动参数被过滤
        assertNotNull(map[PipelineVarUtil.oldVarToNewVar("repoName")]) // 旧参数被转换为新参数
        Assert.assertNull(map["repoName"]) // 旧参数被转换为新参数，如上
        assertNotNull(map[PIPELINE_WEBHOOK_BRANCH]) // 新参数仍然存在，并且值变为传入的值替换默认值
        assertEquals("master", map[PIPELINE_WEBHOOK_BRANCH]!!.value) // 并且值变为传入的值替换默认值
        Assert.assertNull(map["hookBranch"]) // 旧参数被转换为新参数，如上
        assertEquals("success", map["password"]!!.value) // 合法的启动参数保留下来了，并且被解密
    }
}
