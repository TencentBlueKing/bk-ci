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

package com.tencent.devops.repository.service.hub

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.constant.RepositoryMessageCode.NOT_AUTHORIZED_BY_OAUTH
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.repository.pojo.credential.CredentialIdAuthCred
import com.tencent.devops.repository.pojo.credential.UserOauthTokenAuthCred
import com.tencent.devops.repository.service.RepoCredentialService
import com.tencent.devops.repository.service.oauth2.Oauth2TokenStoreManager
import com.tencent.devops.scm.api.pojo.auth.AccessTokenScmAuth
import com.tencent.devops.scm.api.pojo.auth.IScmAuth
import com.tencent.devops.scm.api.pojo.auth.SshPrivateKeyScmAuth
import com.tencent.devops.scm.api.pojo.auth.TokenSshPrivateKeyScmAuth
import com.tencent.devops.scm.api.pojo.auth.TokenUserPassScmAuth
import com.tencent.devops.scm.api.pojo.auth.UserPassScmAuth
import com.tencent.devops.ticket.pojo.item.AccessTokenCredentialItem
import com.tencent.devops.ticket.pojo.item.OauthTokenCredentialItem
import com.tencent.devops.ticket.pojo.item.SshPrivateKeyCredentialItem
import com.tencent.devops.ticket.pojo.item.TokenSshPrivateKeyCredentialItem
import com.tencent.devops.ticket.pojo.item.TokenUserPassCredentialItem
import com.tencent.devops.ticket.pojo.item.UserPassCredentialItem
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ScmProviderAuthFactory @Autowired constructor(
    private val oauth2TokenStoreManager: Oauth2TokenStoreManager,
    private val credentialService: RepoCredentialService
) {

    fun createScmAuth(authRepository: AuthRepository): IScmAuth {
        return when (val auth = authRepository.auth) {
            is UserOauthTokenAuthCred -> {
                createAuthFromOauthToken(userId = auth.userId, scmCode = authRepository.scmCode)
            }

            is CredentialIdAuthCred -> {
                createAuthFromCredential(projectId = auth.projectId, credentialId = auth.credentialId)
            }

            else ->
                throw ErrorCodeException(errorCode = RepositoryMessageCode.ERROR_NOT_SUPPORT_REPOSITORY_AUTH)
        }
    }

    fun createAuthFromOauthToken(userId: String, scmCode: String): IScmAuth {
        val oauth2AccessToken = oauth2TokenStoreManager.get(
            userId = userId, scmCode = scmCode
        ) ?: throw ErrorCodeException(errorCode = NOT_AUTHORIZED_BY_OAUTH, params = arrayOf(userId))
        return AccessTokenScmAuth(oauth2AccessToken.accessToken)
    }

    fun createAuthFromCredential(projectId: String, credentialId: String): IScmAuth {
        val credentialItem = credentialService.getCredentialItem(projectId = projectId, credentialId = credentialId)
        return when (credentialItem) {
            is UserPassCredentialItem ->
                UserPassScmAuth(credentialItem.username, credentialItem.password)

            is TokenUserPassCredentialItem ->
                TokenUserPassScmAuth(credentialItem.token, credentialItem.username, credentialItem.password)

            is OauthTokenCredentialItem ->
                AccessTokenScmAuth(credentialItem.oauthToken)

            is AccessTokenCredentialItem ->
                AccessTokenScmAuth(credentialItem.accessToken)

            is SshPrivateKeyCredentialItem ->
                SshPrivateKeyScmAuth(credentialItem.privateKey, credentialItem.passphrase)

            is TokenSshPrivateKeyCredentialItem ->
                TokenSshPrivateKeyScmAuth(credentialItem.token, credentialItem.privateKey, credentialItem.passphrase)

            else ->
                throw ErrorCodeException(
                    errorCode = RepositoryMessageCode.ERROR_NOT_SUPPORT_CREDENTIAL_TYPE,
                    params = arrayOf(credentialItem.getCredentialType().name)
                )
        }
    }
}
