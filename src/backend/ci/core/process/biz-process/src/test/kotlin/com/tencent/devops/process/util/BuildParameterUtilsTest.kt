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

package com.tencent.devops.process.util

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.process.utils.PipelineVarUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BuildParameterUtilsTest {

    private val pswParameterUtils: PswParameterUtils = mock()

    private val buildParameterUtils = BuildParameterUtils(pswParameterUtils)

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

    @Before
    fun setup() {
        whenever(pswParameterUtils.decrypt("default")).thenReturn("fail")
        whenever(pswParameterUtils.decrypt("fake")).thenReturn("success")
    }

    @Test
    fun parseStartBuildParameter() {
        val paramValues = mutableMapOf(
            "repoName" to "Tencent/bk-ci",
            PIPELINE_WEBHOOK_BRANCH to "master",
            "password" to "fake",
            "illegalStartParam" to "i will be delete after call it"
        )
        val startBuildParameter = buildParameterUtils.parseStartBuildParameter(paramProperties, paramValues)
        assertEquals(3, startBuildParameter.size)
        val map = mutableMapOf<String, BuildParameters>()
        startBuildParameter.forEach {
            map[it.key] = it
            println("${it.key}=$it")
        }
        assertNull(map["illegalStartParam"]) // 非启动参数被过滤
        assertNotNull(map[PipelineVarUtil.oldVarToNewVar("repoName")]) // 旧参数被转换为新参数
        assertNull(map["repoName"]) // 旧参数被转换为新参数，如上
        assertNotNull(map[PIPELINE_WEBHOOK_BRANCH]) // 新参数仍然存在，并且值变为传入的值替换默认值
        assertEquals("master", map[PIPELINE_WEBHOOK_BRANCH]!!.value) // 并且值变为传入的值替换默认值
        assertNull(map["hookBranch"]) // 旧参数被转换为新参数，如上
        assertEquals("success", map["password"]!!.value) // 合法的启动参数保留下来了，并且被解密
    }

    @Test(expected = ErrorCodeException::class)
    fun mockRequiredParamNotFill() {

        val paramValues = mutableMapOf(
            "illegalStartParam" to "i will be delete after call it"
        )
        val startBuildParameter = buildParameterUtils.parseStartBuildParameter(paramProperties, paramValues)
    }
}