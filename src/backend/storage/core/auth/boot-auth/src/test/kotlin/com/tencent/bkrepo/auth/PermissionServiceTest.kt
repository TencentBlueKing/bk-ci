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

import com.tencent.bkrepo.auth.pojo.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.CreatePermissionRequest
import com.tencent.bkrepo.auth.pojo.CreateRoleRequest
import com.tencent.bkrepo.auth.pojo.CreateUserRequest
import com.tencent.bkrepo.auth.pojo.PermissionSet
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@DisplayName("权限测试")
class PermissionServiceTest {
    @Autowired
    private lateinit var permissionService: PermissionService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var roleService: RoleService

    private val userId = "unit_test_id"

    private val roleId = "manager_unit_test"
    private val projectId = "test"

    @AfterEach
    fun teardown() {
        // 删除创建的用户
        userService.getUserById(userId)?.let {
            userService.deleteById(userId)
        }

        // 删除创建的角色
        roleService.detail(roleId, projectId)?.let {
            roleService.deleteRoleByid(it.id!!)
        }

        // 删除创建的权限
        permissionService.listPermission(null, null, null).forEach {
            permissionService.deletePermission(it.id!!)
        }
    }

    @Test
    @DisplayName("创建权限测试")
    fun createPermissionTest() {
        // permName+projectId+resourceType unique
        val createPermissionRequest = createPermissionRequest(permName = "查询信息权限测试")
        val createPermission = permissionService.createPermission(createPermissionRequest)
        permissionService.createPermission(createPermissionRequest(permName = "查询信息权限测试", projectId = "test"))
        Assertions.assertTrue(createPermission)
        assertThrows<ErrorCodeException> { permissionService.createPermission(createPermissionRequest) }
    }

    @Test
    @DisplayName("权限列表测试")
    fun listPermissionTest() {
        permissionService.createPermission(createPermissionRequest(permName = "修改用户信息权限测试"))
        permissionService.createPermission(createPermissionRequest(permName = "增加用户信息权限测试", projectId = "test"))
        permissionService.createPermission(
            createPermissionRequest(permName = "修改用户信息权限测试", projectId = "test", repos = listOf("test-local"))
        )
        permissionService.createPermission(
            createPermissionRequest(
                permName = "权限测试",
                projectId = "test",
                repos = listOf("test-local"),
                resourceType = ResourceType.PROJECT
            )
        )
        permissionService.createPermission(
            createPermissionRequest(
                permName = "修改用户信息权限测试",
                projectId = "bkrepo_test",
                repos = listOf("test-local")
            )
        )
        permissionService.createPermission(
            createPermissionRequest(
                permName = "修改用户信息权限测试",
                projectId = "bkrepo_test",
                repos = listOf("test-local", "test-virtula"),
                resourceType = ResourceType.PROJECT
            )
        )
        permissionService.createPermission(
            createPermissionRequest(
                permName = "修改用户信息权限测试",
                projectId = "bk_test",
                repos = listOf("test-remote")
            )
        )
        permissionService.createPermission(
            createPermissionRequest(
                permName = "修改用户信息权限测试",
                projectId = "bk_test",
                repos = listOf("test-remote", "test-local"),
                resourceType = ResourceType.PROJECT
            )
        )
        val permissionList = permissionService.listPermission(null, null, null)
        Assertions.assertTrue(permissionList.size >= 8)
        val permissionList1 = permissionService.listPermission(ResourceType.PROJECT, null, null)
        Assertions.assertTrue(permissionList1.size >= 3)
        val permissionList2 = permissionService.listPermission(ResourceType.PROJECT, "bk_test", null)
        Assertions.assertTrue(permissionList2.size == 1)
        val permissionList3 = permissionService.listPermission(ResourceType.PROJECT, "bk_test", "test-local")
        Assertions.assertTrue(permissionList3.size == 1)
        val permissionList4 = permissionService.listPermission(ResourceType.REPO, null, null)
        Assertions.assertTrue(permissionList4.size >= 5)
        val permissionList5 = permissionService.listPermission(null, "bkrepo_test", null)
        Assertions.assertTrue(permissionList5.size == 0)
        val permissionList6 = permissionService.listPermission(null, null, "test-remote")
        Assertions.assertTrue(permissionList6.size == 0)
    }

    @Test
    @DisplayName("当用户是管理员时校验权限测试")
    fun checkPermissionWhenUserIsAdminTest() {
        // create user admin
        userService.createUser(createUserRequest(admin = true))

        // check permission when user is admin
        val checkPermissionRequest = CheckPermissionRequest(
            uid = userId,
            resourceType = ResourceType.PROJECT,
            action = PermissionAction.READ,
            projectId = "text",
            repoName = "test-local"
        )
        val checkPermission = permissionService.checkPermission(checkPermissionRequest)
        Assertions.assertTrue(checkPermission)
    }

    @Test
    @DisplayName("当用户角色为空时校验权限测试")
    fun checkPermissionWhenRolesIsEmpty() {
        // create user admin is false
        userService.createUser(createUserRequest())
        // create permission
        permissionService.createPermission(
            createPermissionRequest(
                permName = "权限测试", projectId = "test",
                repos = listOf("test-local"), resourceType = ResourceType.REPO,
                users = listOf(PermissionSet(userId, listOf(PermissionAction.READ, PermissionAction.WRITE)))
            )
        )
        // check permission when user role is empty
        val checkRequest = CheckPermissionRequest(
            uid = userId,
            resourceType = ResourceType.REPO,
            action = PermissionAction.READ,
            projectId = "test",
            repoName = "test-local"
        )
        val isSuccess = permissionService.checkPermission(checkRequest)
        Assertions.assertTrue(isSuccess)
    }

    @Test
    @DisplayName("当用户角色不为空时校验权限测试")
    fun checkPermissionWhenRolesExistAndResourceTypeIsProject() {
        // create user admin is false
        userService.createUser(createUserRequest())
        // create role
        val id = roleService.createRole(createRoleRequest(admin = true))
        userService.addUserToRole(userId, id!!)

        // create permission
        permissionService.createPermission(
            createPermissionRequest(
                permName = "权限测试", projectId = "test",
                repos = listOf("test-local"), resourceType = ResourceType.PROJECT,
                users = listOf(PermissionSet(userId, listOf(PermissionAction.READ, PermissionAction.WRITE)))
            )
        )
        // check permission when user role is empty
        val checkRequest = CheckPermissionRequest(
            uid = userId,
            resourceType = ResourceType.PROJECT,
            action = PermissionAction.READ,
            projectId = "test",
            repoName = "test-local"
        )
        val isSuccess = permissionService.checkPermission(checkRequest)
        Assertions.assertTrue(isSuccess)
    }

    @Test
    @DisplayName("当用户角色不为空时校验权限测试")
    fun checkPermissionWhenRolesExistAndResourceTypeIsRepo() {
        // create user admin is false
        userService.createUser(createUserRequest())
        // create role
        val id = roleService.createRole(createRoleRequest(admin = true))
        userService.addUserToRole(userId, id!!)

        // create permission
        permissionService.createPermission(
            createPermissionRequest(
                permName = "权限测试", projectId = "test",
                repos = listOf("test-local"), resourceType = ResourceType.REPO,
                users = listOf(PermissionSet(userId, listOf(PermissionAction.READ, PermissionAction.WRITE)))
            )
        )
        // check permission when user role is empty
        val checkRequest = CheckPermissionRequest(
            uid = userId,
            resourceType = ResourceType.REPO,
            action = PermissionAction.READ,
            projectId = "test",
            repoName = "test-local"
        )
        val isSuccess = permissionService.checkPermission(checkRequest)
        Assertions.assertTrue(isSuccess)
    }

    @Test
    @DisplayName("删除权限测试用例")
    fun deletePermissionTest() {
        permissionService.createPermission(createPermissionRequest(permName = "查询信息权限测试", projectId = "test"))
        permissionService.listPermission(ResourceType.REPO, "test", null).forEach {
            permissionService.deletePermission(it.id!!)
        }
    }

    @Test
    @DisplayName("修改包含路径测试用例")
    fun updateIncludePathTest() {
        assertThrows<ErrorCodeException> { permissionService.updateIncludePath("test_test", listOf("/include")) }
        permissionService.createPermission(createPermissionRequest(permName = "查询信息权限测试", projectId = "test"))
        permissionService.listPermission(ResourceType.REPO, "test", null).forEach {
            val updateIncludePath = permissionService.updateIncludePath(it.id!!, listOf("/include"))
            Assertions.assertTrue(updateIncludePath)
        }
    }

    @Test
    @DisplayName("修改排除路径测试用例")
    fun updateExcludePathTest() {
        assertThrows<ErrorCodeException> { permissionService.updateExcludePath("test_test", listOf("/exclude")) }
        permissionService.createPermission(createPermissionRequest(permName = "查询信息权限测试", projectId = "test"))
        permissionService.listPermission(ResourceType.REPO, "test", null).forEach {
            val updateExcludePath = permissionService.updateExcludePath(it.id!!, listOf("/exclude"))
            Assertions.assertTrue(updateExcludePath)
        }
    }

    @Test
    @DisplayName("更新权限绑定repo测试")
    fun updateRepoPermissionTest() {
        assertThrows<ErrorCodeException> { permissionService.updateRepoPermission("test_test", listOf("test-local")) }
        permissionService.createPermission(createPermissionRequest(permName = "查询信息权限测试", projectId = "test"))
        permissionService.listPermission(ResourceType.REPO, "test", null).forEach {
            val updateStatus = permissionService.updateRepoPermission(it.id!!, listOf("test-local", "test-remote"))
            Assertions.assertTrue(updateStatus)
        }
    }

    @Test
    @DisplayName("更新权限绑定用户测试")
    fun updateUserPermissionTest() {
        userService.createUser(createUserRequest())
        assertThrows<ErrorCodeException> {
            permissionService.updateUserPermission(
                "test_test",
                userId,
                listOf(PermissionAction.WRITE, PermissionAction.MANAGE)
            )
        }
        permissionService.createPermission(createPermissionRequest(permName = "查询信息权限测试", projectId = "test"))
        permissionService.listPermission(ResourceType.REPO, "test", null).forEach {
            val updateStatus = permissionService.updateUserPermission(
                it.id!!,
                userId,
                listOf(PermissionAction.WRITE, PermissionAction.UPDATE, PermissionAction.DELETE)
            )
            Assertions.assertTrue(updateStatus)
        }
    }

    @Test
    @DisplayName("删除权限绑定用户测试")
    fun removeUserPermissionTest() {
        assertThrows<ErrorCodeException> {
            permissionService.removeUserPermission(
                "test_test",
                userId
            )
        }
        permissionService.createPermission(
            createPermissionRequest(
                permName = "查询信息权限测试", projectId = "test",
                users = listOf(
                    PermissionSet(userId, listOf(PermissionAction.DELETE, PermissionAction.UPDATE)),
                    PermissionSet("test_userId", listOf(PermissionAction.MANAGE, PermissionAction.UPDATE))
                )
            )
        )
        permissionService.listPermission(ResourceType.REPO, "test", null).forEach {
            val updateStatus = permissionService.removeUserPermission(
                it.id!!,
                userId
            )
            Assertions.assertTrue(updateStatus)
        }
    }

    @Test
    @DisplayName("更新权限绑定角色测试")
    fun updateRolePermissionTest() {
        val rid = roleService.createRole(createRoleRequest())!!
        assertThrows<ErrorCodeException> {
            permissionService.updateRolePermission(
                "test_test",
                rid,
                listOf(PermissionAction.WRITE, PermissionAction.MANAGE)
            )
        }
        permissionService.createPermission(createPermissionRequest(permName = "查询信息权限测试", projectId = "test"))
        permissionService.listPermission(ResourceType.REPO, "test", null).forEach {
            val updateStatus = permissionService.updateRolePermission(
                it.id!!,
                rid,
                listOf(PermissionAction.WRITE, PermissionAction.UPDATE, PermissionAction.DELETE)
            )
            Assertions.assertTrue(updateStatus)
        }
    }

    @Test
    @DisplayName("删除权限绑定角色测试")
    fun removeRolePermissionTest() {
        val rid = roleService.createRole(createRoleRequest())!!
        assertThrows<ErrorCodeException> {
            permissionService.removeRolePermission(
                "test_test",
                rid
            )
        }
        permissionService.createPermission(
            createPermissionRequest(
                permName = "查询信息权限测试", projectId = "test",
                roles = listOf(
                    PermissionSet(rid, listOf(PermissionAction.DELETE, PermissionAction.UPDATE)),
                    PermissionSet("test_roleId", listOf(PermissionAction.READ, PermissionAction.UPDATE))
                )
            )
        )
        permissionService.listPermission(ResourceType.REPO, "test", null).forEach {
            val updateStatus = permissionService.removeRolePermission(
                it.id!!,
                rid
            )
            Assertions.assertTrue(updateStatus)
        }
    }

    private fun createUserRequest(
        id: String = userId,
        admin: Boolean = false
    ): CreateUserRequest {
        return CreateUserRequest(
            id, "unit_test_name", "unti_test_pwd", admin
        )
    }

    private fun createRoleRequest(
        roleId: String = this.roleId,
        type: RoleType = RoleType.PROJECT,
        projectId: String = this.projectId,
        repoName: String? = null,
        admin: Boolean = false
    ): CreateRoleRequest {
        return CreateRoleRequest(roleId, "测试项目管理员", type, projectId, repoName, admin)
    }

    private fun createPermissionRequest(
        resourceType: ResourceType = ResourceType.REPO,
        projectId: String? = null,
        permName: String,
        repos: List<String> = emptyList(),
        includePattern: List<String> = emptyList(),
        excludePattern: List<String> = emptyList(),
        users: List<PermissionSet> = emptyList(),
        roles: List<PermissionSet> = emptyList(),
        createBy: String = "admin",
        updatedBy: String = "admin"
    ): CreatePermissionRequest {
        return CreatePermissionRequest(
            resourceType,
            projectId,
            permName,
            repos,
            includePattern,
            excludePattern,
            users,
            roles,
            createBy,
            updatedBy
        )
    }
}
