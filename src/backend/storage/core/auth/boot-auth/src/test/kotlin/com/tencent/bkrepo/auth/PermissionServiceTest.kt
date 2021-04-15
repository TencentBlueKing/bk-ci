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

package com.tencent.bkrepo.auth

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.CreatePermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionPathRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRepoRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRoleRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionUserRequest
import com.tencent.bkrepo.auth.pojo.role.CreateRoleRequest
import com.tencent.bkrepo.auth.pojo.user.CreateUserRequest
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

        val permissionList2 = permissionService.listPermission("bk_test", null)
        Assertions.assertTrue(permissionList2.size == 1)
        val permissionList3 = permissionService.listPermission("bk_test", "test-local")
        Assertions.assertTrue(permissionList3.size == 1)
        val permissionList5 = permissionService.listPermission("bkrepo_test", null)
        Assertions.assertTrue(permissionList5.isEmpty())
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
                users = listOf(userId),
                actions = listOf(PermissionAction.READ, PermissionAction.WRITE)
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
                users = listOf(userId),
                actions = listOf(PermissionAction.READ, PermissionAction.WRITE)
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
                repos = listOf("test-local"),
                resourceType = ResourceType.REPO,
                users = listOf(userId),
                actions = listOf(PermissionAction.READ, PermissionAction.WRITE)
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
        permissionService.listPermission("test", null).forEach {
            permissionService.deletePermission(it.id!!)
        }
    }

    @Test
    @DisplayName("修改包含路径测试用例")
    fun updateIncludePathTest() {
        assertThrows<ErrorCodeException> {
            permissionService.updateIncludePath(
                UpdatePermissionPathRequest(
                    "test_test",
                    listOf("/include")
                )
            )
        }
        permissionService.createPermission(createPermissionRequest(permName = "查询信息权限测试", projectId = "test"))
        permissionService.listPermission("test", null).forEach {
            val request = UpdatePermissionPathRequest(it.id!!, listOf("/include"))
            val updateIncludePath = permissionService.updateIncludePath(request)
            Assertions.assertTrue(updateIncludePath)
        }
    }

    @Test
    @DisplayName("修改排除路径测试用例")
    fun updateExcludePathTest() {
        assertThrows<ErrorCodeException> {
            permissionService.updateExcludePath(
                UpdatePermissionPathRequest(
                    "test_test",
                    listOf("/exclude")
                )
            )
        }
        permissionService.createPermission(createPermissionRequest(permName = "查询信息权限测试", projectId = "test"))
        permissionService.listPermission("test", null).forEach {
            val request = UpdatePermissionPathRequest(it.id!!, listOf("/exclude"))
            val updateExcludePath = permissionService.updateExcludePath(request)
            Assertions.assertTrue(updateExcludePath)
        }
    }

    @Test
    @DisplayName("更新权限绑定repo测试")
    fun updateRepoPermissionTest() {
        assertThrows<ErrorCodeException> {
            permissionService.updateRepoPermission(
                UpdatePermissionRepoRequest(
                    "test_test",
                    listOf("test-local")
                )
            )
        }
        permissionService.createPermission(createPermissionRequest(permName = "查询信息权限测试", projectId = "test"))
        permissionService.listPermission("test", null).forEach {
            val request = UpdatePermissionRepoRequest(it.id!!, listOf("test-local", "test-remote"))
            val updateStatus = permissionService.updateRepoPermission(request)
            Assertions.assertTrue(updateStatus)
        }
    }

    @Test
    @DisplayName("更新权限绑定用户测试")
    fun updateUserPermissionTest() {
        userService.createUser(createUserRequest())
        assertThrows<ErrorCodeException> {
            permissionService.updatePermissionUser(UpdatePermissionUserRequest("test_test", listOf(userId)))
        }
        permissionService.createPermission(createPermissionRequest(permName = "查询信息权限测试", projectId = "test"))
        permissionService.listPermission("test", null).forEach {
            val request = UpdatePermissionUserRequest(it.id!!, listOf(userId))
            val updateStatus = permissionService.updatePermissionUser(request)
            Assertions.assertTrue(updateStatus)
        }
    }

    @Test
    @DisplayName("更新权限绑定角色测试")
    fun updateRolePermissionTest() {
        val rid = roleService.createRole(createRoleRequest())!!
        assertThrows<ErrorCodeException> {
            permissionService.updatePermissionRole(
                UpdatePermissionRoleRequest("test_test", listOf(rid))
            )
        }
        permissionService.createPermission(createPermissionRequest(permName = "查询信息权限测试", projectId = "test"))
        permissionService.listPermission("test", null).forEach {
            val request = UpdatePermissionRoleRequest(it.id!!, listOf(rid))
            val updateStatus = permissionService.updatePermissionRole(request)
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
        return CreateRoleRequest(
            roleId,
            "测试项目管理员",
            type,
            projectId,
            repoName,
            admin
        )
    }

    private fun createPermissionRequest(
        resourceType: ResourceType = ResourceType.REPO,
        projectId: String? = null,
        permName: String,
        repos: List<String> = emptyList(),
        includePattern: List<String> = emptyList(),
        excludePattern: List<String> = emptyList(),
        users: List<String> = emptyList(),
        roles: List<String> = emptyList(),
        departments: List<String> = emptyList(),
        actions: List<PermissionAction> = emptyList(),
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
            departments,
            actions,
            createBy,
            updatedBy
        )
    }
}
