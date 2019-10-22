package com.tencent.devops.plugin.codecc.element

import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement.ProjectLanguage
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import org.junit.Assert.assertEquals
import org.junit.Test

class LinuxPaasCodeCCScriptElementTest {

    @Test
    fun cleanUp() {
        var element = LinuxPaasCodeCCScriptElement(
                name = "exe",
                id = "1",
                status = "1",
                script = "echo hello",
                scanType = "1",
                scriptType = BuildScriptType.SHELL,
                codeCCTaskCnName = "demo",
                codeCCTaskId = "123",
                asynchronous = false,
                path = "/tmp/codecc",
                languages = listOf(ProjectLanguage.JAVA)
        )
        element.cleanUp()
        assertEquals(element.codeCCTaskId, null)
        assertEquals(element.codeCCTaskName, null)

        element = LinuxPaasCodeCCScriptElement(
                scriptType = BuildScriptType.BAT,
                name = "exe", scanType = "1", codeCCTaskCnName = "demo", codeCCTaskId = "123",
                languages = listOf(ProjectLanguage.JAVA)
        )
        element.scriptType = BuildScriptType.PYTHON2
        element.scriptType = BuildScriptType.PYTHON3
        element.scriptType = BuildScriptType.POWER_SHELL
        element.cleanUp()
        assertEquals(element.codeCCTaskId, null)
        assertEquals(element.codeCCTaskName, null)
    }

    @Test
    fun getClassType() {
        val element = LinuxPaasCodeCCScriptElement(
                scriptType = BuildScriptType.POWER_SHELL,
                name = "exe", scanType = "1", codeCCTaskCnName = "demo", codeCCTaskId = "123",
                languages = listOf(ProjectLanguage.JAVA)
        )
        assertEquals(element.getClassType(),
            LinuxPaasCodeCCScriptElement.classType
        )
    }
}