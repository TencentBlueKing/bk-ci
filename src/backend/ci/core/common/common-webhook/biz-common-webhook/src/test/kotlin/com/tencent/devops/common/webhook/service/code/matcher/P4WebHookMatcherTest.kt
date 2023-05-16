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

package com.tencent.devops.common.webhook.service.code.matcher

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.p4.P4Event
import com.tencent.devops.common.webhook.service.code.EventCacheService
import com.tencent.devops.common.webhook.service.code.handler.p4.P4ChangeTriggerHandler
import com.tencent.devops.common.webhook.service.code.loader.CodeWebhookHandlerRegistrar
import com.tencent.devops.repository.pojo.CodeP4Repository
import com.tencent.devops.scm.code.p4.api.P4ServerInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.nio.charset.Charset

class P4WebHookMatcherTest {
    private val repository = CodeP4Repository(
        aliasName = "127.0.0.1:1660",
        url = "127.0.0.1:1660",
        credentialId = "",
        projectName = "127.0.0.1:1660",
        userName = "mingshewhe",
        projectId = "mht",
        repoHashId = "dfd"
    )
    private val eventCacheService: EventCacheService = mockk()

    @BeforeEach
    fun setUp() {
        CodeWebhookHandlerRegistrar.register(P4ChangeTriggerHandler(eventCacheService))
    }

    @Test
    @SuppressWarnings("LongMethod")
    fun p4CommitChangeEventTrigger() {
        every {
            eventCacheService.getP4ChangelistFiles(
                repo = repository,
                projectId = "mht",
                repositoryId = "dfd",
                repositoryType = RepositoryType.ID,
                change = 1
            )
        } returns (listOf("//demo/sRc/tt.txt"))
        every {
            eventCacheService.getP4ServerInfo(
                repo = repository,
                projectId = "mht",
                repositoryId = "dfd",
                repositoryType = RepositoryType.ID
            )
        } returns (P4ServerInfo(
            caseSensitive = false
        ))
        val classPathResource = ClassPathResource(
            "com/tencent/devops/common/webhook/service/code/p4/P4CommitChange.json"
        )
        val event = JsonUtil.to(
            json = classPathResource.inputStream.readBytes().toString(Charset.defaultCharset()),
            type = P4Event::class.java
        )

        val matcher = P4WebHookMatcher(event)

        Assertions.assertTrue(matcher.preMatch().isMatch)
        var webHookParams = WebHookParams(
            repositoryConfig = RepositoryConfig(
                repositoryHashId = "dfd",
                repositoryType = RepositoryType.ID,
                repositoryName = null
            ),
            eventType = CodeEventType.CHANGE_COMMIT,
            includePaths = "//demo/src/**",
            excludePaths = ""
        )
        Assertions.assertTrue(
            matcher.isMatch(
                projectId = "mht",
                pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
                repository = repository,
                webHookParams = webHookParams
            ).isMatch
        )
        webHookParams = WebHookParams(
            repositoryConfig = RepositoryConfig(
                repositoryHashId = "dfd",
                repositoryType = RepositoryType.ID,
                repositoryName = null
            ),
            eventType = CodeEventType.CHANGE_COMMIT,
            includePaths = "//depot/**",
            excludePaths = ""
        )
        Assertions.assertFalse(
            matcher.isMatch(
                projectId = "mht",
                pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
                repository = repository,
                webHookParams = webHookParams
            ).isMatch
        )

        webHookParams = WebHookParams(
            repositoryConfig = RepositoryConfig(
                repositoryHashId = "dfd",
                repositoryType = RepositoryType.ID,
                repositoryName = null
            ),
            eventType = CodeEventType.CHANGE_COMMIT,
            includePaths = "//depot/**",
            excludePaths = "",
            version = "2.0.0"
        )
        Assertions.assertFalse(
            matcher.isMatch(
                projectId = "mht",
                pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
                repository = repository,
                webHookParams = webHookParams
            ).isMatch
        )
    }
}
