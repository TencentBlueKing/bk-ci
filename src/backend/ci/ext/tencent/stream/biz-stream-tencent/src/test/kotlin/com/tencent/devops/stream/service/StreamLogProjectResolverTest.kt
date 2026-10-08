/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved. BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.stream.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StreamLogProjectResolverTest {

    @Test
    fun `allow when viewing the pipeline owner project`() {
        assertTrue(
            StreamLogProjectResolver.canViewFromGitProject(
                viewingGitProjectId = 100L,
                ownerGitProjectId = 100L,
                eventGitProjectId = 200L,
                sourceGitProjectId = 200L
            )
        )
    }

    @Test
    fun `allow when viewing the event project of a remote trigger`() {
        assertTrue(
            StreamLogProjectResolver.canViewFromGitProject(
                viewingGitProjectId = 200L,
                ownerGitProjectId = 100L,
                eventGitProjectId = 200L,
                sourceGitProjectId = null
            )
        )
    }

    @Test
    fun `allow when viewing the fork source project`() {
        assertTrue(
            StreamLogProjectResolver.canViewFromGitProject(
                viewingGitProjectId = 300L,
                ownerGitProjectId = 100L,
                eventGitProjectId = 100L,
                sourceGitProjectId = 300L
            )
        )
    }

    @Test
    fun `deny unrelated project`() {
        assertFalse(
            StreamLogProjectResolver.canViewFromGitProject(
                viewingGitProjectId = 999L,
                ownerGitProjectId = 100L,
                eventGitProjectId = 200L,
                sourceGitProjectId = 300L
            )
        )
    }
}
