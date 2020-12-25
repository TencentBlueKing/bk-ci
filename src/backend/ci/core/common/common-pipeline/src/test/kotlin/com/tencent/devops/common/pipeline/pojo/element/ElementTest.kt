package com.tencent.devops.common.pipeline.pojo.element

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.utils.SkipElementUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElementTest {

    @Test
    fun takeStatus() {
        val element = ManualTriggerElement(id = "1")
        element.status = BuildStatus.QUEUE.name
        var takeStatus = element.takeStatus(params = mapOf(SkipElementUtils.getSkipElementVariableName(element.id!!) to "true"))
        assertEquals(BuildStatus.SKIP.name, takeStatus.name)

        element.status = BuildStatus.SUCCEED.name
        takeStatus = element.takeStatus(params = mapOf(SkipElementUtils.getSkipElementVariableName(element.id!!) to "false"))
        assertEquals(BuildStatus.QUEUE.name, takeStatus.name)

        element.status = BuildStatus.FAILED.name
        takeStatus = element.takeStatus(params = mapOf(SkipElementUtils.getSkipElementVariableName(element.id!!) to "false"))
        assertEquals(BuildStatus.QUEUE.name, takeStatus.name)

        element.status = BuildStatus.QUEUE.name
        takeStatus = element.takeStatus(params = mapOf(SkipElementUtils.getSkipElementVariableName(element.id!!) to "false"))
        assertEquals(BuildStatus.QUEUE.name, takeStatus.name)

        element.status = BuildStatus.SKIP.name
        takeStatus = element.takeStatus(params = mapOf(SkipElementUtils.getSkipElementVariableName(element.id!!) to "false"))
        assertEquals(BuildStatus.SKIP.name, takeStatus.name)

        element.additionalOptions = elementAdditionalOptions(enable = false)
        takeStatus = element.takeStatus(params = mapOf(SkipElementUtils.getSkipElementVariableName(element.id!!) to "false"))
        assertEquals(BuildStatus.SKIP.name, takeStatus.name)
    }

    @Test
    fun isElementEnable() {
        val element = ManualTriggerElement()
        assertTrue(element.isElementEnable())
        element.additionalOptions = null
        assertTrue(element.isElementEnable())
        element.additionalOptions = elementAdditionalOptions(enable = true)
        assertTrue(element.isElementEnable())
        element.additionalOptions = elementAdditionalOptions(enable = false)
        assertFalse(element.isElementEnable())
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
            val element = CodeGitWebHookTriggerElement(id = "3", branchName = "master", eventType = CodeEventType.MERGE_REQUEST, block = false,
                repositoryHashId = null, excludeBranchName = null, excludePaths = null, excludeTagName = null, excludeUsers = null, includePaths = null)
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
}