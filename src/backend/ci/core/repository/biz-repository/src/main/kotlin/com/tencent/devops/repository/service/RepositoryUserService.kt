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

package com.tencent.devops.repository.service

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.repository.dao.RepositoryCodeGitDao
import com.tencent.devops.repository.dao.RepositoryCodeGitLabDao
import com.tencent.devops.repository.dao.RepositoryCodeSvnDao
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.dao.RepositoryGithubDao
import com.tencent.devops.repository.pojo.UpdateRepositoryInfoRequest
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class RepositoryUserService @Autowired constructor(
    private val repositoryDao: RepositoryDao,
    private val repositoryCodeSvnDao: RepositoryCodeSvnDao,
    private val repositoryCodeGitDao: RepositoryCodeGitDao,
    private val repositoryCodeGitLabDao: RepositoryCodeGitLabDao,
    private val repositoryGithubDao: RepositoryGithubDao,
    private val dslContext: DSLContext
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryUserService::class.java)
    }

    /**
     * 更改代码库的用户信息
     * @param userId 用户ID
     * @param projectCode 项目代码
     * @param repositoryHashId 代码库HashId
     */
    fun updateRepositoryUserInfo(
        userId: String,
        projectCode: String,
        repositoryHashId: String
    ): Result<Boolean> {
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        val repositoryRecord = repositoryDao.get(dslContext, repositoryId)
        when (repositoryRecord.type) {
            ScmType.CODE_SVN.name -> {
                repositoryCodeSvnDao.updateRepositoryInfo(dslContext, repositoryId, UpdateRepositoryInfoRequest(userId))
            }
            ScmType.CODE_GIT.name -> {
                repositoryCodeGitDao.updateRepositoryInfo(dslContext, repositoryId, UpdateRepositoryInfoRequest(userId))
            }
            ScmType.CODE_TGIT.name -> {
                repositoryCodeGitDao.updateRepositoryInfo(dslContext, repositoryId, UpdateRepositoryInfoRequest(userId))
            }
            ScmType.CODE_GITLAB.name -> {
                repositoryCodeGitLabDao.updateRepositoryInfo(
                    dslContext = dslContext,
                    repositoryId = repositoryId,
                    updateRepositoryInfoRequest = UpdateRepositoryInfoRequest(userId)
                )
            }
            ScmType.GITHUB.name -> {
                repositoryGithubDao.updateRepositoryInfo(dslContext, repositoryId, UpdateRepositoryInfoRequest(userId))
            }
            else -> {
            }
        }
        return Result(true)
    }
}
