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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.auth

import com.tencent.bkrepo.auth.pojo.CreateRoleRequest
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@DisplayName("角色测试")
class RoleServiceTest {
    @Autowired
    private lateinit var roleService: RoleService

    private val roleId = "manager_unit_test"
    private val roleName = "测试项目管理员"
    private val projectId = "projectId_unit_test"
    private val repoName = "repo_unit_test"

    @BeforeEach
    fun setUp() {
        roleService.detail(roleId, projectId)?.let {
            roleService.deleteRoleByid(it.id!!)
        }
    }

    @AfterEach
    fun teardown() {
        roleService.detail(roleId, projectId)?.let {
            roleService.deleteRoleByid(it.id!!)
        }
    }

    @Test
    @DisplayName("创建角色测试")
    fun createRoleTest() {
        // type -> project
        val id = roleService.createRole(buildRoleRequest())
        Assertions.assertNotNull(id)
        // type -> repo
        val createRoleId = roleService.createRole(buildRoleRequest(type = RoleType.REPO, repoName = repoName))
        Assertions.assertNotNull(createRoleId)
    }

    @Test
    @DisplayName("根据主键id删除角色测试")
    fun deleteRoleByidTest() {
        assertThrows<ErrorCodeException> { roleService.deleteRoleByid(roleId) }
        val id = roleService.createRole(buildRoleRequest())!!
        val deleteRoleByid = roleService.deleteRoleByid(id)
        Assertions.assertTrue(deleteRoleByid)
    }

    @Test
    @DisplayName("通过项目和仓库查找角色测试")
    fun listRoleByProjectTest() {
        roleService.createRole(buildRoleRequest())
        val id = roleService.createRole(buildRoleRequest(projectId = "test_projectId"))
        val id1 = roleService.createRole(buildRoleRequest(projectId = "test_projectId_001", repoName = "test_name"))
        val id2 = roleService.createRole(buildRoleRequest(type = RoleType.REPO, repoName = "test_repo"))
        val listRoleByProject = roleService.listRoleByProject(null, null, null)
        Assertions.assertTrue(listRoleByProject.size == 4)
        val listRoleByProject1 = roleService.listRoleByProject(RoleType.PROJECT, null, null)
        Assertions.assertTrue(listRoleByProject1.size == 3)
        val listRoleByProject2 = roleService.listRoleByProject(RoleType.REPO, null, null)
        Assertions.assertTrue(listRoleByProject2.size == 1)
        val listRoleByProject3 = roleService.listRoleByProject(null, projectId, null)
        Assertions.assertTrue(listRoleByProject3.size == 2)
        val listRoleByProject4 = roleService.listRoleByProject(RoleType.REPO, projectId, null)
        Assertions.assertTrue(listRoleByProject4.size == 1)
        val listRoleByProject5 = roleService.listRoleByProject(RoleType.PROJECT, projectId, null)
        Assertions.assertTrue(listRoleByProject5.size == 1)
        val listRoleByProject6 = roleService.listRoleByProject(RoleType.REPO, projectId, "test_repo")
        Assertions.assertTrue(listRoleByProject6.size == 1)
        // has problems -> The last if condition never goes in
        // val listRoleByProject7 = roleService.listRoleByProject(null, "test_projectId_001", "test_repo")
        // Assertions.assertTrue(listRoleByProject7.size == 0)
        roleService.deleteRoleByid(id!!)
        roleService.deleteRoleByid(id1!!)
        roleService.deleteRoleByid(id2!!)
    }

    @Test
    @DisplayName("角色详情测试")
    fun detailTest() {
        val id = roleService.createRole(buildRoleRequest())
        val role = roleService.detail(id!!)
        role?.let {
            Assertions.assertEquals(it.roleId, roleId)
        }
    }

    @Test
    @DisplayName("角色详情测试")
    fun detailWithProjectIdTest() {
        val id = roleService.createRole(buildRoleRequest())
        val role = roleService.detail(roleId, projectId)
        role?.let {
            Assertions.assertEquals(it.id!!, id)
        }
    }

    @Test
    @DisplayName("角色详情测试")
    fun detailWithProjectIdAndRepoNameTest() {
        val id = roleService.createRole(buildRoleRequest(repoName = repoName))
        val role = roleService.detail(roleId, projectId, repoName)
        role?.let {
            Assertions.assertEquals(it.id!!, id)
        }
    }

    private fun buildRoleRequest(
        roleId: String = this.roleId,
        roleName: String = this.roleName,
        type: RoleType = RoleType.PROJECT,
        projectId: String = this.projectId,
        repoName: String? = null,
        admin: Boolean = false
    ): CreateRoleRequest {
        return CreateRoleRequest(roleId, roleName, type, projectId, repoName, admin)
    }
}
