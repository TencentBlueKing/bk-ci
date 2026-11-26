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

package com.tencent.devops.process.trigger.tapd

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.TapdEventAction
import com.tencent.devops.common.pipeline.enums.TapdEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerData
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerInput
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_NOTE_COMMENT
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ACTION
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAPD_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAPD_PARENT_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAPD_PRIORITY_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAPD_TITLE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAPD_WORKSPACE_ID
import com.tencent.devops.process.trigger.event.TapdWebhookTriggerEvent
import com.tencent.devops.process.trigger.tapd.TapdWebhookUtils.buildStartParams
import com.tencent.devops.process.trigger.tapd.TapdWebhookUtils.getPayloadField
import com.tencent.devops.process.utils.BK_CI_MATERIAL_NAME
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TapdWebhookUtilsTest {

    private val element = TapdWebHookTriggerElement(
        id = "tapd-trigger-1",
        data = TapdWebHookTriggerData(input = TapdWebHookTriggerInput(eventType = TapdEventType.BUG))
    )

    @Test
    fun bugStatusChangeShouldFillTitleAndRelatedParams() {
        val body = loadStatusChangeBody()
        val params = startParams(body, TapdEventType.BUG, TapdEventAction.STATUS_CHANGE)
        val title = body.getValue("new_title")

        assertEquals(title, params[CI_TAPD_TITLE])
        assertEquals(title, params[BK_CI_MATERIAL_NAME])
        assertEquals("[status_change bug] $title", params[PIPELINE_BUILD_MSG])
        assertEquals(TapdEventAction.STATUS_CHANGE.value, params[CI_ACTION])
        assertEquals(body.getValue("object_id"), params[CI_TAPD_ID])
        assertEquals(body.getValue("workspace_id"), params[CI_TAPD_WORKSPACE_ID])
        // 非 update 动作仍优先取无前缀字段，保持 API 补写的中文优先级语义
        assertEquals(body.getValue("priority_label"), params[CI_TAPD_PRIORITY_ID])
        assertEquals(body.getValue("new_description"), params[PIPELINE_WEBHOOK_NOTE_COMMENT])
        // 空字符串不应被当作有效值
        assertEquals("", params[CI_TAPD_PARENT_ID])
    }

    @Test
    fun storyStatusChangeShouldUseNewName() {
        val params = startParams(
            body = mapOf("new_name" to "【样例】需求状态变更", "title" to "不应被使用"),
            eventType = TapdEventType.STORY,
            eventAction = TapdEventAction.STATUS_CHANGE
        )
        assertEquals("【样例】需求状态变更", params[CI_TAPD_TITLE])
    }

    @Test
    fun storyStatusChangeShouldFallbackToEnrichedName() {
        val params = startParams(
            body = mapOf("name" to "API 补写的需求标题"),
            eventType = TapdEventType.STORY,
            eventAction = TapdEventAction.STATUS_CHANGE
        )
        assertEquals("API 补写的需求标题", params[CI_TAPD_TITLE])
    }

    @Test
    fun bugCommentShouldFallbackToEnrichedName() {
        val params = startParams(
            body = mapOf("name" to "API 补写的缺陷标题"),
            eventType = TapdEventType.BUG,
            eventAction = TapdEventAction.ADD_COMMENT
        )
        assertEquals("API 补写的缺陷标题", params[CI_TAPD_TITLE])
    }

    @Test
    fun bugCreateShouldUseTitle() {
        val params = startParams(
            body = mapOf("title" to "新建缺陷标题", "description" to "新建缺陷描述"),
            eventType = TapdEventType.BUG,
            eventAction = TapdEventAction.CREATE
        )
        assertEquals("新建缺陷标题", params[CI_TAPD_TITLE])
        assertEquals("新建缺陷描述", params[PIPELINE_WEBHOOK_NOTE_COMMENT])
    }

    @Test
    fun bugUpdateShouldPreferNewTitle() {
        val params = startParams(
            body = mapOf("title" to "旧标题", "new_title" to "更新后的标题"),
            eventType = TapdEventType.BUG,
            eventAction = TapdEventAction.UPDATE
        )
        assertEquals("更新后的标题", params[CI_TAPD_TITLE])
    }

    @Test
    fun deleteBuildMsgShouldUseObjectId() {
        val params = startParams(
            body = mapOf("object_id" to "1020000000000000009", "title" to "删除的缺陷"),
            eventType = TapdEventType.BUG,
            eventAction = TapdEventAction.DELETE
        )
        assertEquals("[delete bug] 1020000000000000009", params[PIPELINE_BUILD_MSG])
    }

    @Test
    fun replayBodyShouldStillResolveTitle() {
        // 回放使用持久化的 eventBody，eventAction 会被重新解析为 STATUS_CHANGE
        val body = loadStatusChangeBody() + mapOf("id" to "")
        val params = startParams(body, TapdEventType.BUG, TapdEventAction.STATUS_CHANGE)

        assertEquals(body.getValue("new_title"), params[CI_TAPD_TITLE])
        assertEquals(body.getValue("object_id"), params[CI_TAPD_ID])
    }

    @Test
    fun payloadFieldShouldFallbackAcrossPrefixes() {
        val body = mapOf(
            "id" to " ",
            "new_id" to "1020000000000000002",
            "title" to "   ",
            "new_title" to "回退标题",
            "new_parent_id" to ""
        )

        assertEquals("1020000000000000002", body.getPayloadField("id"))
        assertEquals("1020000000000000002", body.getPayloadField("id", update = true))
        assertEquals("回退标题", body.getPayloadField("title"))
        assertEquals("回退标题", body.getPayloadField("title", update = true))
        assertEquals("", body.getPayloadField("parent_id", update = true))
    }

    private fun startParams(
        body: Map<String, String>,
        eventType: TapdEventType,
        eventAction: TapdEventAction
    ) = buildStartParams(
        event = TapdWebhookTriggerEvent(
            projectId = "demo",
            pipelineId = "p-1",
            eventId = 1L,
            workspaceId = "20001234",
            eventType = eventType,
            eventAction = eventAction,
            triggerUser = "tester"
        ),
        body = body,
        element = element
    )

    private fun loadStatusChangeBody(): Map<String, String> {
        val stream = checkNotNull(
            javaClass.classLoader.getResourceAsStream("trigger/tapd_bug_status_change.json")
        ) { "missing tapd status change fixture" }
        val json = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return JsonUtil.getObjectMapper().readValue(json)
    }
}
