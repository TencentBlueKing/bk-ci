/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.repository.service.scm

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.RepoAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.security.util.BkCryptoUtil
import com.tencent.devops.model.repository.tables.records.TRepositoryScmTokenRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.dao.RepositoryScmTokenDao
import com.tencent.devops.repository.pojo.Oauth2State
import com.tencent.devops.repository.pojo.enums.TokenAppTypeEnum
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.repository.pojo.oauth.RepositoryScmToken
import com.tencent.devops.repository.service.hub.ScmTokenApiService
import com.tencent.devops.scm.api.pojo.Oauth2AccessToken
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.time.LocalDateTime

@Service
class ScmTokenService @Autowired constructor(
    private val dslContext: DSLContext,
    private val scmTokenDao: RepositoryScmTokenDao,
    private val client: Client,
    private val authProjectApi: AuthProjectApi,
    private val repoAuthServiceCode: RepoAuthServiceCode,
    private val scmTokenApiService: ScmTokenApiService,
    private val redisOperation: RedisOperation
) {
    @Value("\${aes.git:#{null}}")
    private val aesKey = ""

    fun getAccessToken(
        userId: String,
        scmCode: String,
        appType: TokenAppTypeEnum = TokenAppTypeEnum.OAUTH2
    ): GitToken? {
        val scmTokenRecord = getToken(
            userId = userId,
            appType = appType.name,
            scmCode = scmCode
        ) ?: return null
        val token = BkCryptoUtil.decryptSm4OrAes(aesKey, scmTokenRecord.accessToken)
        // 判断是否过期
        val finalToken = if (isTokenExpire(scmTokenRecord.updateTime.timestamp(), scmTokenRecord.expiresIn)) {
            tryRefreshToken(scmCode, userId, appType)?.accessToken ?: token
        } else {
            token
        }
        return GitToken(
            accessToken = finalToken,
            tokenType = scmTokenRecord.appType,
            operator = scmTokenRecord.operator,
            oauthUserId = userId
        )
    }

    fun checkAndGetAccessToken(
        projectId: String,
        buildId: String,
        userId: String,
        scmCode: String
    ): GitToken? {
        logger.info("buildId: $buildId, userId: $userId")
        val buildInfo = client.get(ServiceBuildResource::class)
                .serviceBasic(
                    projectId = projectId,
                    buildId = buildId
                ).let {
                    if (it.isNotOk()) {
                        throw RemoteServiceException(
                            "Failed to get the basic information based on the buildId: $buildId"
                        )
                    }
                    it
                }
        val accessToken = getAccessToken(userId, scmCode) ?: return null
        val operator = (accessToken.operator ?: "").ifBlank { userId }
        val buildBasicInfo = buildInfo.data ?: throw RemoteServiceException(
            "Failed to get the basic information based on the buildId: $buildId"
        )
        val projectUserCheck = authProjectApi.checkProjectUser(
            user = operator,
            serviceCode = repoAuthServiceCode,
            projectCode = buildBasicInfo.projectId
        )
        if (!projectUserCheck) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.USER_NEED_PROJECT_X_PERMISSION,
                params = arrayOf(operator, buildBasicInfo.projectId)
            )
        }
        return accessToken
    }

    fun getRedirectUrl(scmCode: String, userId: String, redirectUrl: String): String {
        val oauthState = Oauth2State(
            userId = userId,
            redirectUrl = redirectUrl,
            oauthUserId = null
        )
        return scmTokenApiService.authorizationUrl(
            scmCode = scmCode,
            state = URLEncoder.encode(JsonUtil.toJson(oauthState, false), "UTF-8")
        )
    }

    /**
     * token是否过期
     * @param updateTime token的最后更新时间(秒)
     * @param expiresIn token的过期时间(秒)
     */
    fun isTokenExpire(updateTime: Long?, expiresIn: Long): Boolean {
        // 提前半个小时刷新token
        return ((updateTime ?: 0) + expiresIn - 1800) * 1000 <= System.currentTimeMillis()
    }

    fun tryRefreshToken(
        scmCode: String,
        userId: String,
        appType: TokenAppTypeEnum
    ): RepositoryScmToken? {
        val lock = RedisLock(
            redisOperation,
            "OAUTH_REFRESH_TOKEN_${scmCode}_${userId}_${appType.name}",
            60L
        )
        // 防止并发刷新token
        return lock.use {
            lock.lock()
            val record = getToken(
                userId = userId,
                appType = appType.name,
                scmCode = scmCode
            ) ?: return null
            // 二次校验是否已过期
            val expired = isTokenExpire(record.updateTime.timestamp(), record.expiresIn)
            if (expired) {
                refreshToken(scmCode, userId, record)?.let {
                    RepositoryScmToken(
                        userId = userId,
                        scmCode = scmCode,
                        appType = appType.name,
                        accessToken = it.accessToken,
                        refreshToken = it.refreshToken,
                        expiresIn = it.expiresIn,
                        createTime = LocalDateTime.now().timestamp(), // token的创建时间
                        operator = record.operator
                    )
                }
            } else {
                RepositoryScmToken(
                    userId = userId,
                    scmCode = scmCode,
                    appType = appType.name,
                    accessToken = BkCryptoUtil.decryptSm4OrAes(aesKey, record.accessToken),
                    refreshToken = BkCryptoUtil.decryptSm4OrAes(aesKey, record.refreshToken),
                    expiresIn = record.expiresIn,
                    createTime = record.updateTime.timestamp(), // token最后刷新时间
                    operator = record.operator
                )
            }
        }
    }

    private fun refreshToken(
        scmCode: String,
        userId: String,
        scmTokenRecord: TRepositoryScmTokenRecord
    ): Oauth2AccessToken? {
        logger.info("[$scmCode|$userId]token expired, attempting refresh")
        val accessToken = try {
            scmTokenApiService.refresh(
                scmCode = scmCode,
                refreshToken = BkCryptoUtil.decryptSm4OrAes(aesKey, scmTokenRecord.refreshToken)
            )
        } catch (ignored: Exception) {
            logger.warn("failed to refresh token", ignored)
            null
        }
        accessToken?.let {
            scmTokenDao.saveAccessToken(
                dslContext = dslContext,
                scmToken = RepositoryScmToken(
                    userId = scmTokenRecord.userId,
                    scmCode = scmTokenRecord.scmCode,
                    appType = scmTokenRecord.appType,
                    accessToken = BkCryptoUtil.encryptSm4ButAes(aesKey, it.accessToken),
                    refreshToken = BkCryptoUtil.encryptSm4ButAes(aesKey, it.refreshToken),
                    expiresIn = it.expiresIn,
                    operator = scmTokenRecord.operator
                )
            )
        }
        return accessToken
    }

    fun getToken(
        userId: String,
        scmCode: String,
        appType: String
    ): TRepositoryScmTokenRecord? {
        return scmTokenDao.getToken(
            dslContext = dslContext,
            userId = userId,
            appType = appType,
            scmCode = scmCode
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(ScmTokenService::class.java)
    }
}
