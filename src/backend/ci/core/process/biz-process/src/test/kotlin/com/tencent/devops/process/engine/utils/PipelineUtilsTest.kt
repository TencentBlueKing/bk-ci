package com.tencent.devops.process.engine.utils

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import org.junit.Test

class PipelineUtilsTest {

    @Test
    fun checkPipelineNameLength() {
        val name = "1234567890123456789012345678901234567890123456789012345678901234"
        PipelineUtils.checkPipelineName(name)
    }

    @Test(expected = ErrorCodeException::class)
    fun checkPipelineNameTooLength() {
        val name = "12345678901234567890123456789012345678901234567890123456789012345" // exceed 64 char
        PipelineUtils.checkPipelineName(name)
    }

    @Test
    fun checkPipelineParams() {
        val params = mutableListOf<BuildFormProperty>()
        params.add(make(id = "abc_1234"))
        params.add(make(id = "_abc_1234"))
        PipelineUtils.checkPipelineParams(params)
    }

    @Test(expected = OperationException::class)
    fun checkPipelineParamsIllegalId() {
        val params = mutableListOf<BuildFormProperty>()
        params.add(make(id = "abc-123"))
        PipelineUtils.checkPipelineParams(params)
    }

    @Test(expected = OperationException::class)
    fun checkPipelineParamsStartWithNum() {
        val params = mutableListOf<BuildFormProperty>()
        params.add(make(id = "123_abc"))
        PipelineUtils.checkPipelineParams(params)
    }

    @Test
    fun checkPipelineDescLength() {
        var desc: String? = null
        PipelineUtils.checkPipelineDescLength(desc)
        desc = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"
        PipelineUtils.checkPipelineDescLength(desc)
    }

    @Test(expected = ErrorCodeException::class)
    fun checkPipelineDescLength101() {
        val desc = "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901"
        PipelineUtils.checkPipelineDescLength(desc)
    }

    private fun make(id: String): BuildFormProperty {
        return BuildFormProperty(
            id = id,
            required = true,
            type = BuildFormPropertyType.STRING,
            defaultValue = "123",
            options = null,
            desc = "hello",
            repoHashId = null,
            relativePath = null,
            scmType = null,
            containerType = null,
            glob = null,
            properties = null)
    }
}