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

package com.tencent.devops.common.pipeline.pojo.element

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.utils.ElementUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ElementTest {

    @Test
    fun testElementJsonOrder() {
        val jsonFile = ElementTest::class.java.classLoader.getResource("windowsElement.json")
        val expected = jsonFile!!.readText().trim()
        val wel = WindowsScriptElement(
            id = "e-326ce1c320204980a3d2a0f241bccd63",
            name = "batch script",
            script = "unity build",
            scriptType = BuildScriptType.BAT
        )
        wel.additionalOptions = elementAdditionalOptions()
        val actual = JsonUtil.toSortJson(wel)
        assertEquals(expected, actual)
    }

    @Test
    fun unknownSubType() {
        val json = """{
            "@type" : "not exist sub type",
            "name" : "这个是不存在的Element，为了验证反序列化Json时不出错",
            "id" : "e-e89bb10191344f6b84e42c358050dbea",
            "atomCode" : "builder3Dunity"
          } """
        val element = JsonUtil.to(json, Element::class.java)
        println(JsonUtil.toJson(element))
        assertTrue(element is EmptyElement)
    }

    @Test
    fun takeStatus() {
        val element = ManualTriggerElement(id = "1")
        element.status = BuildStatus.QUEUE.name
        val skipElementVariableName = ElementUtils.getSkipElementVariableName(element.id!!)
        var rerun = true
        element.disableBySkipVar(mapOf(skipElementVariableName to "true"))
        var takeStatus = element.initStatus(rerun = rerun)
        assertEquals(BuildStatus.SKIP.name, takeStatus.name)

        element.status = BuildStatus.SUCCEED.name
        rerun = true
        element.additionalOptions?.enable = true
        element.disableBySkipVar(mapOf(skipElementVariableName to "false"))
        takeStatus = element.initStatus(rerun = rerun)
        assertEquals(BuildStatus.QUEUE.name, takeStatus.name)

        element.status = BuildStatus.FAILED.name
        rerun = false
        element.additionalOptions?.enable = true
        element.disableBySkipVar(mapOf(skipElementVariableName to "false"))
        takeStatus = element.initStatus(rerun = rerun)
        assertEquals(BuildStatus.QUEUE.name, takeStatus.name)

        element.status = BuildStatus.QUEUE.name
        rerun = false
        element.additionalOptions?.enable = true
        element.disableBySkipVar(mapOf(skipElementVariableName to "false"))
        takeStatus = element.initStatus(rerun = rerun)
        assertEquals(BuildStatus.QUEUE.name, takeStatus.name)

        element.status = BuildStatus.SKIP.name
        rerun = false
        element.additionalOptions?.enable = true
        element.disableBySkipVar(mapOf(skipElementVariableName to "false"))
        takeStatus = element.initStatus(rerun = rerun)
        assertEquals(BuildStatus.SKIP.name, takeStatus.name)

        element.status = BuildStatus.SKIP.name
        rerun = true
        element.additionalOptions?.enable = true
        element.disableBySkipVar(mapOf(skipElementVariableName to "false"))
        takeStatus = element.initStatus(rerun = rerun)
        assertEquals(BuildStatus.QUEUE.name, takeStatus.name)

        element.additionalOptions = elementAdditionalOptions(enable = false)
        takeStatus = element.initStatus(rerun = rerun)
        assertEquals(BuildStatus.SKIP.name, takeStatus.name)
    }

    @Test
    fun elementEnabled() {
        val element = ManualTriggerElement()
        assertTrue(element.elementEnabled())
        element.additionalOptions = null
        assertTrue(element.elementEnabled())
        element.additionalOptions = elementAdditionalOptions(enable = true)
        assertTrue(element.elementEnabled())
        element.additionalOptions = elementAdditionalOptions(enable = false)
        assertFalse(element.elementEnabled())
    }

    @Test
    fun findFirstTaskIdByStartType() {
        run ManualTriggerElement@{
            val element = ManualTriggerElement(id = "1")
            assertEquals(element.id, element.findFirstTaskIdByStartType(StartType.MANUAL))
            assertEquals(element.id, element.findFirstTaskIdByStartType(StartType.SERVICE))
            assertEquals(element.id, element.findFirstTaskIdByStartType(StartType.PIPELINE))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.WEB_HOOK))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.TIME_TRIGGER))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.REMOTE))
        }

        run TimerTriggerElement@{
            val element = TimerTriggerElement(id = "2", advanceExpression = listOf("1/* * * * *"))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.MANUAL))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.SERVICE))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.PIPELINE))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.WEB_HOOK))
            assertEquals(element.id, element.findFirstTaskIdByStartType(StartType.TIME_TRIGGER))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.REMOTE))
        }

        run WebHookTriggerElement@{
            val element = CodeGitWebHookTriggerElement(
                id = "3",
                branchName = "master", eventType = CodeEventType.MERGE_REQUEST, block = false,
                repositoryHashId = null, excludeBranchName = null, excludePaths = null,
                excludeTagName = null, excludeUsers = null, includePaths = null
            )
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.MANUAL))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.SERVICE))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.PIPELINE))
            assertEquals(element.id, element.findFirstTaskIdByStartType(StartType.WEB_HOOK))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.TIME_TRIGGER))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.REMOTE))
        }

        run RemoteTriggerElement@{
            val element = RemoteTriggerElement(id = "3", remoteToken = "123456")
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.MANUAL))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.SERVICE))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.PIPELINE))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.WEB_HOOK))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.TIME_TRIGGER))
            assertEquals(element.id, element.findFirstTaskIdByStartType(StartType.REMOTE))
        }

        run MarketBL@{
            val element = MarketBuildLessAtomElement(id = "5", atomCode = "agentLess1")
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.MANUAL))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.SERVICE))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.PIPELINE))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.WEB_HOOK))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.TIME_TRIGGER))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.REMOTE))
        }

        run MarketB@{
            val element = MarketBuildAtomElement(id = "6", atomCode = "agentTask2")
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.MANUAL))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.SERVICE))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.PIPELINE))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.WEB_HOOK))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.TIME_TRIGGER))
            assertNotEquals(element.id, element.findFirstTaskIdByStartType(StartType.REMOTE))
        }
    }

    private fun elementAdditionalOptions(
        enable: Boolean = true,
        runCondition: RunCondition = RunCondition.PRE_TASK_SUCCESS,
        continueWhenFailed: Boolean = false
    ): ElementAdditionalOptions {

        return ElementAdditionalOptions(
            enable = enable,
            continueWhenFailed = continueWhenFailed,
            retryWhenFailed = false,
            runCondition = runCondition,
            customVariables = null,
            retryCount = 0,
            timeout = 100,
            otherTask = null,
            customCondition = null,
            pauseBeforeExec = false,
            subscriptionPauseUser = ""
        )
    }

    @Test
    fun genTaskParams() {
        val element = ManualTriggerElement(id = "1")
        element.cleanUp()
        assertEquals("1", element.genTaskParams()["id"])
    }
}
