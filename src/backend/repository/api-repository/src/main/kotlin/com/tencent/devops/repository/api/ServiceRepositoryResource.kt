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

package com.tencent.devops.repository.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryId
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.RepositoryInfoWithPermission
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.enums.Permission
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitProjectInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_REPOSITORY"], description = "服务-代码库资源")
@Path("/service/repositories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceRepositoryResource {

    @ApiOperation("创建git代码库")
    @POST
    @Path("/git/create/repository")
    fun createGitCodeRepository(
            @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_USER_ID)
            userId: String,
            @ApiParam("项目编码", required = true)
            @QueryParam("projectCode")
            projectCode: String,
            @ApiParam("代码库名称", required = true)
            @QueryParam("repositoryName")
            repositoryName: String,
            @ApiParam("样例工程路径", required = true)
            @QueryParam("sampleProjectPath")
            sampleProjectPath: String,
            @ApiParam(value = "命名空间ID", required = false)
            @QueryParam("namespaceId")
            namespaceId: Int?,
            @ApiParam(value = "项目可视范围", required = false)
            @QueryParam("visibilityLevel")
            visibilityLevel: VisibilityLevelEnum?,
            @ApiParam(value = "token类型 1：oauth 2:privateKey", required = true)
            @QueryParam("tokenType")
            tokenType: TokenTypeEnum
    ): Result<RepositoryInfo?>

    @ApiOperation("更新git代码库信息")
    @PUT
    @Path("/git/update/repository")
    fun updateGitCodeRepository(
            @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_USER_ID)
            userId: String,
            @ApiParam(value = "仓库id", required = true)
            @QueryParam("repoId")
            repoId: String,
            @ApiParam("代码库更新信息", required = true)
            updateGitProjectInfo: UpdateGitProjectInfo,
            @ApiParam(value = "token类型 1：oauth 2:privateKey", required = true)
            @QueryParam("tokenType")
            tokenType: TokenTypeEnum
    ): Result<Boolean>

    @ApiOperation("为项目成员赋予代码库权限")
    @POST
    @Path("/git/repository/members/add")
    fun addGitProjectMember(
            @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_USER_ID)
            userId: String,
            @ApiParam("增加的用户列表", required = true)
            @QueryParam("userIdList")
            userIdList: List<String>,
            @ApiParam(value = "仓库id", required = true)
            @QueryParam("repoId")
            repoId: String,
            @ApiParam(value = "git访问权限", required = true)
            @QueryParam("gitAccessLevel")
            gitAccessLevel: GitAccessLevelEnum,
            @ApiParam(value = "token类型 1：oauth 2:privateKey", required = true)
            @QueryParam("tokenType")
            tokenType: TokenTypeEnum
    ): Result<Boolean>

    @ApiOperation("删除项目成员的代码库权限")
    @DELETE
    @Path("/git/repository/members/delete")
    fun deleteGitProjectMember(
            @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_USER_ID)
            userId: String,
            @ApiParam("删除的用户列表", required = true)
            @QueryParam("userIdList")
            userIdList: List<String>,
            @ApiParam(value = "仓库id", required = true)
            @QueryParam("repoId")
            repoId: String,
            @ApiParam(value = "token类型 1：oauth 2:privateKey", required = true)
            @QueryParam("tokenType")
            tokenType: TokenTypeEnum
    ): Result<Boolean>

    @ApiOperation("更新代码库用户信息")
    @PUT
    @Path("/git/repository/user/info/update")
    fun updateRepositoryUserInfo(
            @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_USER_ID)
            userId: String,
            @ApiParam("项目编码", required = true)
            @QueryParam("projectCode")
            projectCode: String,
            @ApiParam("代码库HashId", required = true)
            @QueryParam("repositoryHashId")
            repositoryHashId: String
    ): Result<Boolean>

    @ApiOperation("把项目迁移到指定项目组下")
    @GET
    @Path("/git/move/repository/group")
    fun moveGitProjectToGroup(
            @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_USER_ID)
            userId: String,
            @ApiParam(value = "项目组代码", required = false)
            @QueryParam("groupCode")
            groupCode: String?,
            @ApiParam(value = "仓库id", required = true)
            @QueryParam("repoId")
            repoId: String,
            @ApiParam(value = "token类型 1：oauth 2:privateKey", required = true)
            @QueryParam("tokenType")
            tokenType: TokenTypeEnum
    ): Result<GitProjectInfo?>

    @ApiOperation("获取代码仓库单个文件内容")
    @GET
    @Path("/{repoId}/getFileContent")
    fun getFileContent(
            @ApiParam(value = "仓库id")
            @PathParam("repoId")
            repoId: String,
            @ApiParam(value = "文件路径")
            @QueryParam("filePath")
            filePath: String,
            @ApiParam(value = "版本号（svn）")
            @QueryParam("reversion")
            reversion: String?,
            @ApiParam(value = "分支（git）")
            @QueryParam("branch")
            branch: String?,
            @ApiParam("代码库请求类型", required = true)
            @QueryParam("repositoryType")
            repositoryType: RepositoryType?
    ): Result<String>

    @ApiOperation("关联代码库")
    @POST
    @Path("/{projectId}/")
    fun create(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "代码库模型", required = true)
        repository: Repository
    ): Result<RepositoryId>

    @ApiOperation("代码库列表")
    @GET
    @Path("/{projectId}/")
    fun list(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("仓库类型", required = false)
        @QueryParam("repositoryType")
        repositoryType: ScmType?
    ): Result<List<RepositoryInfoWithPermission>>

    @ApiOperation("获取代码库详情")
    @GET
    @Path("/{projectId}/{repositoryId}/")
    fun get(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("代码库哈希ID货代代码库名称", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @ApiParam("代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?
    ): Result<Repository>

    @ApiOperation("统计代码仓库个数")
    @GET
    @Path("/count")
    fun count(
        @ApiParam("项目ID", required = false)
        @QueryParam("projectId")
        projectId: Set<String> = setOf(),
        @ApiParam("代码库哈希ID", required = false)
        @QueryParam("repositoryHashId")
        repositoryHashId: String? = "",
        @ApiParam("代码仓库类型", required = false)
        @QueryParam("repositoryType")
        repositoryType: ScmType? = null,
        @ApiParam("代码仓库别名", required = false)
        @QueryParam("aliasName")
        aliasName: String? = ""
    ): Result<Long>

    @ApiOperation("代码库列表")
    @GET
    @Path("/{projectId}/hasPermissionList")
    fun hasPermissionList(
            @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_USER_ID)
            userId: String,
            @ApiParam("项目ID", required = true)
            @PathParam("projectId")
            projectId: String,
            @ApiParam("仓库类型", required = false)
            @QueryParam("repositoryType")
            repositoryType: ScmType?,
            @ApiParam("对应权限", required = true, defaultValue = "")
            @QueryParam("permission")
            permission: Permission
    ): Result<Page<RepositoryInfo>>

    @ApiOperation("删除代码库")
    @DELETE
    @Path("/{projectId}/{repositoryHashId}")
    fun delete(
            @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_USER_ID)
            userId: String,
            @ApiParam("项目ID", required = true)
            @PathParam("projectId")
            projectId: String,
            @ApiParam("代码库哈希ID", required = true)
            @PathParam("repositoryHashId")
            repositoryHashId: String
    ): Result<Boolean>

    @ApiOperation("关联代码库")
    @POST
//    @Path("/{projectId}/")
    @Path("/projectId/{projectId}/")
    fun createV2(
            @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_USER_ID)
            userId: String,
            @ApiParam("项目ID", required = true)
            @PathParam("projectId")
            projectId: String,
            @ApiParam(value = "代码库模型", required = true)
            repository: Repository
    ): Result<RepositoryId>

    @ApiOperation("代码库列表")
    @GET
//    @Path("/{projectId}/")
    @Path("/projectId/{projectId}/")
    fun listV2(
            @ApiParam("项目ID", required = true)
            @PathParam("projectId")
            projectId: String,
            @ApiParam("仓库类型", required = false)
            @QueryParam("repositoryType")
            repositoryType: ScmType?
    ): Result<List<RepositoryInfoWithPermission>>

    @ApiOperation("获取代码库详情")
    @GET
//    @Path("/{projectId}/{repositoryId}/")
    @Path("/projectId/{projectId}/repositoryId/{repositoryId}/")
    fun getV2(
            @ApiParam("项目ID", required = true)
            @PathParam("projectId")
            projectId: String,
            @ApiParam("代码库哈希ID货代代码库名称", required = true)
            @PathParam("repositoryId")
            repositoryId: String,
            @ApiParam("代码库请求类型", required = true)
            @QueryParam("repositoryType")
            repositoryType: RepositoryType?
    ): Result<Repository>

    @ApiOperation("获取代码仓库单个文件内容")
    @GET
//    @Path("/{repoId}/getFileContent")
    @Path("/repoId/{repoId}/getFileContent")
    fun getFileContentV2(
            @ApiParam(value = "仓库id")
            @PathParam("repoId")
            repoId: String,
            @ApiParam(value = "文件路径")
            @QueryParam("filePath")
            filePath: String,
            @ApiParam(value = "版本号（svn）")
            @QueryParam("reversion")
            reversion: String?,
            @ApiParam(value = "分支（git）")
            @QueryParam("branch")
            branch: String?,
            @ApiParam("代码库请求类型", required = true)
            @QueryParam("repositoryType")
            repositoryType: RepositoryType?
    ): Result<String>

    @ApiOperation("代码库列表")
    @GET
//    @Path("/{projectId}/hasPermissionList")
    @Path("/projectId/{projectId}/hasPermissionList")
    fun hasPermissionListV2(
            @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_USER_ID)
            userId: String,
            @ApiParam("项目ID", required = true)
            @PathParam("projectId")
            projectId: String,
            @ApiParam("仓库类型", required = false)
            @QueryParam("repositoryType")
            repositoryType: ScmType?,
            @ApiParam("对应权限", required = true, defaultValue = "")
            @QueryParam("permission")
            permission: Permission
    ): Result<Page<RepositoryInfo>>

    @ApiOperation("删除代码库")
    @DELETE
//    @Path("/{projectId}/{repositoryHashId}")
    @Path("/projectId/{projectId}/repositoryHashId/{repositoryHashId}")
    fun deleteV2(
            @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_USER_ID)
            userId: String,
            @ApiParam("项目ID", required = true)
            @PathParam("projectId")
            projectId: String,
            @ApiParam("代码库哈希ID", required = true)
            @PathParam("repositoryHashId")
            repositoryHashId: String
    ): Result<Boolean>
}