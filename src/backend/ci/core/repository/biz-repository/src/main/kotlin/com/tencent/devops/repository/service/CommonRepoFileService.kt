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

package com.tencent.devops.repository.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.dao.GitTokenDao
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.service.scm.IGitService
import com.tencent.devops.scm.pojo.GitMember
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class CommonRepoFileService @Autowired constructor(
    private val gitService: IGitService,
    private val gitTokenDao: GitTokenDao,
    private val dslContext: DSLContext
) {

    @Value("\${aes.git:#{null}}")
    private val aesKey: String = ""

    fun getGitFileContent(
        repoUrl: String,
        filePath: String,
        ref: String?,
        token: String,
        authType: RepoAuthType?,
        subModule: String?
    ): String {
        val projectName = if (subModule.isNullOrBlank()) GitUtils.getProjectName(repoUrl) else subModule
        return gitService.getGitFileContent(
            repoUrl = repoUrl,
            repoName = projectName!!,
            filePath = filePath.removePrefix("/"),
            authType = authType,
            token = token,
            ref = ref ?: "master"
        )
    }

    fun getGitFileContentOauth(userId: String, repoName: String, filePath: String, ref: String?): Result<String> {
        val token = AESUtil.decrypt(
            key = aesKey,
            content = gitTokenDao.getAccessToken(dslContext, userId)?.accessToken
                ?: return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.OAUTH_TOKEN_IS_INVALID,
                    language = I18nUtil.getLanguage(userId)
                )
        )
        return Result(
            gitService.getGitFileContent(
                repoUrl = null,
                repoName = repoName,
                filePath = filePath.removePrefix("/"),
                authType = RepoAuthType.OAUTH,
                token = token,
                ref = ref ?: "master"
            )
        )
    }

    fun getGitProjectMembers(repoUrl: String, userId: String): Result<List<GitMember>> {
        val token = AESUtil.decrypt(
            key = aesKey,
            content = gitTokenDao.getAccessToken(dslContext, userId)?.accessToken
                ?: return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.OAUTH_TOKEN_IS_INVALID,
                    language = I18nUtil.getLanguage(userId)
                )
        )
        return Result(
            data = gitService.getRepoMembers(
                accessToken = token,
                userId = userId,
                repoName = GitUtils.getProjectName(repoUrl)
            )
        )
    }

    fun getGitProjectAllMembers(repoUrl: String, userId: String): Result<List<GitMember>> {
        val token = AESUtil.decrypt(
            key = aesKey,
            content = gitTokenDao.getAccessToken(dslContext, userId)?.accessToken
                ?: return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.OAUTH_TOKEN_IS_INVALID,
                    language = I18nUtil.getLanguage(userId)
                )
        )
        return Result(
            data = gitService.getRepoAllMembers(
                accessToken = token,
                userId = userId,
                repoName = GitUtils.getProjectName(repoUrl)
            )
        )
    }

    fun isProjectMember(repoUrl: String, userId: String): Result<Boolean> {
        val token = AESUtil.decrypt(
            key = aesKey,
            content = gitTokenDao.getAccessToken(dslContext, userId)?.accessToken
                ?: return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.OAUTH_TOKEN_IS_INVALID,
                    language = I18nUtil.getLanguage(userId)
                )
        )
        val projectUser = gitService.getProjectMembersAll(
            gitProjectId = GitUtils.getProjectName(repoUrl),
            page = 1,
            pageSize = 10,
            search = userId,
            tokenType = TokenTypeEnum.OAUTH,
            token = token
        ).data?.map {
            it.username
        } ?: emptyList()
        return if (projectUser.isNotEmpty() && projectUser.contains(userId)) {
            Result(true)
        } else {
            Result(false)
        }
    }
}
