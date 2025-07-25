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
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.scm.api.enums.ScmProviderType
import com.tencent.devops.scm.api.pojo.repository.ScmProviderRepository
import com.tencent.devops.scm.api.pojo.repository.git.GitScmProviderRepository
import com.tencent.devops.scm.api.pojo.repository.svn.SvnScmProviderRepository
import com.tencent.devops.scm.spring.properties.ScmProviderProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ScmProviderRepositoryFactory @Autowired constructor(
    private val providerAuthFactory: ScmProviderAuthFactory
) {

    fun create(
        properties: ScmProviderProperties,
        authRepository: AuthRepository
    ): ScmProviderRepository {
        val auth = providerAuthFactory.createScmAuth(
            authRepository = authRepository
        )
        return when (properties.providerType) {
            ScmProviderType.GIT.name -> {
                GitScmProviderRepository().withUrl(authRepository.url).withAuth(auth)
            }

            ScmProviderType.SVN.name -> {
                SvnScmProviderRepository()
                    .withUserName(authRepository.userName)
                    .withUrl(authRepository.url)
                    .withAuth(auth)
            }

            else ->
                throw ErrorCodeException(
                    errorCode = RepositoryMessageCode.ERROR_NOT_SUPPORT_SCM_PROVIDER_TYPE,
                    params = arrayOf(properties.providerType)
                )
        }
    }
}
