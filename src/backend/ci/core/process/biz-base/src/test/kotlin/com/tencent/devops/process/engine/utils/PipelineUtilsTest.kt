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

package com.tencent.devops.process.engine.utils

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.SpringContextUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [SpringContextUtil::class, CommonConfig::class])
class PipelineUtilsTest {

    @Test
    fun checkPipelineNameLength() {
        val name = "1234567890123456789012345678901234567890123456789012345678901234"
        PipelineUtils.checkPipelineName(name, 64)
    }

    @Test
    fun checkPipelineNameTooLength() {
        val name = "12345678901234567890123456789012345678901234567890123456789012345" // exceed 64 char
        Assertions.assertThrows(ErrorCodeException::class.java) { PipelineUtils.checkPipelineName(name, 64) }
    }

    @Test
    fun checkPipelineParams() {
        val params = mutableListOf<BuildFormProperty>()
        params.add(make(id = "abc_1234"))
        params.add(make(id = "_abc_1234"))
        PipelineUtils.checkPipelineParams(params)
    }

    @Test
    fun checkPipelineParamsIllegalId() {
        val params = mutableListOf<BuildFormProperty>()
        params.add(make(id = "abc-123"))
        Assertions.assertThrows(OperationException::class.java) { PipelineUtils.checkPipelineParams(params) }
    }

    @Test
    fun checkPipelineParamsStartWithNum() {
        val params = mutableListOf<BuildFormProperty>()
        params.add(make(id = "123_abc"))
        Assertions.assertThrows(OperationException::class.java) { PipelineUtils.checkPipelineParams(params) }
    }

    @Test
    fun checkPipelineParamsChineseNameWhenSupport() {
        val params = mutableListOf<BuildFormProperty>()
        params.add(make(id = "中文变量"))
        params.add(make(id = "abc-123"))
        val result = PipelineUtils.checkPipelineParams(params, supportChineseVarName = true)
        Assertions.assertEquals(2, result.size)
        Assertions.assertNotNull(result["中文变量"])
        Assertions.assertNotNull(result["abc-123"])
    }

    @Test
    fun checkPipelineParamsChineseNameWhenSupportIsNull() {
        val params = mutableListOf<BuildFormProperty>()
        params.add(make(id = "中文变量"))
        val result = PipelineUtils.checkPipelineParams(params, supportChineseVarName = null)
        Assertions.assertNotNull(result["中文变量"])
    }

    @Test
    fun checkPipelineParamsChineseNameWhenNotSupport() {
        val params = mutableListOf<BuildFormProperty>()
        params.add(make(id = "中文变量"))
        Assertions.assertThrows(OperationException::class.java) {
            PipelineUtils.checkPipelineParams(params, supportChineseVarName = false)
        }
    }

    @Test
    fun checkPipelineParamsConstantNormalizeFlags() {
        val params = mutableListOf<BuildFormProperty>()
        params.add(make(id = "myConst", constant = true, required = true, defaultValue = "value"))
        val result = PipelineUtils.checkPipelineParams(params)
        val ret = result["myConst"]!!
        Assertions.assertFalse(ret.required)
        Assertions.assertEquals(true, ret.readOnly)
        Assertions.assertEquals(null, ret.asInstanceInput)
    }

    @Test
    fun checkPipelineParamsConstantBlankValue() {
        val params = mutableListOf<BuildFormProperty>()
        params.add(make(id = "myConst", constant = true, defaultValue = ""))
        Assertions.assertThrows(OperationException::class.java) { PipelineUtils.checkPipelineParams(params) }
    }

    @Test
    fun checkPipelineParamsConstantWhitespaceValue() {
        val params = mutableListOf<BuildFormProperty>()
        params.add(make(id = "myConst", constant = true, defaultValue = "   "))
        Assertions.assertThrows(OperationException::class.java) { PipelineUtils.checkPipelineParams(params) }
    }

    @Test
    fun checkPipelineParamsNonRequiredAsInstanceInput() {
        val params = mutableListOf<BuildFormProperty>()
        params.add(make(id = "myVar", required = false))
        val result = PipelineUtils.checkPipelineParams(params)
        Assertions.assertEquals(null, result["myVar"]!!.asInstanceInput)
    }

    @Test
    fun checkPipelineParamsReturnMapKeyedById() {
        val params = mutableListOf(
            make(id = "param1"),
            make(id = "param2")
        )
        val result = PipelineUtils.checkPipelineParams(params)
        Assertions.assertEquals(2, result.size)
        Assertions.assertEquals("param1", result["param1"]!!.id)
        Assertions.assertEquals("param2", result["param2"]!!.id)
    }

    @Test
    fun checkPipelineDescLength() {
        var desc: String? = null
        PipelineUtils.checkPipelineDescLength(desc, 100)
        desc = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"
        PipelineUtils.checkPipelineDescLength(desc, 100)
    }

    @Test
    fun checkPipelineDescLength101() {
        val desc =
            "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901"
        Assertions.assertThrows(ErrorCodeException::class.java) { PipelineUtils.checkPipelineDescLength(desc, 100) }
    }

    private fun make(
        id: String,
        required: Boolean = true,
        constant: Boolean? = false,
        defaultValue: Any = "123"
    ): BuildFormProperty {
        return BuildFormProperty(
            id = id,
            required = required,
            constant = constant,
            type = BuildFormPropertyType.STRING,
            defaultValue = defaultValue,
            options = null,
            desc = "hello",
            repoHashId = null,
            relativePath = null,
            scmType = null,
            containerType = null,
            glob = null,
            properties = null
        )
    }
}
