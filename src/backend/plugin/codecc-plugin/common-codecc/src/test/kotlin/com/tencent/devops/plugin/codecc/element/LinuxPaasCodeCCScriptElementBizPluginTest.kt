package com.tencent.devops.plugin.codecc.element

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement.ProjectLanguage
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.plugin.codecc.CodeccApi
import com.tencent.devops.plugin.codecc.pojo.coverity.CoverityResult
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
        plugin.afterCreate(element, projectId, pipelineId, pipelineName, userId, ChannelCode.BS)
    }

    @Test
    fun afterCreateWhenTaskExists() {
        whenever(
                coverityApi.isTaskExist(
                        taskId = any(),
                        userId = any()

                )
        ).thenReturn(true)

        plugin.afterCreate(element, projectId, pipelineId, pipelineName, userId, ChannelCode.BS)
    }

    @Test(expected = OperationException::class)
    fun afterCreateWhenCoverityReturnNull() {
        val coverityResult = CoverityResult(
                status = 1010,
                message = "error",
                data = null
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

        plugin.afterCreate(element, projectId, pipelineId, pipelineName, userId, ChannelCode.BS)
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

        plugin.afterCreate(element, projectId, pipelineId, pipelineName, userId, ChannelCode.BS)
        val map = coverityResult.data as Map<String, Any>
        assertEquals(map["taskId"], element.codeCCTaskId)
    }

    @Test
    fun beforeDeleteFail() {
        plugin.beforeDelete(element, userId, pipelineId)
    }

    @Test
    fun beforeDeleteWhenIDNull() {
        element.codeCCTaskId = null
        plugin.beforeDelete(element, userId, pipelineId)
        element.codeCCTaskId = ""
        plugin.beforeDelete(element, userId, pipelineId)
    }

    @Test
    fun beforeDeleteWhenSuccess() {
        plugin.beforeDelete(element, userId, pipelineId)
        assertEquals(element.codeCCTaskId, null)
    }
}