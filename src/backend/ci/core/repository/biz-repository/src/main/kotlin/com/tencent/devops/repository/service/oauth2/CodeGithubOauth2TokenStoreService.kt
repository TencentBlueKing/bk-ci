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

package com.tencent.devops.repository.service.oauth2

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.security.util.BkCryptoUtil
import com.tencent.devops.repository.dao.GithubTokenDao
import com.tencent.devops.repository.pojo.oauth.GithubTokenType
import com.tencent.devops.repository.pojo.oauth.OauthTokenInfo
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * github oauth2 token存储管理
 */
@Service
class CodeGithubOauth2TokenStoreService @Autowired constructor(
    private val dslContext: DSLContext,
    private val githubTokenDao: GithubTokenDao
) : IOauth2TokenStoreService {

    @Value("\${aes.github:#{null}}")
    private val aesKey = ""

    // 兼容历史数据,直接用scmType的值代替scmCode
    override fun support(scmCode: String): Boolean {
        return scmCode == ScmType.GITHUB.name
    }

    override fun get(userId: String, scmCode: String): OauthTokenInfo? {
        return githubTokenDao.getOrNull(
            dslContext = dslContext,
            userId = userId,
            githubTokenType = GithubTokenType.GITHUB_APP
        )?.let {
            OauthTokenInfo(
                accessToken = BkCryptoUtil.decryptSm4OrAes(aesKey, it.accessToken),
                tokenType = it.tokenType,
                expiresIn = null,
                refreshToken = null,
                createTime = it.createTime.timestampmilli(),
                userId = it.userId,
                operator = it.operator
            )
        }
    }

    override fun store(scmCode: String, oauthTokenInfo: OauthTokenInfo) {
        with(oauthTokenInfo) {
            val encryptedAccessToken = BkCryptoUtil.encryptSm4ButAes(aesKey, accessToken)
            val record = githubTokenDao.getOrNull(
                dslContext = dslContext,
                userId = userId,
                githubTokenType = GithubTokenType.GITHUB_APP
            )
            if (record == null) {
                githubTokenDao.create(
                    dslContext,
                    userId = userId,
                    accessToken = encryptedAccessToken,
                    tokenType = tokenType,
                    scope = "",
                    githubTokenType = GithubTokenType.GITHUB_APP,
                    operator = operator ?: userId
                )
            } else {
                githubTokenDao.update(
                    dslContext,
                    userId = userId,
                    accessToken = encryptedAccessToken,
                    tokenType = tokenType,
                    scope = "",
                    githubTokenType = GithubTokenType.GITHUB_APP,
                    operator = operator ?: userId
                )
            }
        }
    }

    override fun delete(userId: String, scmCode: String) {
        githubTokenDao.delete(dslContext = dslContext, userId = userId)
    }
}
