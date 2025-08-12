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

package com.tencent.devops.repository.service.oauth2

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.security.util.BkCryptoUtil
import com.tencent.devops.model.repository.tables.records.TRepositoryScmTokenRecord
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.dao.RepositoryScmTokenDao
import com.tencent.devops.repository.pojo.enums.TokenAppTypeEnum
import com.tencent.devops.repository.pojo.oauth.OauthTokenInfo
import com.tencent.devops.repository.pojo.oauth.RepositoryScmToken
import com.tencent.devops.repository.service.scm.ScmTokenService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

/**
 * 通用的oauth2 token存储服务,除了codeGit、codeGithub和CODE_TGIT,都可以使用这个处理
 */
@Service
class DefaultOauth2TokenStoreService(
    private val dslContext: DSLContext,
    private val repositoryScmTokenDao: RepositoryScmTokenDao,
    @Lazy
    private val scmTokenService: ScmTokenService
) : IOauth2TokenStoreService {

    @Value("\${aes.git:#{null}}")
    private val aesKey: String = ""

    override fun support(scmCode: String): Boolean {
        return scmCode != ScmType.GITHUB.name &&
                scmCode != ScmType.CODE_GIT.name &&
                scmCode != ScmType.CODE_TGIT.name
    }

    override fun get(userId: String, scmCode: String): OauthTokenInfo? {
        val scmTokenRecord = repositoryScmTokenDao.getToken(
            dslContext = dslContext,
            userId = userId,
            scmCode = scmCode,
            appType = TokenAppTypeEnum.OAUTH2.name
        ) ?: repositoryScmTokenDao.getTokenByOperator(
            dslContext = dslContext,
            operator = userId,
            scmCode = scmCode,
            appType = TokenAppTypeEnum.OAUTH2.name
        )
        return scmTokenRecord?.let {
            checkExpire(it, userId)
        }
    }

    override fun store(scmCode: String, oauthTokenInfo: OauthTokenInfo) {
        with(oauthTokenInfo) {
            val scmToken = RepositoryScmToken(
                userId = userId,
                scmCode = scmCode,
                appType = TokenAppTypeEnum.OAUTH2.name,
                accessToken = BkCryptoUtil.encryptSm4ButAes(aesKey, accessToken),
                refreshToken = refreshToken?.let { BkCryptoUtil.encryptSm4ButAes(aesKey, it) } ?: "",
                expiresIn = expiresIn ?: 0L,
                operator = operator ?: userId
            )
            repositoryScmTokenDao.saveAccessToken(
                dslContext = dslContext,
                scmToken = scmToken
            )
        }
    }

    override fun delete(userId: String, scmCode: String, username: String) {
        get(username, scmCode)?.let {
            // 非OAUTH授权代持人不得删除
            if (it.operator != userId) {
                throw ErrorCodeException(
                    errorCode = RepositoryMessageCode.ERROR_NOT_OAUTH_PROXY_FORBIDDEN_DELETE
                )
            }
        }
        repositoryScmTokenDao.delete(
            dslContext = dslContext,
            userId = username,
            scmCode = scmCode
        )
    }

    override fun list(userId: String, scmCode: String): List<OauthTokenInfo> {
        return repositoryScmTokenDao.list(
            dslContext = dslContext,
            scmCode = scmCode,
            operator = userId
        ).map {
            checkExpire(it, userId)
        }
    }

    /**
     * 检查是否过期，过期则尝试续期
     */
    private fun checkExpire(
        it: TRepositoryScmTokenRecord,
        userId: String
    ): OauthTokenInfo {
        val oauthTokenInfo = OauthTokenInfo(
            accessToken = BkCryptoUtil.decryptSm4OrAes(aesKey, it.accessToken),
            tokenType = "",
            expiresIn = it.expiresIn,
            refreshToken = BkCryptoUtil.decryptSm4OrAes(aesKey, it.refreshToken),
            createTime = it.createTime.timestampmilli(),
            userId = it.userId,
            operator = it.operator ?: userId,
            updateTime = it.updateTime.timestampmilli()
        )
        return if (scmTokenService.isTokenExpire(it.updateTime.timestamp(), it.expiresIn)) {
            val refreshToken = scmTokenService.tryRefreshToken(
                scmCode = it.scmCode,
                userId = it.userId,
                appType = TokenAppTypeEnum.OAUTH2
            )
            // 刷新成功，返回新值
            refreshToken?.let {
                oauthTokenInfo.copy(
                    accessToken = it.accessToken,
                    expiresIn = it.expiresIn,
                    refreshToken = it.refreshToken,
                    createTime = it.createTime
                )
            } ?: oauthTokenInfo
        } else {
            oauthTokenInfo
        }
    }
}
