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

package com.tencent.devops.project.service

import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.archive.config.BkRepoConfig
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.UserDao
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.pojo.enums.ProjectScopeType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProjectLocalServiceTest {

    private val dslContext = mockk<DSLContext>(relaxed = true)
    private val projectDao = mockk<ProjectDao>(relaxed = true)
    private val authProjectApi = mockk<AuthProjectApi>(relaxed = true)
    private val pipelineAuthServiceCode = mockk<PipelineAuthServiceCode>(relaxed = true)
    private val jmxApi = mockk<ProjectJmxApi>(relaxed = true)
    private val projectService = mockk<ProjectService>(relaxed = true)
    private val projectTagService = mockk<ProjectTagService>(relaxed = true)
    private val client = mockk<Client>(relaxed = true)
    private val projectPermissionService = mockk<ProjectPermissionService>(relaxed = true)
    private val tokenService = mockk<ClientTokenService>(relaxed = true)
    private val projectExtPermissionService = mockk<ProjectExtPermissionService>(relaxed = true)
    private val bkTag = mockk<BkTag>(relaxed = true)
    private val commonConfig = mockk<CommonConfig>(relaxed = true)
    private val bkRepoConfig = mockk<BkRepoConfig>(relaxed = true)
    private val userDao = mockk<UserDao>(relaxed = true)

    private val service = ProjectLocalService(
        dslContext = dslContext,
        projectDao = projectDao,
        authProjectApi = authProjectApi,
        pipelineAuthServiceCode = pipelineAuthServiceCode,
        jmxApi = jmxApi,
        projectService = projectService,
        projectTagService = projectTagService,
        client = client,
        projectPermissionService = projectPermissionService,
        tokenService = tokenService,
        projectExtPermissionService = projectExtPermissionService,
        bkTag = bkTag,
        commonConfig = commonConfig,
        bkRepoConfig = bkRepoConfig,
        userDao = userDao
    )

    @Test
    fun `get or create personal project should return existing personal project directly`() {
        val personalRecord = mockk<TProjectRecord>(relaxed = true)
        every { personalRecord.id } returns 1L
        every { personalRecord.projectId } returns "p-1"
        every { personalRecord.projectName } returns "personal-project"
        every { personalRecord.englishName } returns "_tester"
        every { personalRecord.createdAt } returns LocalDateTime.of(2026, 5, 8, 12, 0)
        every { personalRecord.projectType } returns 0
        every { personalRecord.approvalStatus } returns 2
        every { personalRecord.projectScope } returns ProjectScopeType.PERSONAL.value
        every { personalRecord.enabled } returns true

        every { projectDao.getFirstPersonalProjectByCreator(dslContext, "tester") } returns personalRecord

        val result = service.getOrCreatePersonalProject("tester")

        assertEquals("_tester", result.projectCode)
        assertEquals(ProjectScopeType.PERSONAL.value, result.projectScope)
        verify(exactly = 1) { projectDao.getFirstPersonalProjectByCreator(dslContext, "tester") }
        verify(exactly = 0) { projectDao.getByEnglishName(dslContext, any()) }
        verify(exactly = 0) { authProjectApi.getProjectUsers(any(), any(), any()) }
        verify(exactly = 0) { projectDao.updateProjectScopeByCode(any(), any(), any()) }
        verify(exactly = 0) { projectService.create(any(), any(), any(), any(), any()) }
    }
}
