/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.repository.api.scm

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.Project
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitProjectInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.repository.pojo.scm.GitRepositoryResp
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_SCM_GIT"], description = "Service Code GIT resource")
@Path("/service/git/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGitResource {

    @ApiOperation("获取用户所有git项目")
    @GET
    @Path("/getProject")
    fun getProject(
            @ApiParam("accessToken", required = true)
            @QueryParam("accessToken")
            accessToken: String,
            @ApiParam("用户id", required = true)
            @QueryParam("userId")
            userId: String
    ): Result<List<Project>>

    @ApiOperation("刷新用户的token")
    @PUT
    @Path("/refreshToken")
    fun refreshToken(
            @ApiParam("用户id", required = true)
            @QueryParam("userId")
            userId: String,
            @ApiParam("accessToken", required = true)
            accessToken: GitToken
    ): Result<GitToken>

    @ApiOperation("获取授权路径")
    @GET
    @Path("/getAuthUrl")
    fun getAuthUrl(
            @ApiParam("参数json串", required = true)
            @QueryParam("authParamJsonStr")
            authParamJsonStr: String
    ): Result<String>

    @ApiOperation("获取用户的token")
    @GET
    @Path("/getToken")
    fun getToken(
            @ApiParam("用户id", required = true)
            @QueryParam("userId")
            userId: String,
            @ApiParam("code", required = true)
            @QueryParam("code")
            code: String
    ): Result<GitToken>

    @ApiOperation("获取转发地址")
    @GET
    @Path("/getRedirectUrl")
    fun getRedirectUrl(
            @ApiParam("重定向url类型", required = false)
            @QueryParam("redirectUrlType")
            redirectUrlType: String?
    ): Result<String>

    @ApiOperation("获取git文件内容")
    @GET
    @Path("/getGitFileContent")
    fun getGitFileContent(
            @ApiParam(value = "仓库名字")
            @QueryParam("repoName")
            repoName: String,
            @ApiParam(value = "文件路径")
            @QueryParam("filePath")
            filePath: String,
            @ApiParam(value = "认证方式")
            @QueryParam("authType")
            authType: RepoAuthType?,
            @ApiParam(value = "token")
            @QueryParam("token")
            token: String,
            @ApiParam(value = "提交id 或者 分支")
            @QueryParam("ref")
            ref: String
    ): Result<String>

    @ApiOperation("获取gitlab文件内容")
    @GET
    @Path("/getGitlabFileContent")
    fun getGitlabFileContent(
            @ApiParam(value = "仓库Url")
            @QueryParam("repoUrl")
            repoUrl: String,
            @ApiParam(value = "仓库名字")
            @QueryParam("repoName")
            repoName: String,
            @ApiParam(value = "文件路径")
            @QueryParam("filePath")
            filePath: String,
            @ApiParam(value = "提交id 或者 分支")
            @QueryParam("ref")
            ref: String,
            @ApiParam(value = "accessToken")
            @QueryParam("accessToken")
            accessToken: String
    ): Result<String>

    @ApiOperation("更新git代码库信息")
    @PUT
    @Path("/updateGitCodeRepository")
    fun updateGitCodeRepository(
            @ApiParam("token", required = true)
            @QueryParam("token")
            token: String,
            @ApiParam(value = "项目名称", required = true)
            @QueryParam("projectName")
            projectName: String,
            @ApiParam(value = "更新git项目信息", required = true)
            updateGitProjectInfo: UpdateGitProjectInfo,
            @ApiParam(value = "token类型 1：oauth 2:privateKey", required = true)
            @QueryParam("tokenType")
            tokenType: TokenTypeEnum
    ): Result<Boolean>

    @ApiOperation("把项目迁移到指定项目组下")
    @POST
    @Path("/moveProjectToGroup")
    fun moveProjectToGroup(
            @ApiParam("token", required = true)
            @QueryParam("token")
            token: String,
            @ApiParam(value = "项目组代码", required = true)
            @QueryParam("groupCode")
            groupCode: String,
            @ApiParam(value = "代码库名称", required = true)
            @QueryParam("repositoryName")
            repositoryName: String,
            @ApiParam(value = "token类型 1：oauth 2:privateKey", required = true)
            @QueryParam("tokenType")
            tokenType: TokenTypeEnum
    ): Result<GitProjectInfo?>

//    @ApiOperation("创建git代码库")
//    @POST
//    @Path("/createGitCodeRepository")
//    fun createGitCodeRepository(
//            @ApiParam("用户id", required = true)
//            @QueryParam("userId")
//            userId: String,
//            @ApiParam("token", required = true)
//            @QueryParam("token")
//            token: String,
//            @ApiParam(value = "代码库名称", required = true)
//            @QueryParam("repositoryName")
//            repositoryName: String,
//            @ApiParam("样例工程路径", required = true)
//            @QueryParam("sampleProjectPath")
//            sampleProjectPath: String,
//            @ApiParam(value = "命名空间ID", required = false)
//            @QueryParam("namespaceId")
//            namespaceId: Int?,
//            @ApiParam(value = "项目可视范围", required = false)
//            @QueryParam("visibilityLevel")
//            visibilityLevel: VisibilityLevelEnum?,
//            @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
//            @QueryParam("tokenType")
//            tokenType: TokenTypeEnum
//    ): Result<GitRepositoryResp?>
//
//    // TODO: 考虑是否放在内部版
//    @ApiOperation("为项目成员赋予代码库权限")
//    @POST
//    @Path("/addGitProjectMember")
//    fun addGitProjectMember(
//            @ApiParam("增加的用户列表", required = true)
//            @QueryParam("userIdList")
//            userIdList: List<String>,
//            @ApiParam(value = "代码库命名空间名称", required = true)
//            @QueryParam("repositorySpaceName")
//            repositorySpaceName: String,
//            @ApiParam(value = "git访问权限", required = true)
//            @QueryParam("gitAccessLevel")
//            gitAccessLevel: GitAccessLevelEnum,
//            @ApiParam("token", required = true)
//            @QueryParam("token")
//            token: String,
//            @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
//            @QueryParam("tokenType")
//            tokenType: TokenTypeEnum
//    ): Result<Boolean>
//
//    // TODO: 考虑是否放在内部版
//    @ApiOperation("删除项目成员的代码库权限")
//    @DELETE
//    @Path("/deleteGitProjectMember")
//    fun deleteGitProjectMember(
//            @ApiParam("删除的用户列表", required = true)
//            @QueryParam("userIdList")
//            userIdList: List<String>,
//            @ApiParam(value = "代码库命名空间名称", required = true)
//            @QueryParam("repositorySpaceName")
//            repositorySpaceName: String,
//            @ApiParam("token", required = true)
//            @QueryParam("token")
//            token: String,
//            @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
//            @QueryParam("tokenType")
//            tokenType: TokenTypeEnum
//    ): Result<Boolean>
}