/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.common.webhook.service.code.handler.tgit

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REF
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_NOTE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_NOTE_PROJECT_ID
import com.tencent.devops.common.webhook.pojo.code.git.GitNoteEvent
import com.tencent.devops.common.webhook.service.code.EventCacheService
import com.tencent.devops.repository.pojo.Repository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class TGitNoteTriggerHandlerTest {

    private val eventCacheService = mockk<EventCacheService>()
    private val handler = TGitNoteTriggerHandler(eventCacheService)
    private val noteFixtures = listOf("TGitNoteCommitEvent.json", "TGitNoteIssueEvent.json", "TGitNoteMrEvent.json")

    @Test
    fun shouldUseCodeProjectIdInsteadOfBkCiProjectId() {
        for (fixture in noteFixtures) {
            val event = noteEvent(fixture)
            val params = handler.retrieveParams(event, projectId = "game_ci", repository = null)

            assertEquals("198178", params[BK_REPO_GIT_WEBHOOK_NOTE_PROJECT_ID], fixture)
            assertEquals(event.objectAttributes.id, params[BK_REPO_GIT_WEBHOOK_NOTE_ID])
        }
    }

    @Test
    fun shouldExposeCodeProjectIdWithoutBkCiProjectContext() {
        for (fixture in noteFixtures) {
            val params = handler.retrieveParams(noteEvent(fixture), projectId = null, repository = null)

            assertEquals("198178", params[BK_REPO_GIT_WEBHOOK_NOTE_PROJECT_ID], fixture)
        }
    }

    @Test
    fun shouldKeepBkCiProjectIdForRepositoryLookup() {
        val repository = mockk<Repository>()
        every {
            eventCacheService.getDefaultBranchLatestCommitInfo(projectId = "game_ci", repo = repository)
        } returns ("main" to null)

        val params = handler.retrieveParams(noteEvent("TGitNoteCommitEvent.json"), "game_ci", repository)

        assertEquals("198178", params[BK_REPO_GIT_WEBHOOK_NOTE_PROJECT_ID])
        assertEquals("main", params[PIPELINE_GIT_REF])
        verify(exactly = 1) {
            eventCacheService.getDefaultBranchLatestCommitInfo(projectId = "game_ci", repo = repository)
        }
    }

    private fun noteEvent(fixture: String): GitNoteEvent {
        val payload = ClassPathResource(
            "com/tencent/devops/common/webhook/service/code/tgit/$fixture"
        ).inputStream.bufferedReader().use { it.readText() }
        return JsonUtil.to(payload, GitNoteEvent::class.java)
    }
}
