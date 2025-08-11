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
import com.tencent.devops.common.security.util.BkCryptoUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.dao.RepositoryScmTokenDao
import com.tencent.devops.repository.pojo.Oauth2State
import com.tencent.devops.repository.pojo.enums.TokenAppTypeEnum
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.repository.pojo.oauth.RepositoryScmToken
import com.tencent.devops.repository.service.hub.ScmTokenApiService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder

@Service
class ScmTokenService @Autowired constructor(
    private val dslContext: DSLContext,
    private val scmTokenDao: RepositoryScmTokenDao,
    private val client: Client,
    private val authProjectApi: AuthProjectApi,
    private val repoAuthServiceCode: RepoAuthServiceCode,
    private val scmTokenApiService: ScmTokenApiService
) {
    @Value("\${aes.git:#{null}}")
    private val aesKey = ""

    fun getAccessToken(
        userId: String,
        scmCode: String,
        appType: TokenAppTypeEnum = TokenAppTypeEnum.OAUTH2
    ): GitToken? {
        val scmTokenRecord = scmTokenDao.getToken(
            dslContext = dslContext,
            userId = userId,
            appType = appType.name,
            scmCode = scmCode
        ) ?: return null
        // 判断是否过期
        val token = if (isTokenExpire(scmTokenRecord.updateTime.timestamp(), scmTokenRecord.expiresIn)) {
            logger.info("[$scmCode|$userId]token expired, attempting refresh")
            val accessToken = scmTokenApiService.refresh(
                scmCode = scmCode,
                refreshToken = BkCryptoUtil.decryptSm4OrAes(aesKey, scmTokenRecord.refreshToken)
            )
            scmTokenDao.saveAccessToken(
                dslContext = dslContext,
                scmToken = RepositoryScmToken(
                    userId = scmTokenRecord.userId,
                    scmCode = scmTokenRecord.scmCode,
                    appType = scmTokenRecord.appType,
                    accessToken = BkCryptoUtil.decryptSm4OrAes(aesKey, accessToken.accessToken),
                    refreshToken = BkCryptoUtil.decryptSm4OrAes(aesKey, accessToken.refreshToken),
                    expiresIn = accessToken.expiresIn,
                    operator = scmTokenRecord.operator
                )
            )
            accessToken.accessToken
        } else {
            BkCryptoUtil.decryptSm4OrAes(aesKey, scmTokenRecord.accessToken)
        }
        return GitToken(
            accessToken = token,
            tokenType = scmTokenRecord.appType,
            operator = scmTokenRecord.operator,
            userId = userId
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
            username = null
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
    private fun isTokenExpire(updateTime: Long?, expiresIn: Long): Boolean {
        // 提前半个小时刷新token
        return ((updateTime ?: 0) + expiresIn - 1800) * 1000 <= System.currentTimeMillis()
    }

    companion object {
        val logger = LoggerFactory.getLogger(ScmTokenService::class.java)
    }
}
