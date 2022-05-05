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

package com.tencent.bkrepo.auth.service.oauth

import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TAccount
import com.tencent.bkrepo.auth.model.TOauthToken
import com.tencent.bkrepo.auth.pojo.oauth.AuthorizationGrantType
import com.tencent.bkrepo.auth.pojo.oauth.OauthToken
import com.tencent.bkrepo.auth.repository.AccountRepository
import com.tencent.bkrepo.auth.repository.OauthTokenRepository
import com.tencent.bkrepo.auth.service.OauthAuthorizationService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.auth.util.OauthUtils
import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.api.util.toXmlString
import com.tencent.bkrepo.common.redis.RedisOperation
import com.tencent.bkrepo.common.service.util.HeaderUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.TimeUnit

@Service
class OauthAuthorizationServiceImpl(
    private val accountRepository: AccountRepository,
    private val oauthTokenRepository: OauthTokenRepository,
    private val userService: UserService,
    private val redisOperation: RedisOperation
) : OauthAuthorizationService {

    override fun authorized(clientId: String, state: String) {
        val client = accountRepository.findById(clientId)
            .orElseThrow { ErrorCodeException(AuthMessageCode.AUTH_CLIENT_NOT_EXIST) }
        val code = OauthUtils.generateCode()

        val userId = HttpContextHolder.getRequest().getAttribute(USER_KEY).toString()
        val userIdKey = "$clientId:$code:userId"
        redisOperation.set(userIdKey, userId, TimeUnit.MINUTES.toSeconds(10L))

        val redirectUrl = "${client.redirectUri!!.removeSuffix(StringPool.SLASH)}?code=$code&state=$state"
        HttpContextHolder.getResponse().sendRedirect(redirectUrl)
    }

    override fun createToken(clientId: String, clientSecret: String, code: String) {
        val userIdKey = "$clientId:$code:userId"
        val userId = redisOperation.get(userIdKey) ?: throw ErrorCodeException(AuthMessageCode.AUTH_CODE_CHECK_FAILED)

        val client = checkClientSecret(clientId, clientSecret)
        var tOauthToken = oauthTokenRepository.findFirstByAccountIdAndUserId(clientId, userId)
        if (tOauthToken == null) {
            tOauthToken = TOauthToken(
                accessToken = OauthUtils.generateAccessToken(),
                type = "Bearer",
                accountId = clientId,
                userId = userId,
                scope = client.scope!!,
                issuedAt = Instant.now()
            )
            oauthTokenRepository.insert(tOauthToken)
        } else if (client.scope != tOauthToken.scope) {
            tOauthToken.scope = client.scope!!
            oauthTokenRepository.save(tOauthToken)
        }
        userService.addUserAccount(userId, client.id!!)

        val token = transfer(tOauthToken)
        val accept = HeaderUtils.getHeader(HttpHeaders.ACCEPT).orEmpty()
        val response = HttpContextHolder.getResponse()
        response.addHeader(HttpHeaders.CACHE_CONTROL, "no-store")
        response.addHeader(HttpHeaders.PRAGMA, "no-cache")
        when {
            accept.contains(MediaType.APPLICATION_JSON_VALUE) -> {
                response.contentType = MediaTypes.APPLICATION_JSON
                response.writer.write(token.toJsonString())
            }
            accept.contains(MediaType.APPLICATION_XML_VALUE) -> {
                response.contentType = MediaTypes.APPLICATION_XML
                response.writer.write(token.toXmlString())
            }
            else -> {
                response.writer.write(token.toString())
            }
        }
    }

    override fun getToken(accessToken: String): OauthToken? {
        val tOauthToken = oauthTokenRepository.findFirstByAccessToken(accessToken) ?: return null
        return transfer(tOauthToken)
    }

    override fun validateToken(accessToken: String): String? {
        return oauthTokenRepository.findFirstByAccessToken(accessToken)?.userId
    }

    override fun deleteToken(clientId: String, clientSecret: String, accessToken: String) {
        checkClientSecret(clientId, clientSecret)
        oauthTokenRepository.deleteByAccessToken(accessToken)
    }

    private fun transfer(tOauthToken: TOauthToken) = OauthToken(
        accessToken = tOauthToken.accessToken,
        tokenType = tOauthToken.type,
        scope = tOauthToken.scope.joinToString(StringPool.COMMA)
    )

    private fun checkClientSecret(clientId: String, clientSecret: String): TAccount {
        val client = accountRepository.findById(clientId)
            .orElseThrow { ErrorCodeException(AuthMessageCode.AUTH_CLIENT_NOT_EXIST) }

        if (client.credentials.find {
            it.secretKey == clientSecret &&
                it.authorizationGrantType == AuthorizationGrantType.AUTHORIZATION_CODE
        } == null
        ) {
            throw ErrorCodeException(AuthMessageCode.AUTH_SECRET_CHECK_FAILED)
        }
        return client
    }
}
