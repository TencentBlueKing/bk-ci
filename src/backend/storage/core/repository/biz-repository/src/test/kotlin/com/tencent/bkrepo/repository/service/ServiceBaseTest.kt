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
import com.tencent.bkrepo.common.security.http.core.HttpAuthProperties
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.repository.UT_USER
import com.tencent.bkrepo.repository.config.RepositoryProperties
import com.tencent.bkrepo.repository.dao.ProjectDao
import com.tencent.bkrepo.repository.dao.RepositoryDao
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@Import(
    HttpAuthProperties::class,
    StorageProperties::class,
    RepositoryProperties::class,
    ProjectDao::class,
    RepositoryDao::class
)
@ComponentScan("com.tencent.bkrepo.repository.service")
@TestPropertySource(locations = ["classpath:bootstrap-ut.properties"])
class ServiceBaseTest {

    @MockBean
    lateinit var storageService: StorageService

    @MockBean
    lateinit var roleResource: ServiceRoleResource

    @MockBean
    lateinit var userResource: ServiceUserResource

    @MockBean
    lateinit var permissionManager: PermissionManager

    fun initMock() {
        Mockito.`when`(roleResource.createRepoManage(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).then {
            ResponseBuilder.success(UT_USER)
        }

        Mockito.`when`(roleResource.createProjectManage(ArgumentMatchers.anyString())).thenReturn(
            ResponseBuilder.success(UT_USER)
        )

        Mockito.`when`(userResource.addUserRole(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(
            ResponseBuilder.success()
        )
    }
}
