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
import com.tencent.devops.repository.dao.TGitTokenDao
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.repository.pojo.oauth.OauthTokenInfo
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 内部工蜂oauth存储管理
 */
@Service
class CodeTGitOauth2TokenStoreService @Autowired constructor(
    private val dslContext: DSLContext,
    private val tGitTokenDao: TGitTokenDao
) : IOauth2TokenStoreService {

    @Value("\${aes.git:#{null}}")
    private val aesKey: String = ""

    // 兼容历史数据,直接用scmType的值代替scmCode
    override fun support(scmCode: String): Boolean {
        return scmCode == ScmType.CODE_TGIT.name
    }

    override fun get(userId: String, scmCode: String): OauthTokenInfo? {
        return tGitTokenDao.getAccessToken(dslContext, userId)?.let {
            OauthTokenInfo(
                accessToken = BkCryptoUtil.decryptSm4OrAes(aesKey, it.accessToken),
                tokenType = it.tokenType,
                expiresIn = it.expiresIn,
                refreshToken = BkCryptoUtil.decryptSm4OrAes(aesKey, it.refreshToken),
                createTime = it.createTime.timestampmilli(),
                userId = it.oauthUserId,
                operator = userId
            )
        }
    }

    override fun store(scmCode: String, oauthTokenInfo: OauthTokenInfo) {
        with(oauthTokenInfo) {
            val gitToken = GitToken(
                accessToken = BkCryptoUtil.encryptSm4ButAes(aesKey, accessToken),
                refreshToken = refreshToken?.let { BkCryptoUtil.encryptSm4ButAes(aesKey, it) } ?: "",
                tokenType = tokenType,
                expiresIn = expiresIn ?: 0L,
                operator = operator
            )
            tGitTokenDao.saveAccessToken(
                dslContext,
                userId = operator ?: userId,
                oauthUserId = userId,
                token = gitToken
            )
        }
    }

    override fun delete(userId: String, scmCode: String) {
        tGitTokenDao.deleteToken(dslContext = dslContext, userId = userId)
    }
}
