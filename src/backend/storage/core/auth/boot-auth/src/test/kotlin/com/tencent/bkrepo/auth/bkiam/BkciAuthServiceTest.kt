/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.auth.bkiam

import com.tencent.bkrepo.auth.config.BkAuthConfig
import com.tencent.bkrepo.auth.pojo.enums.BkAuthPermission
import com.tencent.bkrepo.auth.pojo.enums.BkAuthResourceType
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.service.bkauth.BkciAuthService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@DisplayName("bkciAuth相关")
class BkciAuthServiceTest {

    val bkAuthConfig = BkAuthConfig()

    @BeforeEach
    fun setUp() {
        bkAuthConfig.setBkciAuthServer("http://localhost")
        bkAuthConfig.setBkciAuthToken("")
    }

    @Test
    @DisplayName("用户项目权限测试")
    fun checkUserProjectMemberTest() {
        val bkciAuthService = BkciAuthService(bkAuthConfig)
        val result1 = bkciAuthService.isProjectMember("aaa", "bkrepo")
        Assertions.assertEquals(result1, true)
        val result2 = bkciAuthService.isProjectMember("aa", "bkrepo2")
        Assertions.assertEquals(result2, false)
    }

    @Test
    @DisplayName("超级管理员权限测试")
    fun checkUserProjectSuperAdminTest() {
        val bkciAuthService = BkciAuthService(bkAuthConfig)
        val result1 = bkciAuthService.isProjectSuperAdmin(
            "aa",
            "bkrepo",
            BkAuthPermission.DOWNLOAD,
            BkAuthResourceType.PIPELINE_DEFAULT,
            PermissionAction.READ.toString()
        )
        Assertions.assertEquals(result1, false)
    }

    @Test
    @DisplayName("用户资源权限测试")
    fun validateUserResourcePermissionTest() {
        val bkciAuthService = BkciAuthService(bkAuthConfig)
        val result1 = bkciAuthService.validateUserResourcePermission(
            user = "ab",
            projectCode = "bkrepo",
            action = BkAuthPermission.DOWNLOAD,
            resourceCode = "p-abc",
            resourceType = BkAuthResourceType.PIPELINE_DEFAULT
        )
        Assertions.assertEquals(result1, true)
        val result2 = bkciAuthService.validateUserResourcePermission(
            user = "ab",
            projectCode = "bkrepo",
            action = BkAuthPermission.DOWNLOAD,
            resourceCode = "p-abc",
            resourceType = BkAuthResourceType.PIPELINE_DEFAULT
        )
        Assertions.assertEquals(result2, false)
    }

    @Test
    @DisplayName("用户资源列表测试")
    fun getUserResourceByPermissionTest() {
        val bkciAuthService = BkciAuthService(bkAuthConfig)
        val result1 = bkciAuthService.getUserResourceByPermission(
            user = "bkrepo",
            projectCode = "bkrepo",
            action = BkAuthPermission.DOWNLOAD,
            resourceType = BkAuthResourceType.PIPELINE_DEFAULT
        )
        Assertions.assertEquals(result1.size, 1)
        val result2 = bkciAuthService.getUserResourceByPermission(
            user = "bkrepo",
            projectCode = "bkrepo",
            action = BkAuthPermission.DOWNLOAD,
            resourceType = BkAuthResourceType.PIPELINE_DEFAULT
        )
        Assertions.assertEquals(result2.size, 0)
    }
}
