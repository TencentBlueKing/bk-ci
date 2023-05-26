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

package com.tencent.devops.repository.service.scm

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.service.CredentialService
import com.tencent.devops.repository.service.RepositoryService
import com.tencent.devops.scm.code.p4.api.P4Api
import com.tencent.devops.scm.code.p4.api.P4FileSpec
import com.tencent.devops.scm.code.p4.api.P4ServerInfo
import org.springframework.stereotype.Service
import java.net.URLDecoder

@Service
class P4Service(
    private val repositoryService: RepositoryService,
    private val credentialService: CredentialService
) : Ip4Service {

    override fun getChangelistFiles(
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?,
        change: Int
    ): List<P4FileSpec> {
        val (repository, username, password) = getRepositoryInfo(projectId, repositoryId, repositoryType)
        return P4Api(
            p4port = repository.url,
            username = username,
            password = password
        ).getChangelistFiles(change)
    }

    override fun getShelvedFiles(
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?,
        change: Int
    ): List<P4FileSpec> {
        val (repository, username, password) = getRepositoryInfo(projectId, repositoryId, repositoryType)
        return P4Api(
            p4port = repository.url,
            username = username,
            password = password
        ).getShelvedFiles(change)
    }

    override fun getFileContent(
        p4Port: String,
        filePath: String,
        reversion: Int,
        username: String,
        password: String
    ): String {
        return P4Api(
            p4port = p4Port,
            username = username,
            password = password
        ).getFileContent(filePath = filePath, reversion = reversion)
    }

    @SuppressWarnings("ThrowsCount")
    private fun getRepositoryInfo(
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?
    ): Triple<Repository, String, String> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (repositoryId.isBlank()) {
            throw ParamBlankException("Invalid repositoryHashId")
        }
        val repository = repositoryService.serviceGet(
            projectId = projectId,
            repositoryConfig =
            RepositoryConfigUtils.buildConfig(URLDecoder.decode(repositoryId, "UTF-8"), repositoryType)
        )
        val credentials = credentialService.getCredential(
            projectId = projectId,
            repository = repository
        )
        val username = credentials[0]
        if (username.isEmpty()) {
            throw OperationException(
                message = I18nUtil.getCodeLanMessage(CommonMessageCode.USER_NAME_EMPTY)
            )
        }
        if (credentials.size < 2) {
            throw OperationException(
                message = I18nUtil.getCodeLanMessage(CommonMessageCode.PWD_EMPTY)
            )
        }
        val password = credentials[1]
        if (password.isEmpty()) {
            throw OperationException(
                message = I18nUtil.getCodeLanMessage(
                    CommonMessageCode.PWD_EMPTY
                )
            )
        }
        return Triple(repository, username, password)
    }

    override fun getServerInfo(
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?
    ): P4ServerInfo {
        val (repository, username, password) = getRepositoryInfo(projectId, repositoryId, repositoryType)
        return P4Api(
            p4port = repository.url,
            username = username,
            password = password
        ).getServerInfo()
    }
}
