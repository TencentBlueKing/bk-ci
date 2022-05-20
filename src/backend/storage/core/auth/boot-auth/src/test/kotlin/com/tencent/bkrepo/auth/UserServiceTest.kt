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

import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.pojo.role.CreateRoleRequest
import com.tencent.bkrepo.auth.pojo.user.CreateUserRequest
import com.tencent.bkrepo.auth.pojo.user.CreateUserToProjectRequest
import com.tencent.bkrepo.auth.pojo.user.UpdateUserRequest
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.auth.util.DataDigestUtils
import com.tencent.bkrepo.auth.util.IDUtil
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
@DisplayName("用户服务测试")
class UserServiceTest {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var roleService: RoleService

    private val userId = "unit_test_id"
    private val username = "unit_test_name"
    private val userPwd = "unti_test_pwd"

    private val roleId = "test_project_manage"
    private val roleName = "测试项目管理员"
    private val testProjectId = "projectId_unit_test"
    private val testRepoName = "repoId_unit_test"

    private val repeatTimes = 10

    @BeforeEach
    fun setUp() {
        userService.getUserById(userId)?.let {
            userService.deleteById(userId)
        }
        roleService.detail(roleId, testProjectId)?.let {
            roleService.deleteRoleByid(it.id!!)
        }
    }

    @AfterEach
    fun teardown() {
        userService.getUserById(userId)?.let {
            userService.deleteById(userId)
        }
        roleService.detail(roleId, testProjectId)?.let {
            roleService.deleteRoleByid(it.id!!)
        }
        repeat(repeatTimes) {
            userService.getUserById(userId + "_" + it)?.let { user ->
                userService.deleteById(user.userId)
            }
        }
    }

    @Test
    @DisplayName("查询用户测试用例")
    fun getUserByIdTest() {
        val user = userService.getUserById(userId)
        Assertions.assertEquals(user, null)
        userService.createUser(createUserRequest())
        userService.getUserById(userId)?.let {
            Assertions.assertEquals(it.userId, userId)
            Assertions.assertEquals(it.name, username)
            Assertions.assertEquals(it.pwd, DataDigestUtils.md5FromStr(userPwd))
        }
    }

    @Test
    @DisplayName("创建用户测试用例")
    fun createUserTest() {
        val createUserRequest = createUserRequest()
        val createUser = userService.createUser(createUserRequest)
        Assertions.assertEquals(createUser, true)
        assertThrows<ErrorCodeException> { userService.createUser(createUserRequest) }
    }

    @Test
    @DisplayName("创建项目用户测试用例")
    fun createUserToProjectTest() {
        userService.createUser(createUserRequest())
        val createUserRequest = createUserToProjectRequest()
        assertThrows<ErrorCodeException> { userService.createUserToProject(createUserRequest) }
        userService.deleteById(userId)
        // projectId not exists
        assertThrows<ErrorCodeException> { userService.createUserToProject(createUserRequest) }
        // project test exists
        val userToProjectRequest = createUserToProjectRequest(projectId = "test")
        val isSuccess = userService.createUserToProject(userToProjectRequest)
        Assertions.assertEquals(isSuccess, true)
    }

    @Test
    @DisplayName("用户列表测试用例")
    fun listUserTest() {
        // 创建角色
        val roleId = createRole()!!
        val rids = listOf(roleId)
        var listUser = userService.listUser(rids)
        Assertions.assertTrue(listUser.size >= 0)
        // 创建用户关联角色
        userService.createUser(createUserRequest())
        userService.addUserToRole(userId, roleId)
        listUser = userService.listUser(rids)
        Assertions.assertEquals(listUser.size, 1)
    }

    @Test
    @DisplayName("根据用户ID删除用户测试用例")
    fun deleteByIdTest() {
        // user not exists
        assertThrows<ErrorCodeException> { userService.deleteById(userId) }
        userService.createUser(createUserRequest())
        val isSuccess = userService.deleteById(userId)
        Assertions.assertTrue(isSuccess)
    }

    @Test
    @DisplayName("用户修改测试用例")
    fun updateUserByIdTest() {
        userService.createUser(createUserRequest())
        val newUserName = "test1"
        val nwePwd = "!@#$%^&*"
        val updateUserRequest =
            UpdateUserRequest(newUserName, nwePwd)
        val result = userService.updateUserById(userId, updateUserRequest)
        Assertions.assertEquals(result, true)
        userService.getUserById(userId)?.let {
            Assertions.assertEquals(it.userId, userId)
            Assertions.assertEquals(it.name, newUserName)
            Assertions.assertEquals(it.pwd, DataDigestUtils.md5FromStr(nwePwd))
        }
    }

    @Test
    @DisplayName("添加用户到角色测试用例")
    fun addUserToRoleTest() {
        userService.createUser(createUserRequest())
        assertThrows<ErrorCodeException> { userService.addUserToRole(userId, roleId) }
        val roleRequest = CreateRoleRequest(
            roleId,
            roleName,
            RoleType.REPO,
            testProjectId,
            testRepoName,
            false
        )
        // 创建角色
        val roleId = roleService.createRole(roleRequest)!!

        val user = userService.addUserToRole(userId, roleId)
        user?.let {
            Assertions.assertEquals(it.userId, userId)
            Assertions.assertEquals(it.name, username)
        }
    }

    @Test
    @DisplayName("批量添加用户到角色测试用例")
    fun addUserToRoleBatchTest() {
        val idList = mutableListOf<String>()
        repeat(repeatTimes) {
            val userId = userId + "_" + it
            userService.createUser(createUserRequest(id = userId))
            idList.add(userId)
        }
        assertThrows<ErrorCodeException> { userService.addUserToRoleBatch(idList, roleId) }
        val roleRequest = CreateRoleRequest(
            roleId,
            roleName,
            RoleType.REPO,
            testProjectId,
            testRepoName,
            false
        )
        // 创建角色
        val roleId = roleService.createRole(roleRequest)!!
        val isSuccess = userService.addUserToRoleBatch(idList, roleId)
        Assertions.assertTrue(isSuccess)
    }

    @Test
    @DisplayName("删除用户的角色测试用例")
    fun removeUserFromRoleTest() {
        userService.createUser(createUserRequest())
        val roleId = createRole()!!
        assertThrows<Exception> { userService.removeUserFromRole(userId, roleId) }
        userService.addUserToRole(userId, roleId)
        val removeUserFromRole = userService.removeUserFromRole(userId, roleId)
        removeUserFromRole?.let {
            Assertions.assertFalse(it.roles.contains(roleId))
        }
    }

    @Test
    @DisplayName("删除用户的角色测试用例")
    fun removeUserFromRoleBatchTest() {
        val idList = mutableListOf<String>()
        repeat(repeatTimes) {
            val userId = userId + "_" + it
            userService.createUser(createUserRequest(id = userId))
            idList.add(userId)
        }
        val roleRequest = CreateRoleRequest(
            roleId,
            roleName,
            RoleType.REPO,
            testProjectId,
            testRepoName,
            false
        )
        // 创建角色
        val roleId = roleService.createRole(roleRequest)!!
        val isSuccess = userService.addUserToRoleBatch(idList, roleId)
        Assertions.assertTrue(isSuccess)
        val removeUserFromRoleBatch = userService.removeUserFromRoleBatch(idList, roleId)
        Assertions.assertTrue(removeUserFromRoleBatch)
    }

    @Test
    @DisplayName("创建token测试")
    fun createTokenTest() {
        userService.createUser(createUserRequest())
        val userWithToken = userService.createToken(userId)
        userWithToken?.let {
            Assertions.assertNotEquals(it.id, IDUtil.genRandomId())
        }
    }

    @Test
    @DisplayName("添加token测试用例")
    fun addUserTokenTest() {
        val token = IDUtil.genRandomId()
        val expiredAt = null
        userService.createUser(createUserRequest())
        userService.addUserToken(userId, token, expiredAt)?.let {
            Assertions.assertEquals(token, it.id)
        }
    }

    @Test
    @DisplayName("添加token测试用例")
    fun removeTokenTest() {
        userService.createUser(createUserRequest())
        val userWithToken = userService.createToken(userId)
        val removeToken = userService.removeToken(userId, userWithToken!!.id)
        removeToken.let {
            Assertions.assertTrue(it)
        }
    }

    @Test
    @DisplayName("通过密码或者token来寻找用户测试")
    fun findUserByUserTokenTest() {
        userService.createUser(createUserRequest())
        val user = userService.createToken(userId)
        val userWithPwd = userService.findUserByUserToken(userId, userPwd)!!
        val userWithToken = userService.findUserByUserToken(userId, user!!.id)!!
        Assertions.assertEquals(userWithPwd.toString(), userWithToken.toString())
    }

    private fun createUserRequest(
        id: String = userId,
        name: String = username,
        pwd: String = userPwd,
        admin: Boolean = false
    ): CreateUserRequest {
        return CreateUserRequest(
            id, name, pwd, admin
        )
    }

    private fun createUserToProjectRequest(
        id: String = userId,
        name: String = username,
        pwd: String = userPwd,
        admin: Boolean = false,
        asstUsers: List<String> = emptyList(),
        group: Boolean = false,
        projectId: String = testProjectId
    ): CreateUserToProjectRequest {
        return CreateUserToProjectRequest(
            id, name, pwd, admin, asstUsers, group, projectId
        )
    }

    private fun createRole(): String? {
        val roleRequest = CreateRoleRequest(
            roleId,
            roleName,
            RoleType.PROJECT,
            testProjectId,
            null,
            false
        )
        return roleService.createRole(roleRequest)
    }
}
