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

package com.tencent.bkrepo.auth

import com.tencent.bkrepo.auth.config.RedisTestConfig
import com.tencent.bkrepo.auth.pojo.account.Account
import com.tencent.bkrepo.auth.pojo.account.CreateAccountRequest
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.oauth.AuthorizationGrantType
import com.tencent.bkrepo.auth.service.AccountService
import com.tencent.bkrepo.auth.service.OauthAuthorizationService
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.redis.RedisOperation
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.TimeUnit

@SpringBootTest(classes = [RedisTestConfig::class])
@DisplayName("Oauth授权服务相关接口")
class OauthAuthorizationServiceTest {

    @Autowired
    private lateinit var accountService: AccountService
    @Autowired
    private lateinit var oauthAuthorizationService: OauthAuthorizationService
    @Autowired
    private lateinit var redisOperation: RedisOperation

    private val userId = "test"
    private val appId = "unit-test-app"
    private val homepageUrl = "http://localhost"
    private val redirectUri = "http://localhost/redirect"
    private val scope = setOf(ResourceType.PROJECT, ResourceType.REPO, ResourceType.NODE)
    private val type = setOf(AuthorizationGrantType.AUTHORIZATION_CODE)

    private lateinit var account: Account

    @BeforeEach
    fun setUp() {
        HttpContextHolder.getRequest().setAttribute(USER_KEY, userId)
        account = try {
            accountService.deleteAccount(appId)
            accountService.createAccount(buildCreateAccountRequest())
        } catch (exception: ErrorCodeException) {
            accountService.createAccount(buildCreateAccountRequest())
        }
    }

    @AfterEach
    fun tearDown() {
        accountService.deleteAccount(appId)
    }

    @Test
    fun createTokenTest() {
        val clientId = account.id
        val clientSecret = account.credentials.first().secretKey
        val mockCode = "123"
        val userIdKey = "$clientId:$mockCode:userId"
        redisOperation.set(userIdKey, userId, TimeUnit.SECONDS.toSeconds(10L))
        oauthAuthorizationService.createToken(clientId, clientSecret, mockCode)
        Assertions.assertTrue(accountService.listAuthorizedAccount().find { it.id == clientId } != null)
    }

    private fun buildCreateAccountRequest(
        appId: String = this.appId,
        locked: Boolean = false,
        homepageUrl: String = this.homepageUrl,
        redirectUri: String = this.redirectUri,
        scope: Set<ResourceType> = this.scope,
        authorizationGrantTypes: Set<AuthorizationGrantType> = this.type
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
}
