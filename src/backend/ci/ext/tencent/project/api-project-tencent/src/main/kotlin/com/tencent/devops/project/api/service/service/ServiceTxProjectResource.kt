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

package com.tencent.devops.project.api.service.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BG_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_DEPT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.project.api.pojo.PipelinePermissionInfo
import com.tencent.devops.project.pojo.AddManagerRequest
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectCreateUserDTO
import com.tencent.devops.project.pojo.ProjectDeptInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
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

@Api(tags = ["SERVICE_PROJECT_TX"], description = "蓝盾项目列表接口")
@Path("/service/projects/tx")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceTxProjectResource {

    @GET
    @Path("/")
    @ApiOperation("查询所有项目")
    fun list(
        @ApiParam("PAAS_CC Token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String
    ): Result<List<ProjectVO>>

    @GET
    @Path("/getProjectByGroup")
    @ApiOperation("根据组织架构查询所有项目")
    fun getProjectByGroup(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("bgName", required = false)
        @QueryParam("bgName")
        bgName: String?,
        @ApiParam("deptName", required = false)
        @QueryParam("deptName")
        deptName: String?,
        @ApiParam("centerName", required = false)
        @QueryParam("centerName")
        centerName: String?
    ): Result<List<ProjectVO>>

    @GET
    @Path("/getProjectByOrganizationId")
    @ApiOperation("根据组织架构查询所有项目")
    fun getProjectByName(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "组织类型", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE)
        organizationType: String,
        @ApiParam(value = "组织Id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_ID)
        organizationId: Long,
        @ApiParam("deptName", required = false)
        @QueryParam("deptName")
        deptName: String?,
        @ApiParam("centerName", required = false)
        @QueryParam("centerName")
        centerName: String?
    ): Result<List<ProjectVO>>

    @GET
    @Path("/getProjectByName")
    @ApiOperation("根据名称查询项目信息,组织限制")
    fun getProjectByName(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "组织类型", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE)
        organizationType: String,
        @ApiParam(value = "组织Id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_ID)
        organizationId: Long,
        @ApiParam("项目名称,精准匹配", required = true)
        @QueryParam("name")
        name: String,
        @ApiParam("名称类型: 中文名称、英文名称", required = true)
        @QueryParam("nameType")
        nameType: ProjectValidateType,
        @ApiParam("是否过滤保密项目", required = false)
        @QueryParam("showSecrecy")
        showSecrecy: Boolean?
    ): Result<ProjectVO?>

    @GET
    @Path("/getProjectByGroupId")
    @ApiOperation("根据组织架构查询所有项目")
    fun getProjectByGroupId(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("bgId", required = false)
        @QueryParam("bgId")
        bgId: Long?,
        @ApiParam("deptId", required = false)
        @QueryParam("deptId")
        deptId: Long?,
        @ApiParam("centerId", required = false)
        @QueryParam("centerId")
        centerId: Long?
    ): Result<List<ProjectVO>>

    @GET
    @Path("/preBuild/userProject/{userId}")
    @ApiOperation("查询用户项目")
    fun getPreUserProject(
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String,
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String
    ): Result<ProjectVO?>

    @GET
    @Path("/enNames/organization")
    @ApiOperation("查询用户项目")
    fun getProjectEnNamesByOrganization(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("BG_ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BG_ID)
        bgId: Long,
        @ApiParam("部门名称", required = true)
        @QueryParam("deptName")
        deptName: String?,
        @ApiParam("中心名称", required = true)
        @QueryParam("centerName")
        centerName: String?
    ): Result<List<String>>

    @GET
    @Path("/enNames/dept")
    @ApiOperation("查询用户项目")
    fun getProjectEnNamesByDeptIdAndCenterName(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("部门ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_DEPT_ID)
        deptId: Long?,
        @ApiParam("中心名称", required = true)
        @QueryParam("centerName")
        centerName: String?
    ): Result<List<String>>

    @GET
    @Path("/enNames/center")
    @ApiOperation("查询用户项目")
    fun getProjectEnNamesByCenterId(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("中心ID", required = true)
        @QueryParam("centerId")
        centerId: Long?
    ): Result<List<String>>

    @GET
    @Path("/rds/getOrCreate")
    @ApiOperation("查询用户项目")
    fun getOrCreateRdsProject(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("蓝盾项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("蓝盾项目名称", required = true)
        @QueryParam("projectName")
        projectName: String
    ): Result<ProjectVO?>

    @POST
    @Path("/newProject")
    @ApiOperation("创建项目")
    fun create(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("PAAS_CC Token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam(value = "项目信息", required = true)
        projectCreateInfo: ProjectCreateInfo,
        @QueryParam("routerTag")
        routerTag: String?
    ): Result<String>

    @GET
    @Path("/projects/{projectCode}/managers")
    @ApiOperation(" 查询项目的管理员")
    fun getProjectManagers(
        @ApiParam("项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<List<String>>

    @GET
    @Path("/{projectCode}/users/{userId}/verifyWithToken")
    @ApiOperation(" 校验用户是否项目成员")
    fun verifyUserProjectPermission(
        @ApiParam("PAAS_CC Token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String
    ): Result<Boolean>

    @GET
    @Path("/{projectCode}/verifyProjectByOrganization")
    @ApiOperation(" 校验项目是否数据某组织架构")
    fun verifyProjectByOrganization(
        @ApiParam("项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam(value = "组织类型", required = true)
        @QueryParam(AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE)
        organizationType: String,
        @ApiParam(value = "组织ID", required = true)
        @QueryParam(AUTH_HEADER_DEVOPS_ORGANIZATION_ID)
        organizationId: Int
    ): Result<Boolean>

    @POST
    @Path("/gitci/{gitProjectId}/{userId}")
    @ApiOperation("创建gitCI项目")
    fun createGitCIProject(
        @ApiParam("工蜂项目id", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam("用户名", required = true)
        @PathParam("userId")
        userId: String,
        @ApiParam("工蜂项目名称", required = false)
        @QueryParam("gitProjectName")
        gitProjectName: String?
    ): Result<ProjectVO>

    @POST
    @Path("/addManager")
    @ApiOperation(" 为项目添加管理员")
    fun addManagerForProject(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("管理员", required = true)
        addManagerRequest: AddManagerRequest
    ): Result<Boolean>

    @POST
    @Path("/createProjectUser")
    fun createProjectUser(
        @ApiParam("执行人Id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        createUser: String?,
        @ApiParam("是否校验管理员", required = true)
        @QueryParam("checkManager")
        checkManager: Boolean,
        @ApiParam("添加信息", required = true)
        createInfo: ProjectCreateUserDTO
    ): Result<Boolean>

    @POST
    @Path("/create/permission/")
    fun createPipelinePermission(
        @ApiParam("执行人Id", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        createUser: String?,
        @ApiParam("是否校验管理员", required = true)
        @QueryParam("checkManager")
        checkManager: Boolean,
        @ApiParam("添加信息", required = true)
        createInfo: PipelinePermissionInfo
    ): Result<Boolean>

    @GET
    @Path("{projectId}/roles")
    fun getProjectRoles(
        @ApiParam("项目Id", required = true)
        @PathParam("projectId")
        projectCode: String
    ): Result<List<BKAuthProjectRolesResources>>

    @PUT
    @Path("{projectCode}/relation/bind")
    fun bindRelationSystem(
        @ApiParam("项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("关联系统ID", required = true)
        @QueryParam("relationId")
        relationId: String
    ): Result<Boolean>

    @POST
    @Path("{projectCode}/update/name")
    @ApiOperation("修改项目名称")
    fun updateProjectName(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("关联系统ID", required = true)
        @QueryParam("projectName")
        projectName: String
    ): Result<Boolean>

    @GET
    @Path("/getProjectInfoByProjectName")
    @ApiOperation("根据项目名称查询项目信息")
    fun getProjectInfoByProjectName(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("项目名称", required = true)
        @QueryParam("projectName")
        projectName: String
    ): Result<ProjectVO>?

    @PUT
    @Path("/{projectCode}/bind/organization")
    @ApiOperation("绑定项目组织信息")
    fun bindProjectOrganization(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("项目名称", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("项目组织信息", required = true)
        projectDeptInfo: ProjectDeptInfo
    ): Result<Boolean>
}
