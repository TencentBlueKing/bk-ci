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
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.svn.SvnCommitEvent
import com.tencent.devops.common.webhook.service.code.handler.svn.SvnCommitTriggerHandler
import com.tencent.devops.common.webhook.service.code.loader.CodeWebhookHandlerRegistrar
import com.tencent.devops.repository.pojo.CodeSvnRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.nio.charset.Charset

class SvnWebHookMatcherTest {
    private val repository = CodeSvnRepository(
        aliasName = "ddlin/ddlin_proj",
        url = "http://svn.example.com/ddlin/ddlin_proj",
        credentialId = "",
        projectName = "ddlin/ddlin_proj",
        userName = "mingshewhe",
        projectId = "mht",
        repoHashId = "dfd"
    )

    @BeforeEach
    fun setUp() {
        CodeWebhookHandlerRegistrar.register(SvnCommitTriggerHandler())
    }

    @Test
    fun svnCommitEventTrigger() {
        val classPathResource = ClassPathResource(
            "com/tencent/devops/common/webhook/service/code/svn/SvnCommitEvent.json"
        )
        val event = JsonUtil.to(
            json = classPathResource.inputStream.readBytes().toString(Charset.defaultCharset()),
            type = SvnCommitEvent::class.java
        )

        val matcher = SvnWebHookMatcher(event)

        Assertions.assertTrue(matcher.preMatch().isMatch)
        var webHookParams = WebHookParams(
            repositoryConfig = RepositoryConfig(
                repositoryHashId = "eraf",
                repositoryType = RepositoryType.ID,
                repositoryName = null
            ),
            eventType = CodeEventType.POST_COMMIT,
            relativePath = "trunk/,release/",
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
                repositoryHashId = "eraf",
                repositoryType = RepositoryType.ID,
                repositoryName = null
            ),
            eventType = CodeEventType.POST_COMMIT,
            relativePath = "trunk/aa/aaa.txt",
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

        Assertions.assertEquals("mingshewhe", matcher.getUsername())
        Assertions.assertEquals("116", matcher.getRevision())
        Assertions.assertEquals("ddlin/ddlin_proj", matcher.getRepoName())
        Assertions.assertEquals("", matcher.getBranchName())
        Assertions.assertEquals(CodeEventType.POST_COMMIT, matcher.getEventType())
        Assertions.assertEquals(CodeType.SVN, matcher.getCodeType())
        Assertions.assertEquals(null, matcher.getHookSourceUrl())
        Assertions.assertEquals(null, matcher.getHookTargetUrl())
        Assertions.assertEquals(null, matcher.getMergeRequestId())
        Assertions.assertEquals("文件匹配", matcher.getMessage())
    }
}
