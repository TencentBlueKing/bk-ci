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

package com.tencent.devops.project.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.MigrateProjectConditionDTO
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.project.pojo.OrgInfo
import com.tencent.devops.project.pojo.ProjectBaseInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.ProjectWithPermission
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_PROJECT"], description = "项目列表接口")
@Path("/service/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceProjectResource {

    @GET
    @Path("/")
    @ApiOperation("查询当前用户有权限的项目列表")
    fun list(
        @ApiParam("用户ID", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<ProjectVO>>

    @GET
    @Path("/getAllProject")
    @ApiOperation("查询所有项目")
    fun getAllProject(): Result<List<ProjectVO>>

    @POST
    @Path("/listMigrateProjects/{offset}/{limit}")
    @ApiOperation("条件迁移项目实体")
    fun listMigrateProjects(
        @ApiParam("条件迁移项目实体", required = false)
        migrateProjectConditionDTO: MigrateProjectConditionDTO,
        @ApiParam("limit", required = true)
        @PathParam("limit")
        limit: Int,
        @ApiParam("offset", required = true)
        @PathParam("offset")
        offset: Int
    ): Result<List<ProjectWithPermission>>

    @POST
    @Path("/")
    @ApiOperation("查询指定项目，不包括被禁用的项目")
    fun listByProjectCode(
        @ApiParam(value = "项目id", required = true)
        projectCodes: Set<String>
    ): Result<List<ProjectVO>>

    @POST
    @Path("/listOnlyByProjectCode")
    @ApiOperation("查询指定项目，包括被禁用的项目")
    fun listOnlyByProjectCode(
        @ApiParam(value = "项目id", required = true)
        projectCodes: Set<String>
    ): Result<List<ProjectVO>>

    @POST
    @Path("/listByProjectCodes")
    @ApiOperation("查询指定项目")
    fun listByProjectCodeList(
        @ApiParam(value = "项目id", required = true)
        projectCodes: List<String>
    ): Result<List<ProjectVO>>

    @GET
    @Path("/getProjectByUser")
    @ApiOperation("查询所有项目")
    fun getProjectByUser(
        @ApiParam("userId", required = true)
        @QueryParam("userId")
        userName: String
    ): Result<List<ProjectVO>>

    @GET
    @Path("/{projectCode}/users/{userId}/verify")
    @ApiOperation(" 校验用户是否项目成员")
    fun verifyUserProjectPermission(
        @ApiParam("accessToken", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String? = null,
        @ApiParam("项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String
    ): Result<Boolean>

    @GET
    @Path("/getNameByCode")
    @ApiOperation("根据项目Code获取对应的名称")
    fun getNameByCode(
        @ApiParam("projectCodes，多个以英文逗号分隔", required = true)
        @QueryParam("projectCodes")
        projectCodes: String
    ): Result<HashMap<String, String>>

    @GET
    @Path("/{projectId}")
    @ApiOperation("查询指定EN项目")
    fun get(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        englishName: String
    ): Result<ProjectVO?>

    @POST
    @Path("/create")
    @ApiOperation("创建项目")
    fun create(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "项目信息", required = true)
        projectCreateInfo: ProjectCreateInfo,
        @ApiParam("accessToken", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String? = null
    ): Result<Boolean>

    @POST
    @Path("/create/ext/system")
    @ApiOperation("创建扩展系统项目")
    fun createExtSystem(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("项目信息")
        projectInfo: ProjectCreateInfo,
        @QueryParam("needAuth")
        @ApiParam("是否需要权限")
        needAuth: Boolean,
        @QueryParam("needAuth")
        @ApiParam("是否需要校验")
        needValidate: Boolean,
        @QueryParam("projectChanel")
        channel: ProjectChannelCode
    ): Result<ProjectVO?>

    @PUT
    @Path("/{projectId}")
    @ApiOperation("修改项目")
    fun update(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "项目信息", required = true)
        projectUpdateInfo: ProjectUpdateInfo,
        @ApiParam("accessToken", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String? = null
    ): Result<Boolean>

    @PUT
    @Path("/{projectCode}/projectName")
    fun updateProjectName(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("项目Code", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("项目名称", required = true)
        @QueryParam("projectName")
        projectName: String
    ): Result<Boolean>

    @GET
    @Path("projectNames/{projectName}")
    fun getProjectByName(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("projectName", required = true)
        @PathParam("projectName")
        projectName: String
    ): Result<ProjectVO?>

    @PUT
    @Path("/{validateType}/names/validate")
    @ApiOperation("校验项目名称和项目英文名")
    fun validate(
        @ApiParam("校验的是项目名称或者项目英文名")
        @PathParam("validateType")
        validateType: ProjectValidateType,
        @ApiParam("项目名称或者项目英文名")
        @QueryParam("name")
        name: String,
        @ApiParam("项目ID")
        @QueryParam("english_name")
        projectId: String?
    ): Result<Boolean>

    @POST
    @Path("/{projectId}/orgcheck")
    @ApiOperation("是否是组织下的项目")
    fun isOrgProject(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("orgInfo", required = true)
        orgInfos: OrgInfo
    ): Result<Boolean>

    @GET
    @Path("/getMinId")
    @ApiOperation("查询最小项目ID")
    fun getMinId(): Result<Long>

    @GET
    @Path("/getMaxId")
    @ApiOperation("查询最大项目ID")
    fun getMaxId(): Result<Long>

    @GET
    @Path("/getProjectListById")
    @ApiOperation("根据ID查询项目列表")
    fun getProjectListById(
        @ApiParam("最小项目ID", required = true)
        @QueryParam("minId")
        minId: Long,
        @ApiParam("最大项目ID", required = true)
        @QueryParam("maxId")
        maxId: Long
    ): Result<List<ProjectBaseInfo>>

    @ApiOperation("查看灰度项目列表")
    @GET
    @Path("/listSecrecyProject")
    fun listSecrecyProject(): Result<Set<String>?>

    @ApiOperation("为项目添加成员")
    @POST
    @Path("/{projectId}/createUser")
    fun createProjectUser(
        @ApiParam("项目Id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("添加信息", required = true)
        createInfo: ProjectCreateUserInfo
    ): Result<Boolean>

    @ApiOperation("是否拥有某实例的某action的权限")
    @Path("/{projectId}/hasPermission/{permission}")
    @GET
    fun hasPermission(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("权限action", required = true)
        @PathParam("permission")
        permission: AuthPermission
    ): Result<Boolean>

    @PUT
    @Path("/{projectId}/updateProjectSubjectScopes")
    @ApiOperation("修改项目最大可授权范围")
    fun updateProjectSubjectScopes(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "ke", required = true)
        subjectScopes: List<SubjectScopeInfo>
    ): Result<Boolean>
}
