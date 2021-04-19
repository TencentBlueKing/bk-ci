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

import com.tencent.bkrepo.auth.api.ServiceRoleResource
import com.tencent.bkrepo.auth.api.ServiceUserResource
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.local.LocalConfiguration
import com.tencent.bkrepo.common.security.http.core.HttpAuthProperties
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.repository.UT_PROJECT_ID
import com.tencent.bkrepo.repository.UT_REPO_DESC
import com.tencent.bkrepo.repository.UT_REPO_DISPLAY
import com.tencent.bkrepo.repository.UT_REPO_NAME
import com.tencent.bkrepo.repository.UT_USER
import com.tencent.bkrepo.repository.config.RepositoryProperties
import com.tencent.bkrepo.repository.dao.ProjectDao
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.service.repo.ProjectService
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@Import(
    StorageProperties::class,
    RepositoryProperties::class,
    ProjectDao::class,
    RepositoryDao::class,
    HttpAuthProperties::class,
    SpringContextUtils::class
)
@ComponentScan("com.tencent.bkrepo.repository.service")
@TestPropertySource(locations = ["classpath:bootstrap-ut.properties"])
open class ServiceBaseTest {

    @MockBean
    lateinit var storageService: StorageService

    @MockBean
    lateinit var roleResource: ServiceRoleResource

    @MockBean
    lateinit var userResource: ServiceUserResource

    @MockBean
    lateinit var permissionManager: PermissionManager

    @Autowired
    lateinit var springContextUtils: SpringContextUtils

    fun initMock() {
        Mockito.`when`(roleResource.createRepoManage(anyString(), anyString())).then {
            ResponseBuilder.success(UT_USER)
        }

        Mockito.`when`(roleResource.createProjectManage(anyString())).thenReturn(
            ResponseBuilder.success(UT_USER)
        )

        Mockito.`when`(userResource.addUserRole(anyString(), anyString())).thenReturn(
            ResponseBuilder.success()
        )
    }

    fun initRepoForUnitTest(
        projectService: ProjectService,
        repositoryService: RepositoryService
    ) {
        if (!projectService.checkExist(UT_PROJECT_ID)) {
            val projectCreateRequest = ProjectCreateRequest(UT_PROJECT_ID, UT_REPO_NAME, UT_REPO_DISPLAY, UT_USER)
            projectService.createProject(projectCreateRequest)
        }
        if (!repositoryService.checkExist(UT_PROJECT_ID, UT_REPO_NAME)) {
            val repoCreateRequest = RepoCreateRequest(
                projectId = UT_PROJECT_ID,
                name = UT_REPO_NAME,
                type = RepositoryType.GENERIC,
                category = RepositoryCategory.LOCAL,
                public = false,
                description = UT_REPO_DESC,
                configuration = LocalConfiguration(),
                operator = UT_USER
            )
            repositoryService.createRepo(repoCreateRequest)
        }
    }
}
