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

package com.tencent.devops.auth.service

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.pojo.enum.LoginType
import com.tencent.devops.auth.pojo.enum.UserStatus
import com.tencent.devops.common.api.auth.AUTH_HEADER_BK_CI_LOGIN_TOKEN
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.ws.rs.core.Cookie
import javax.ws.rs.core.NewCookie
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder

@Service
class ThirdLoginService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val userInfoService: UserInfoService
) {

    @Value("\${login.third.url:#{null}}")
    val callbackUrl: String = ""

    @Value("\${login.third.domain:#{null}}")
    val domain: String = ""

    fun thirdLogin(code: String, userId: String, type: String, email: String?): Response {
        logger.info("$userId login by $type")

        // code校验不通过直接报错
        if (!checkCode(code, userId, type)) {
            throw PermissionForbiddenException(AuthMessageCode.LOGIN_THIRD_CODE_INVALID)
        }

        val token = buildLoginToken(userId, type)
        val cookie = Cookie(AUTH_HEADER_BK_CI_LOGIN_TOKEN, "$type:$token", "/", domain)
        val userType = LoginType.getTypeNum(type).typeNum

        val userInfo = userInfoService.getUserInfo(userId, userType)
        if (userInfo == null) {
            userInfoService.thirdLoginAndRegister(userId, userType, email)
        } else {
            if (userInfo.userStatus == UserStatus.FREEZE.id) {
                logger.warn("user is freeze , user:$userId")
                throw ErrorCodeException(
                    errorCode = AuthMessageCode.LOGIN_USER_FREEZE,
                    defaultMessage = "user is freeze , user:$userId"
                )
            }
        }

        logger.info("cookie: $cookie")
        return Response.temporaryRedirect(
            UriBuilder.fromUri(callbackUrl).build()
        )
            .cookie(NewCookie(cookie, "", LOGIN_EXPIRE_TIME.toInt(), false))
            .build()
    }

    fun verifyToken(token: String): String {
        return redisOperation.get("$LOGIN_REDIS_KEY$token") ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.LOGIN_TOKEN_VERIFY_FAILED,
            defaultMessage = "token verify failed , token:$token"
        )
    }

    fun thirdLoginOut(userId: String): Response {
        logger.info("$userId loginOut")
        redisOperation.delete(LOGIN_REDIS_KEY + userId)
        val cookie = Cookie(AUTH_HEADER_BK_CI_LOGIN_TOKEN, null, "/", domain)
        return Response.temporaryRedirect(UriBuilder.fromUri(callbackUrl).build())
            .cookie(NewCookie(cookie, "", 0, false))
            .build()
    }

    private fun checkCode(code: String, userId: String, type: String): Boolean {
        val githubCodeKey = String.format(LOGIN_CODE_REDIS_KEY, type, userId)
        val redisCode = redisOperation.get(githubCodeKey)
        return code == redisCode
    }

    private fun buildLoginToken(userId: String, type: String): String {
        val token = UUIDUtil.generate()
        redisOperation.set("$LOGIN_REDIS_KEY$type:$token", userId, LOGIN_EXPIRE_TIME)
        return token
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdLoginService::class.java)
        const val LOGIN_EXPIRE_TIME = 7 * 24 * 3600L // 登录7天有效
        const val LOGIN_REDIS_KEY = "bk:login:third:key:"
        const val LOGIN_CODE_REDIS_KEY = "bk:login:third:%s:code:%s"
    }
}
