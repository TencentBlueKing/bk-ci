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
 *
 */

package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.api.UserRepositoryPacResource
import com.tencent.devops.repository.service.RepositoryPacService
import com.tencent.devops.scm.config.GitConfig
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserRepositoryPacResourceImpl @Autowired constructor(
    private val repositoryPacService: RepositoryPacService,
    private val gitConfig: GitConfig
) : UserRepositoryPacResource {

    override fun getPacProjectId(
        userId: String,
        repoUrl: String,
        repositoryType: ScmType
    ): Result<String?> {
        return Result(
            repositoryPacService.getPacProjectId(
                userId = userId,
                repoUrl = repoUrl,
                repositoryType = repositoryType
            )
        )
    }

    override fun enablePac(userId: String, projectId: String, repositoryHashId: String): Result<Boolean> {
        repositoryPacService.enablePac(
            userId = userId,
            projectId = projectId,
            repositoryHashId = repositoryHashId
        )
        return Result(true)
    }

    override fun getYamlSyncStatus(userId: String, projectId: String, repositoryHashId: String): Result<String?> {
        return Result(
            repositoryPacService.getYamlSyncStatus(
                projectId = projectId,
                repositoryHashId = repositoryHashId
            )
        )
    }

    override fun retry(userId: String, projectId: String, repositoryHashId: String): Result<Boolean> {
        repositoryPacService.retry(
            userId = userId,
            projectId = projectId,
            repositoryHashId = repositoryHashId
        )
        return Result(true)
    }

    override fun disablePac(
        userId: String,
        projectId: String,
        repositoryHashId: String
    ): Result<Boolean> {
        repositoryPacService.disablePac(
            userId = userId,
            projectId = projectId,
            repositoryHashId = repositoryHashId
        )
        return Result(true)
    }

    override fun checkCiDirExists(
        userId: String,
        projectId: String,
        repositoryHashId: String
    ): Result<Boolean> {
        // 禁用PAC，默认检查.ci文件夹是否存在
        return Result(
            repositoryPacService.checkCiDirExists(
                userId = userId,
                projectId = projectId,
                repositoryHashId = repositoryHashId
            )
        )
    }

    override fun getCiSubDir(userId: String, projectId: String, repositoryHashId: String): Result<List<String>> {
        return Result(
            repositoryPacService.getCiSubDir(
                userId = userId,
                projectId = projectId,
                repositoryHashId = repositoryHashId
            )
        )
    }

    override fun supportScmType(): Result<List<IdValue>> {
        // TODO 源码管理需要优化
        return if (gitConfig.clientId.isBlank()) {
            return Result(emptyList())
        } else {
            Result(listOf(ScmType.CODE_GIT).map {
                IdValue(
                    id = it.name,
                    value = I18nUtil.getCodeLanMessage(
                        messageCode = "TRIGGER_TYPE_${it.name}",
                        defaultMessage = it.name
                    )
                )
            })
        }
    }
}
