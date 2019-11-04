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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
import com.tencent.devops.process.pojo.BuildBasicInfo
import com.tencent.devops.repository.pojo.Project
import com.tencent.devops.repository.pojo.enums.*
import com.tencent.devops.repository.pojo.git.GitProjectInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.pojo.GitRepositoryResp
import com.tencent.devops.scm.pojo.TokenCheckResult
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.*

interface RepostioryScmService {

    fun getProject(
            accessToken: String,
            userId: String
    ): List<Project>

    fun getAuthUrl(
            authParamJsonStr: String
    ): String

    fun getToken(
            userId: String,
            code: String
    ): GitToken

    @ApiOperation("��ȡת����ַ")
    fun getRedirectUrl(
            redirectUrlType: String
    ): String

    @ApiOperation("ˢ���û���token")
    fun refreshToken(
            userId: String,
            accessToken: GitToken
    ): GitToken

    @ApiOperation("��ȡ�ļ�����")
    fun getSvnFileContent(
            url: String,
            userId: String,
            svnType: String,
            filePath: String,
            reversion: Long,
            credential1: String,
            credential2: String? = null
    ): String

    @ApiOperation("��ȡgit�ļ�����")
    fun getGitFileContent(
            repoName: String,
            filePath: String,
            authType: RepoAuthType?,
            token: String,
            ref: String
    ): String

    @ApiOperation("��ȡgitlab�ļ�����")
    fun getGitlabFileContent(
            repoUrl: String,
            repoName: String,
            filePath: String,
            ref: String,
            accessToken: String
    ): String

    @ApiOperation("lock svn")
    fun unlock(
            projectName: String,
            url: String,
            type: ScmType,
            region: CodeSvnRegion?,
            userName: String
    ): Boolean

    @ApiOperation("lock svn")
    fun lock(
            projectName: String,
            url: String,
            type: ScmType,
            region: CodeSvnRegion?,
            userName: String
    ): Boolean

    @ApiOperation("����ĿǨ�Ƶ�ָ����Ŀ����")
    fun moveProjectToGroup(
            token: String,
            groupCode: String,
            repositoryName: String,
            tokenType: TokenTypeEnum
    ): Result<GitProjectInfo?>

    @ApiOperation("����git�������Ϣ")
    fun updateGitCodeRepository(
            token: String,
            projectName: String,
            updateGitProjectInfo: UpdateGitProjectInfo,
            tokenType: TokenTypeEnum
    ): Result<Boolean>

    @ApiOperation("����git�����")
    fun createGitCodeRepository(
            userId: String,
            token: String,
            repositoryName: String,
            sampleProjectPath: String?,
            namespaceId: Int?,
            visibilityLevel: VisibilityLevelEnum?,
            tokenType: TokenTypeEnum
    ): Result<GitRepositoryResp?>

    @ApiOperation("Ϊ��Ŀ��Ա��������Ȩ��")
    fun addGitProjectMember(
            userIdList: List<String>,
            repositorySpaceName: String,
            gitAccessLevel: GitAccessLevelEnum,
            token: String,
            tokenType: TokenTypeEnum
    ): Result<Boolean>

    @ApiOperation("ɾ����Ŀ��Ա�Ĵ����Ȩ��")
    fun deleteGitProjectMember(
            userIdList: List<String>,
            repositorySpaceName: String,
            token: String,
            tokenType: TokenTypeEnum
    ): Result<Boolean>

    @ApiOperation("Check if the svn private key and passphrase legal")
    fun checkPrivateKeyAndToken(
            projectName: String,
            url: String,
            type: ScmType,
            privateKey: String?,
            passPhrase: String?,
            token: String?,
            region: CodeSvnRegion?,
            userName: String
    ): Result<TokenCheckResult>

    @ApiOperation("Check if the svn private key and passphrase legal")
    fun checkUsernameAndPassword(
            projectName: String,
            @ApiParam("�ֿ��ַ", required = true)
            url: String,
            type: ScmType,
            username: String,
            password: String,
            token: String,
            region: CodeSvnRegion?,
            repoUsername: String
    ): Result<TokenCheckResult>
}