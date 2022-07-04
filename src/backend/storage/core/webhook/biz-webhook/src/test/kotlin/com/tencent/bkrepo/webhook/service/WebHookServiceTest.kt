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

package com.tencent.bkrepo.webhook.service

import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.webhook.UT_PROJECT_ID
import com.tencent.bkrepo.webhook.UT_USER
import com.tencent.bkrepo.webhook.constant.AssociationType
import com.tencent.bkrepo.webhook.dao.WebHookDao
import com.tencent.bkrepo.webhook.pojo.CreateWebHookRequest
import com.tencent.bkrepo.webhook.pojo.UpdateWebHookRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.query.Query

@DataMongoTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebHookServiceTest @Autowired constructor(
    private val webHookService: WebHookService,
    private val webHookDao: WebHookDao
) : ServiceBaseTest() {

    @BeforeAll
    fun init() {
        initMock()
    }

    @BeforeEach
    fun beforeEach() {
        webHookDao.remove(Query())
    }

    @Test
    @DisplayName("创建WebHook测试")
    fun create() {
        val request = CreateWebHookRequest(
            url = "https://localhost",
            headers = mapOf("key" to "value"),
            triggers = listOf(EventType.NODE_CREATED),
            associationType = AssociationType.PROJECT,
            associationId = UT_PROJECT_ID
        )
        val webhook = webHookService.createWebHook(UT_USER, request)
        Assertions.assertEquals(webhook.associationId, UT_PROJECT_ID)
    }

    @Test
    @DisplayName("查询WebHook列表")
    fun list() {
        val list = webHookService.listWebHook(UT_USER, AssociationType.PROJECT, UT_PROJECT_ID)
        Assertions.assertEquals(list.size, 0)
    }

    @Test
    @DisplayName("删除WebHook")
    fun delete() {
        val request = CreateWebHookRequest(
            url = "https://localhost",
            headers = mapOf("key" to "value"),
            triggers = listOf(EventType.NODE_CREATED),
            associationType = AssociationType.PROJECT,
            associationId = UT_PROJECT_ID
        )
        val webhook = webHookService.createWebHook(UT_USER, request)
        webHookService.deleteWebHook(UT_USER, webhook.id)
    }

    @Test
    @DisplayName("更新WebHook")
    fun update() {
        val createWebHookRequest = CreateWebHookRequest(
            url = "https://localhost",
            headers = mapOf("key" to "value"),
            triggers = listOf(EventType.NODE_CREATED),
            associationType = AssociationType.PROJECT,
            associationId = UT_PROJECT_ID
        )
        var webhook = webHookService.createWebHook(UT_USER, createWebHookRequest)
        val updateWebHookRequest = UpdateWebHookRequest(webhook.id, "https://127.0.0.1")
        webhook = webHookService.updateWebHook(UT_USER, updateWebHookRequest)
        Assertions.assertEquals(webhook.url, "https://127.0.0.1")
    }
}
