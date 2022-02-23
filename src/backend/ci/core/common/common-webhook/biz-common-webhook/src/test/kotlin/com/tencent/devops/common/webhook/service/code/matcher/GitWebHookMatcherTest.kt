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

import com.nhaarman.mockito_kotlin.mock
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.git.GitIssueEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitNoteEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.common.webhook.service.code.GitScmService
import com.tencent.devops.common.webhook.service.code.handler.tgit.TGitIssueTriggerHandler
import com.tencent.devops.common.webhook.service.code.handler.tgit.TGitMrTriggerHandler
import com.tencent.devops.common.webhook.service.code.handler.tgit.TGitNoteTriggerHandler
import com.tencent.devops.common.webhook.service.code.handler.tgit.TGitPushTriggerHandler
import com.tencent.devops.common.webhook.service.code.handler.tgit.TGitReviewTriggerHandler
import com.tencent.devops.common.webhook.service.code.handler.tgit.TGitTagPushTriggerHandler
import com.tencent.devops.common.webhook.service.code.loader.CodeWebhookHandlerRegistrar
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.springframework.core.io.ClassPathResource
import java.nio.charset.Charset

class GitWebHookMatcherTest {

    private val repository = CodeGitRepository(
        aliasName = "mingshewhe/webhook_test3",
        url = "https://git.code.tencent.com/mingshewhe/webhook_test3.git",
        credentialId = "",
        projectName = "mingshewhe/webhook_test3",
        userName = "mingshewhe",
        authType = RepoAuthType.HTTP,
        projectId = "mht",
        repoHashId = "eraf"
    )

    private val repositoryDyy = CodeGitRepository(
        aliasName = "yongyiduan/webhook-test",
        url = "https://git.code.tencent.com/yongyiduan/webhook-test.git",
        credentialId = "",
        projectName = "yongyiduan/webhook-test",
        userName = "yongyiduan",
        authType = RepoAuthType.HTTP,
        projectId = "mht",
        repoHashId = "eraf"
    )

    @Before
    fun setUp() {
        val gitScmService: GitScmService = mock()
        CodeWebhookHandlerRegistrar.register(TGitPushTriggerHandler(gitScmService))
        CodeWebhookHandlerRegistrar.register(TGitTagPushTriggerHandler())
        CodeWebhookHandlerRegistrar.register(TGitMrTriggerHandler(gitScmService))
        CodeWebhookHandlerRegistrar.register(TGitReviewTriggerHandler(gitScmService))
        CodeWebhookHandlerRegistrar.register(TGitIssueTriggerHandler(gitScmService))
        CodeWebhookHandlerRegistrar.register(TGitNoteTriggerHandler(gitScmService))
    }

    @Test
    fun pushEventTrigger() {
        val classPathResource = ClassPathResource(
            "com/tencent/devops/common/webhook/service/code/tgit/TGitPushEvent.json"
        )
        val event = JsonUtil.to(
            json = classPathResource.inputStream.readBytes().toString(Charset.defaultCharset()),
            type = GitPushEvent::class.java
        )
        val webHookParams = WebHookParams(
            repositoryConfig = RepositoryConfig(
                repositoryHashId = "eraf",
                repositoryType = RepositoryType.ID,
                repositoryName = null
            ),
            eventType = CodeEventType.PUSH,
            branchName = "master"
        )
        val matcher = GitWebHookMatcher(event = event)

        Assert.assertTrue(matcher.preMatch().isMatch)
        Assert.assertFalse(
            matcher.isMatch(
                projectId = "mht",
                pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
                repository = repository,
                webHookParams = webHookParams
            ).isMatch
        )
        Assert.assertEquals("mingshewhe", matcher.getUsername())
        Assert.assertEquals("9c9f8cc062060fdad67137e5e102689be765b4d4", matcher.getRevision())
        Assert.assertEquals("mingshewhe/webhook_test3", matcher.getRepoName())
        Assert.assertEquals("mr_test", matcher.getBranchName())
        Assert.assertEquals(CodeEventType.PUSH, matcher.getEventType())
        Assert.assertEquals(CodeType.GIT, matcher.getCodeType())
        Assert.assertEquals(null, matcher.getHookSourceUrl())
        Assert.assertEquals(null, matcher.getHookTargetUrl())
        Assert.assertEquals(null, matcher.getMergeRequestId())
        Assert.assertEquals("mr 19", matcher.getMessage())
    }

    @Test
    fun tagEventTrigger() {
        val classPathResource = ClassPathResource(
            "com/tencent/devops/common/webhook/service/code/tgit/TGitTagEvent.json"
        )
        val event = JsonUtil.to(
            json = classPathResource.inputStream.readBytes().toString(Charset.defaultCharset()),
            type = GitTagPushEvent::class.java
        )
        val webHookParams = WebHookParams(
            repositoryConfig = RepositoryConfig(
                repositoryHashId = "eraf",
                repositoryType = RepositoryType.ID,
                repositoryName = null
            ),
            eventType = CodeEventType.TAG_PUSH,
            branchName = "v1.0.1"
        )
        val matcher = GitWebHookMatcher(event = event)

        Assert.assertTrue(matcher.preMatch().isMatch)
        Assert.assertTrue(
            matcher.isMatch(
                projectId = "mht",
                pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
                repository = repository,
                webHookParams = webHookParams
            ).isMatch
        )
        Assert.assertEquals("mingshewhe", matcher.getUsername())
        Assert.assertEquals("87acd380f4a91ba1eb200a082ad60f394f3062a5", matcher.getRevision())
        Assert.assertEquals("mingshewhe/webhook_test3", matcher.getRepoName())
        Assert.assertEquals("v1.0.1", matcher.getBranchName())
        Assert.assertEquals(CodeEventType.TAG_PUSH, matcher.getEventType())
        Assert.assertEquals(CodeType.GIT, matcher.getCodeType())
        Assert.assertEquals(null, matcher.getHookSourceUrl())
        Assert.assertEquals(null, matcher.getHookTargetUrl())
        Assert.assertEquals(null, matcher.getMergeRequestId())
        Assert.assertEquals(
            "Merge branch 'mr_test' into 'master' (merge request !6)\n\nmr 6",
            matcher.getMessage()
        )
    }

    @Test
    fun mrEventTrigger() {
        val classPathResource = ClassPathResource(
            "com/tencent/devops/common/webhook/service/code/tgit/TGitMrEvent.json"
        )
        val event = JsonUtil.to(
            json = classPathResource.inputStream.readBytes().toString(Charset.defaultCharset()),
            type = GitMergeRequestEvent::class.java
        )
        val webHookParams = WebHookParams(
            repositoryConfig = RepositoryConfig(
                repositoryHashId = "eraf",
                repositoryType = RepositoryType.ID,
                repositoryName = null
            ),
            eventType = CodeEventType.MERGE_REQUEST,
            branchName = "master"
        )
        val matcher = GitWebHookMatcher(event = event)

        Assert.assertTrue(matcher.preMatch().isMatch)
        Assert.assertTrue(
            matcher.isMatch(
                projectId = "mht",
                pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
                repository = repository,
                webHookParams = webHookParams
            ).isMatch
        )
        Assert.assertEquals("mingshewhe", matcher.getUsername())
        Assert.assertEquals("9c9f8cc062060fdad67137e5e102689be765b4d4", matcher.getRevision())
        Assert.assertEquals("mingshewhe/webhook_test3", matcher.getRepoName())
        Assert.assertEquals("master", matcher.getBranchName())
        Assert.assertEquals(CodeEventType.MERGE_REQUEST, matcher.getEventType())
        Assert.assertEquals(CodeType.GIT, matcher.getCodeType())
        Assert.assertEquals(
            "https://git.code.tencent.com/mingshewhe/webhook_test3.git",
            matcher.getHookSourceUrl()
        )
        Assert.assertEquals(
            "https://git.code.tencent.com/mingshewhe/webhook_test3.git",
            matcher.getHookTargetUrl()
        )
        Assert.assertEquals(290966L, matcher.getMergeRequestId())
        Assert.assertEquals("mr 19", matcher.getMessage())
    }

    @Test
    fun codeReviewEventTrigger() {
        val classPathResource = ClassPathResource(
            "com/tencent/devops/common/webhook/service/code/tgit/TGitCodeReviewEvent.json"
        )
        val event = JsonUtil.to(
            json = classPathResource.inputStream.readBytes().toString(Charset.defaultCharset()),
            type = GitReviewEvent::class.java
        )
        val webHookParams = WebHookParams(
            repositoryConfig = RepositoryConfig(
                repositoryHashId = "eraf",
                repositoryType = RepositoryType.ID,
                repositoryName = null
            ),
            eventType = CodeEventType.REVIEW,
            includeCrState = "approved"
        )
        val matcher = GitWebHookMatcher(event = event)

        Assert.assertTrue(matcher.preMatch().isMatch)
        Assert.assertTrue(
            matcher.isMatch(
                projectId = "mht",
                pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
                repository = repository,
                webHookParams = webHookParams
            ).isMatch
        )
        Assert.assertEquals("mingshewhe", matcher.getUsername())
        Assert.assertEquals("", matcher.getRevision())
        Assert.assertEquals("mingshewhe/webhook_test3", matcher.getRepoName())
        Assert.assertEquals("", matcher.getBranchName())
        Assert.assertEquals(CodeEventType.REVIEW, matcher.getEventType())
        Assert.assertEquals(CodeType.GIT, matcher.getCodeType())
        Assert.assertEquals(null, matcher.getHookSourceUrl())
        Assert.assertEquals(null, matcher.getHookTargetUrl())
        Assert.assertEquals(null, matcher.getMergeRequestId())
        Assert.assertEquals("", matcher.getMessage())
    }

    @Test
    fun gitIssueEventTrigger() {
        val classPathResource = ClassPathResource(
            "com/tencent/devops/common/webhook/service/code/tgit/TGitIssueEvent.json"
        )
        val event = JsonUtil.to(
            json = classPathResource.inputStream.readBytes().toString(Charset.defaultCharset()),
            type = GitIssueEvent::class.java
        )
        val webHookParams = WebHookParams(
            repositoryConfig = RepositoryConfig(
                repositoryHashId = "eraf",
                repositoryType = RepositoryType.ID,
                repositoryName = null
            ),
            eventType = CodeEventType.ISSUES,
            includeIssueAction = "open"
        )
        val matcher = GitWebHookMatcher(event = event)

        Assert.assertTrue(
            matcher.isMatch(
                projectId = "mht",
                pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
                repository = repository,
                webHookParams = webHookParams
            ).isMatch
        )
        Assert.assertEquals("mingshewhe", matcher.getUsername())
        Assert.assertEquals("", matcher.getRevision())
        Assert.assertEquals("mingshewhe/webhook_test3", matcher.getRepoName())
        Assert.assertEquals("", matcher.getBranchName())
        Assert.assertEquals(CodeEventType.ISSUES, matcher.getEventType())
        Assert.assertEquals(CodeType.GIT, matcher.getCodeType())
        Assert.assertEquals(null, matcher.getHookSourceUrl())
        Assert.assertEquals(null, matcher.getHookTargetUrl())
        Assert.assertEquals(null, matcher.getMergeRequestId())
        Assert.assertEquals("issue创建", matcher.getMessage())
    }

    @Test
    fun gitNoteIssueEventTrigger() {
        val classPathResource = ClassPathResource(
            "com/tencent/devops/common/webhook/service/code/tgit/TGitNoteIssueEvent.json"
        )
        val event = JsonUtil.to(
            json = classPathResource.inputStream.readBytes().toString(Charset.defaultCharset()),
            type = GitNoteEvent::class.java
        )
        val webHookParams = WebHookParams(
            repositoryConfig = RepositoryConfig(
                repositoryHashId = "eraf",
                repositoryType = RepositoryType.ID,
                repositoryName = null
            ),
            eventType = CodeEventType.NOTE,
            includeNoteTypes = "Issue",
            includeNoteComment = "^@Stream"
        )
        val matcher = GitWebHookMatcher(event = event)
        Assert.assertTrue(
            matcher.isMatch(
                projectId = "mht",
                pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
                repository = repositoryDyy,
                webHookParams = webHookParams
            ).isMatch
        )
        Assert.assertEquals("yongyiduan", matcher.getUsername())
        Assert.assertEquals("", matcher.getRevision())
        Assert.assertEquals("yongyiduan/webhook-test", matcher.getRepoName())
        Assert.assertEquals("", matcher.getBranchName())
        Assert.assertEquals(CodeEventType.NOTE, matcher.getEventType())
        Assert.assertEquals(CodeType.GIT, matcher.getCodeType())
        Assert.assertEquals(null, matcher.getHookSourceUrl())
        Assert.assertEquals(null, matcher.getHookTargetUrl())
        Assert.assertEquals(null, matcher.getMergeRequestId())
        Assert.assertEquals("@Stream issue test", matcher.getMessage())
    }

    @Test
    fun gitNoteCommitEventTrigger() {
        val classPathResource = ClassPathResource(
            "com/tencent/devops/common/webhook/service/code/tgit/TGitNoteCommitEvent.json"
        )
        val event = JsonUtil.to(
            json = classPathResource.inputStream.readBytes().toString(Charset.defaultCharset()),
            type = GitNoteEvent::class.java
        )
        val webHookParams = WebHookParams(
            repositoryConfig = RepositoryConfig(
                repositoryHashId = "eraf",
                repositoryType = RepositoryType.ID,
                repositoryName = null
            ),
            eventType = CodeEventType.NOTE,
            includeNoteTypes = "Commit"
        )
        val matcher = GitWebHookMatcher(event = event)
        Assert.assertTrue(
            matcher.isMatch(
                projectId = "mht",
                pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
                repository = repositoryDyy,
                webHookParams = webHookParams
            ).isMatch
        )
        Assert.assertEquals("yongyiduan", matcher.getUsername())
        Assert.assertEquals("", matcher.getRevision())
        Assert.assertEquals("yongyiduan/webhook-test", matcher.getRepoName())
        Assert.assertEquals("", matcher.getBranchName())
        Assert.assertEquals(CodeEventType.NOTE, matcher.getEventType())
        Assert.assertEquals(CodeType.GIT, matcher.getCodeType())
        Assert.assertEquals(null, matcher.getHookSourceUrl())
        Assert.assertEquals(null, matcher.getHookTargetUrl())
        Assert.assertEquals(null, matcher.getMergeRequestId())
        Assert.assertEquals("commit test", matcher.getMessage())
    }

    @Test
    fun gitNoteMrEventTrigger() {
        val classPathResource = ClassPathResource(
            "com/tencent/devops/common/webhook/service/code/tgit/TGitNoteMrEvent.json"
        )
        val event = JsonUtil.to(
            json = classPathResource.inputStream.readBytes().toString(Charset.defaultCharset()),
            type = GitNoteEvent::class.java
        )
        val webHookParams = WebHookParams(
            repositoryConfig = RepositoryConfig(
                repositoryHashId = "eraf",
                repositoryType = RepositoryType.ID,
                repositoryName = null
            ),
            eventType = CodeEventType.NOTE,
            includeNoteTypes = "Review"
        )
        val matcher = GitWebHookMatcher(event = event)
        Assert.assertTrue(
            matcher.isMatch(
                projectId = "mht",
                pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
                repository = repositoryDyy,
                webHookParams = webHookParams
            ).isMatch
        )
        Assert.assertEquals("yongyiduan", matcher.getUsername())
        Assert.assertEquals("", matcher.getRevision())
        Assert.assertEquals("yongyiduan/webhook-test", matcher.getRepoName())
        Assert.assertEquals("", matcher.getBranchName())
        Assert.assertEquals(CodeEventType.NOTE, matcher.getEventType())
        Assert.assertEquals(CodeType.GIT, matcher.getCodeType())
        Assert.assertEquals(null, matcher.getHookSourceUrl())
        Assert.assertEquals(null, matcher.getHookTargetUrl())
        Assert.assertEquals(null, matcher.getMergeRequestId())
        Assert.assertEquals("mr test", matcher.getMessage())
    }
}
