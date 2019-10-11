package com.tencent.devops.repository.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.Project
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.oauth.GitToken
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.QueryParam

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

    @ApiOperation("获取转发地址")
    fun getRedirectUrl(
            redirectUrlType: String
    ): String

    @ApiOperation("刷新用户的token")
    fun refreshToken(
            userId: String,
            accessToken: GitToken
    ): GitToken

    @ApiOperation("获取文件内容")
    fun getSvnFileContent(
            url: String,
            userId: String,
            svnType: String,
            filePath: String,
            reversion: Long,
            credential1: String,
            credential2: String? = null
    ): String

    @ApiOperation("获取git文件内容")
    fun getGitFileContent(
            repoName: String,
            filePath: String,
            authType: RepoAuthType?,
            token: String,
            ref: String
    ): String

    @ApiOperation("获取gitlab文件内容")
    fun getGitlabFileContent(
            repoUrl: String,
            repoName: String,
            filePath: String,
            ref: String,
            accessToken: String
    ): String

}