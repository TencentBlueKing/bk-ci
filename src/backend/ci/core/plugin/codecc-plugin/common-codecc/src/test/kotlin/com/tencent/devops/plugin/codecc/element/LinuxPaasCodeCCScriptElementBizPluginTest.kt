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

package com.tencent.devops.plugin.codecc.element

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.plugin.codecc.CodeccApi
import com.tencent.devops.plugin.codecc.pojo.coverity.CoverityResult
import com.tencent.devops.plugin.codecc.pojo.coverity.ProjectLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class LinuxPaasCodeCCScriptElementBizPluginTest {

    private val coverityApi: CodeccApi = mock()

    private val plugin = LinuxPaasCodeCCScriptElementBizPlugin(coverityApi)

    private val pipelineId = "p-123"
    private val projectId = "test"
    private val pipelineName = "codecc Pipeline"
    private val userId = "admin"
    private val element = LinuxPaasCodeCCScriptElement(
        name = "exe",
        id = "1",
        status = "1",
        script = "echo hello",
        scanType = "1",
        scriptType = BuildScriptType.SHELL,
        codeCCTaskCnName = "demo",
        codeCCTaskId = "123",
        asynchronous = true,
        path = "/tmp/codecc",
        languages = listOf(ProjectLanguage.JAVA)
    )

    @Test
    fun elementClass() {
        assertEquals(
            LinuxPaasCodeCCScriptElement::class.java,
            plugin.elementClass()
        )
    }

    @Test
    fun checkOne() {
        plugin.check(element, 1)
        plugin.check(element, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun checkMoreOne() {
        plugin.check(element, 2)
    }

    @Test(expected = OperationException::class)
    fun afterCreateWhenLanguagesIsEmptyThrowException() {
        element.languages = emptyList()
        plugin.afterCreate(
            element = element,
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineName = pipelineName,
            userId = userId,
            channelCode = ChannelCode.BS,
            create = true
        )
    }

    @Test
    fun afterCreateWhenTaskExists() {
        whenever(
            coverityApi.isTaskExist(
                taskId = any(),
                userId = any()

            )
        ).thenReturn(true)

        plugin.afterCreate(
            element = element,
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineName = pipelineName,
            userId = userId,
            channelCode = ChannelCode.BS,
            create = false
        )
    }

    @Test(expected = OperationException::class)
    fun afterCreateWhenCoverityReturnNull() {
        whenever(
            coverityApi.isTaskExist(
                taskId = any(),
                userId = any()

            )
        ).thenReturn(false)

        whenever(
            coverityApi.createTask(
                projectId = any(),
                pipelineId = any(),
                pipelineName = any(),
                rtx = any(),
                element = any()
            )
        ).thenReturn(null)

        plugin.afterCreate(
            element = element,
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineName = pipelineName,
            userId = userId,
            channelCode = ChannelCode.BS,
            create = false
        )
    }

    @Test(expected = OperationException::class)
    fun afterCreateWhenTaskNotExists() {
        val coverityResult = CoverityResult(
            status = 0,
            message = "",
            data = mapOf(
                "taskId" to 123
            )
        )
        whenever(
            coverityApi.isTaskExist(
                taskId = any(),
                userId = any()

            )
        ).thenReturn(false)

        whenever(
            coverityApi.createTask(
                projectId = any(),
                pipelineId = any(),
                pipelineName = any(),
                rtx = any(),
                element = any()
            )
        ).thenReturn(coverityResult)

        plugin.afterCreate(
            element = element,
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineName = pipelineName,
            userId = userId,
            channelCode = ChannelCode.BS,
            create = true
        )
        val map = coverityResult.data as Map<String, Any>
        assertEquals(map["taskId"], element.codeCCTaskId)
    }

    @Test
    fun beforeDeleteFail() {
        val param = BeforeDeleteParam(userId, projectId, pipelineId)
        plugin.beforeDelete(element = element, param = param)
    }

    @Test
    fun beforeDeleteWhenIDNull() {
        val param = BeforeDeleteParam(userId, projectId, pipelineId)
        element.codeCCTaskId = null
        plugin.beforeDelete(element = element, param = param)
        element.codeCCTaskId = ""
        plugin.beforeDelete(element = element, param = param)
    }

    @Test
    fun beforeDeleteWhenSuccess() {
        val param = BeforeDeleteParam(userId, projectId, pipelineId)
        plugin.beforeDelete(element = element, param = param)
        assertEquals(element.codeCCTaskId, null)
    }
}