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

import com.tencent.bkrepo.auth.constant.RANDOM_KEY_LENGTH
import com.tencent.bkrepo.auth.pojo.account.CreateAccountRequest
import com.tencent.bkrepo.auth.pojo.account.UpdateAccountRequest
import com.tencent.bkrepo.auth.pojo.enums.CredentialStatus
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.oauth.AuthorizationGrantType
import com.tencent.bkrepo.auth.service.AccountService
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@DisplayName("服务账号相关接口")
class AccountServiceTest {

    @Autowired
    private lateinit var accountService: AccountService

    private val userId = "test"
    private val appId = "unit_test_id"
    private val testAppId1 = "test1"
    private val testAppId2 = "test2"
    private val appIdList = listOf(appId, testAppId1, testAppId2)
    private val homepageUrl = "http://localhost"
    private val redirectUri = "http://localhost/redirect"
    private val scope = setOf(ResourceType.PROJECT, ResourceType.REPO, ResourceType.NODE)
    private val allTypes =
        setOf(AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.PLATFORM)
    private val platformType = setOf(AuthorizationGrantType.PLATFORM)

    @BeforeEach
    fun setUp() {
        HttpContextHolder.getRequest().setAttribute(USER_KEY, "admin")
        accountService.listAccount().filter { appIdList.contains(it.appId) }.forEach {
            accountService.deleteAccount(it.appId)
        }
        HttpContextHolder.getRequest().setAttribute(USER_KEY, userId)
    }

    @AfterEach
    fun teardown() {
        HttpContextHolder.getRequest().setAttribute(USER_KEY, "admin")
        accountService.listAccount().filter { appIdList.contains(it.appId) }.forEach {
            accountService.deleteAccount(it.appId)
        }
    }

    @Test
    @DisplayName("创建账户测试")
    fun createAccountTest() {
        val account = accountService.createAccount(buildCreateAccountRequest())
        assertThrows<ErrorCodeException> { accountService.createAccount(buildCreateAccountRequest()) }
        Assertions.assertEquals(account.appId, appId)
        Assertions.assertFalse(account.locked)
        Assertions.assertTrue(account.credentials.size == account.authorizationGrantTypes.size)
    }

    @Test
    @DisplayName("查询账户测试")
    fun listAccountTest() {
        accountService.createAccount(buildCreateAccountRequest())
        accountService.createAccount(buildCreateAccountRequest(appId = "test1"))
        accountService.createAccount(buildCreateAccountRequest(appId = "test2", locked = true))
        Assertions.assertTrue(accountService.listOwnAccount().size == 3)
        accountService.deleteAccount("test1")
        accountService.deleteAccount("test2")
    }

    @Test
    @DisplayName("删除账户测试")
    fun deleteAccountTest() {
        accountService.createAccount(buildCreateAccountRequest())
        accountService.deleteAccount(appId)
        assertThrows<ErrorCodeException> { accountService.deleteAccount(appId) }
    }

    @Test
    @DisplayName("修改账户测试")
    fun updateAccountTest() {
        assertThrows<ErrorCodeException> { accountService.updateAccount(buildUpdateAccountRequest()) }
        accountService.createAccount(buildCreateAccountRequest())
        var updateAccount = accountService.updateAccount(buildUpdateAccountRequest())
        Assertions.assertTrue(updateAccount)
        var account = accountService.findAccountByAppId(appId)
        Assertions.assertTrue(account.credentials.size == 1)
        updateAccount = accountService.updateAccount(buildUpdateAccountRequest(authorizationGrantTypes = allTypes))
        Assertions.assertTrue(updateAccount)
        account = accountService.findAccountByAppId(appId)
        Assertions.assertTrue(account.credentials.size == 2)
    }

    @Test
    @DisplayName("创建ak/sk对测试")
    fun createCredentialTest() {
        assertThrows<ErrorCodeException> {
            accountService.createCredential(appId, AuthorizationGrantType.AUTHORIZATION_CODE)
        }
        // 创建账户每个认证授权类型会自带创建一个as/sk对
        accountService.createAccount(buildCreateAccountRequest())
        val credential = accountService.createCredential(appId, AuthorizationGrantType.AUTHORIZATION_CODE)
        val account = accountService.findAccountByAppId(appId)
        Assertions.assertTrue(account.credentials.size == 3)
        with(credential) {
            Assertions.assertTrue(this.accessKey.length == 32)
            Assertions.assertTrue(this.secretKey.length == RANDOM_KEY_LENGTH)
        }
    }

    @Test
    @DisplayName("获取as/sk对测试")
    fun listCredentialsTest() {
        accountService.createAccount(buildCreateAccountRequest())
        val credentialsList = accountService.listCredentials(appId)
        Assertions.assertTrue(credentialsList.size == 2)
    }

    @Test
    @DisplayName("删除as/sk对测试")
    fun deleteCredentialTest() {
        val account = accountService.createAccount(buildCreateAccountRequest())
        assertThrows<ErrorCodeException> {
            accountService.deleteCredential(account.appId, account.credentials[0].accessKey)
        }
        accountService.createCredential(account.appId, AuthorizationGrantType.PLATFORM)
        val result = accountService.deleteCredential(account.appId, account.credentials.last().accessKey)
        Assertions.assertTrue(result)
    }

    @Test
    @DisplayName("更新ak/sk对状态测试")
    fun updateCredentialStatusTest() {
        val account = accountService.createAccount(buildCreateAccountRequest())
        val accessKey = account.credentials[0].accessKey
        val credentialStatus = account.credentials[0].status
        Assertions.assertEquals(credentialStatus, CredentialStatus.ENABLE)
        val status = accountService.updateCredentialStatus(account.appId, accessKey, CredentialStatus.DISABLE)
        Assertions.assertTrue(status)
    }

    @Test
    @DisplayName("校验ak/sk")
    fun checkCredentialTest() {
        val account = accountService.createAccount(buildCreateAccountRequest())
        val accessKey = account.credentials[0].accessKey
        val secretKey = account.credentials[0].secretKey
        val checkCredential = accountService.checkCredential("accessKey", "secretKey")
        Assertions.assertNull(checkCredential)
        val credential = accountService.checkCredential(accessKey, secretKey)
        credential?.let { Assertions.assertEquals(appId, it) }
    }

    private fun buildCreateAccountRequest(
        appId: String = this.appId,
        locked: Boolean = false,
        homepageUrl: String = this.homepageUrl,
        redirectUri: String = this.redirectUri,
        scope: Set<ResourceType> = this.scope,
        authorizationGrantTypes: Set<AuthorizationGrantType> = this.allTypes
    ): CreateAccountRequest {
        return CreateAccountRequest(
            appId = appId,
            locked = locked,
            authorizationGrantTypes = authorizationGrantTypes,
            homepageUrl = homepageUrl,
            redirectUri = redirectUri,
            scope = scope,
            avatarUrl = null,
            description = null
        )
    }

    private fun buildUpdateAccountRequest(
        appId: String = this.appId,
        locked: Boolean = false,
        homepageUrl: String = this.homepageUrl,
        redirectUri: String = this.redirectUri,
        scope: Set<ResourceType> = this.scope,
        authorizationGrantTypes: Set<AuthorizationGrantType> = this.platformType
    ): UpdateAccountRequest {
        return UpdateAccountRequest(
            appId = appId,
            locked = locked,
            authorizationGrantTypes = authorizationGrantTypes,
            homepageUrl = homepageUrl,
            redirectUri = redirectUri,
            scope = scope,
            avatarUrl = null,
            description = null
        )
    }
}
