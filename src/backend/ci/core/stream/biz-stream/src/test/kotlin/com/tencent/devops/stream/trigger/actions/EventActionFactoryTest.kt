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

package com.tencent.devops.stream.trigger.actions

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.stream.trigger.actions.streamActions.StreamDeleteAction
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class EventActionFactoryTest {

    private val factory = EventActionFactory(
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        RedisOperation(mockk()),
        mockk()
    )

    private val objectMapper = JsonUtil.getObjectMapper()

    @Test
    fun loadDeleteAction() {
        val deleteEvent = """
{
    "object_kind": "push",
    "operation_kind": "delete",
    "action_kind": "client push",
    "before": "1602a5c0a15e612cf992d1631fbb6b2b317a568b",
    "after": "0000000000000000000000000000000000000000",
    "ref": "refs/heads/delete-1",
    "checkout_sha": "0000000000000000000000000000000000000000",
    "user_name": "",
    "user_id": "",
    "user_email": "",
    "project_id": 10603763,
    "repository": {
        "name": "test",
        "description": "",
        "homepage": "https://xxxx/xxxxx/xxx",
        "git_http_url": "",
        "git_ssh_url": "",
        "url": "",
        "visibility_level": 10
    },
    "commits": [],
    "push_options": {},
    "push_timestamp": "2022-04-20T06:22:25+0000",
    "total_commits_count": 0,
    "create_and_update": null
}
        """.trimIndent()
        val eventObject = try {
            objectMapper.readValue<GitEvent>(deleteEvent)
        } catch (ignore: Exception) {
            assert(false)
            return
        }
        val action = factory.load(eventObject)
        assert(action is StreamDeleteAction)
    }
}
