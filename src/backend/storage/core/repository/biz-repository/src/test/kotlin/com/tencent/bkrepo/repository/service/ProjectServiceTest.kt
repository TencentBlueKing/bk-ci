/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.repository.service

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.repository.UT_PROJECT_DESC
import com.tencent.bkrepo.repository.UT_PROJECT_DISPLAY
import com.tencent.bkrepo.repository.UT_PROJECT_ID
import com.tencent.bkrepo.repository.UT_USER
import com.tencent.bkrepo.repository.dao.ProjectDao
import com.tencent.bkrepo.repository.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.repository.service.repo.ProjectService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.query.Query

@DisplayName("项目服务测试")
@DataMongoTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProjectServiceTest @Autowired constructor(
    private val projectService: ProjectService,
    private val projectDao: ProjectDao
) : ServiceBaseTest() {

    @BeforeAll
    fun beforeAll() {
        initMock()
    }

    @BeforeEach
    fun beforeEach() {
        removeAllProject()
    }

    @Test
    @DisplayName("测试创建项目")
    fun `test create project`() {
        val request = ProjectCreateRequest(UT_PROJECT_ID, UT_PROJECT_DISPLAY, UT_PROJECT_DESC, UT_USER)
        projectService.createProject(request)
    }

    @Test
    @DisplayName("测试创建同名项目")
    fun `should throw exception when project exists`() {
        val request = ProjectCreateRequest(UT_PROJECT_ID, UT_PROJECT_DISPLAY, UT_PROJECT_DESC, UT_USER)
        projectService.createProject(request)
        assertThrows<ErrorCodeException> { projectService.createProject(request) }
    }

    @Test
    @DisplayName("测试非法项目名称")
    fun `should throw exception with illegal name`() {
        var request = ProjectCreateRequest("1", UT_PROJECT_DISPLAY, UT_PROJECT_DESC, UT_USER)
        assertThrows<ErrorCodeException> { projectService.createProject(request) }

        request = ProjectCreateRequest("11", UT_PROJECT_DISPLAY, UT_PROJECT_DESC, UT_USER)
        assertThrows<ErrorCodeException> { projectService.createProject(request) }

        request = ProjectCreateRequest("a".repeat(33), UT_PROJECT_DISPLAY, UT_PROJECT_DESC, UT_USER)
        assertThrows<ErrorCodeException> { projectService.createProject(request) }

        request = ProjectCreateRequest("test_1", UT_PROJECT_DISPLAY, UT_PROJECT_DESC, UT_USER)
        projectService.createProject(request)

        request = ProjectCreateRequest("test-1", UT_PROJECT_DISPLAY, UT_PROJECT_DESC, UT_USER)
        projectService.createProject(request)

        request = ProjectCreateRequest("a1", UT_PROJECT_DISPLAY, UT_PROJECT_DESC, UT_USER)
        projectService.createProject(request)

        request = ProjectCreateRequest("_prebuild", UT_PROJECT_DISPLAY, UT_PROJECT_DESC, UT_USER)
        projectService.createProject(request)

        request = ProjectCreateRequest("CODECC_a1", UT_PROJECT_DISPLAY, UT_PROJECT_DESC, UT_USER)
        projectService.createProject(request)
    }

    @Test
    @DisplayName("测试非法项目显示名")
    fun `should throw exception with illegal display name`() {
        var request = ProjectCreateRequest(UT_PROJECT_ID, "", UT_PROJECT_DESC, UT_USER)
        assertThrows<ErrorCodeException> { projectService.createProject(request) }

        request = ProjectCreateRequest(UT_PROJECT_ID, "1".repeat(33), UT_PROJECT_DESC, UT_USER)
        assertThrows<ErrorCodeException> { projectService.createProject(request) }

        request = ProjectCreateRequest(UT_PROJECT_ID, "1".repeat(1), UT_PROJECT_DESC, UT_USER)
        assertThrows<ErrorCodeException> { projectService.createProject(request) }

        request = ProjectCreateRequest(UT_PROJECT_ID, "1".repeat(32), UT_PROJECT_DESC, UT_USER)
        projectService.createProject(request)

        removeAllProject()
        request = ProjectCreateRequest(UT_PROJECT_ID, "123-abc", UT_PROJECT_DESC, UT_USER)
        projectService.createProject(request)
    }

    private fun removeAllProject() {
        projectDao.remove(Query())
    }
}
