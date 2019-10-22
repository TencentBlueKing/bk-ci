package com.tencent.devops.plugin.codecc.element

import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement.ProjectLanguage
import org.junit.Assert
import org.junit.Test

class LinuxCodeCCScriptElementBizPluginTest {

    private val plugin = LinuxCodeCCScriptElementBizPlugin()

    private val pipelineId = "p-123"
    private val projectId = "test"
    private val pipelineName = "codecc Pipeline"
    private val userId = "admin"
    private val element = LinuxCodeCCScriptElement(
            name = "exe",
            id = "1",
            status = "1",
            script = "echo hello",
            scanType = "1",
            scriptType = BuildScriptType.SHELL,
            codeCCTaskCnName = "demo",
            asynchronous = true,
            path = "/tmp/codecc",
            languages = listOf(ProjectLanguage.JAVA)
    )

    @Test
    fun afterCreate() {
        plugin.afterCreate(element, projectId, pipelineId, pipelineName, userId, ChannelCode.BS)
    }

    @Test
    fun beforeDelete() {
        plugin.beforeDelete(element, userId, pipelineId)
    }

    @Test
    fun elementClass() {
        Assert.assertEquals(
            LinuxCodeCCScriptElement::class.java,
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

}