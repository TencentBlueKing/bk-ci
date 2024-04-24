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
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.project.api.pojo.PipelinePermissionInfo
import com.tencent.devops.project.pojo.AddManagerRequest
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectCreateUserDTO
import com.tencent.devops.project.pojo.ProjectDeptInfo
import com.tencent.devops.project.pojo.ProjectExtSystemTagDTO
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "SERVICE_PROJECT_TX", description = "蓝盾项目列表接口")
@Path("/service/projects/tx")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("TooManyFunctions", "LongParameterList")
interface ServiceTxProjectResource {
    @GET
    @Path("/")
    @Operation(summary = "查询所有项目")
    fun list(
        @Parameter(description = "PAAS_CC Token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String
    ): Result<List<ProjectVO>>

    @GET
    @Path("/getProjectByGroup")
    @Operation(summary = "根据组织架构查询所有项目")
    fun getProjectByGroup(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "bgName", required = false)
        @QueryParam("bgName")
        bgName: String?,
        @Parameter(description = "deptName", required = false)
        @QueryParam("deptName")
        deptName: String?,
        @Parameter(description = "centerName", required = false)
        @QueryParam("centerName")
        centerName: String?
    ): Result<List<ProjectVO>>

    @GET
    @Path("/getProjectByOrganizationId")
    @Operation(summary = "根据组织架构查询所有项目")
    fun getProjectByName(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "组织类型", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE)
        organizationType: String,
        @Parameter(description = "组织Id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_ID)
        organizationId: Long,
        @Parameter(description = "deptName", required = false)
        @QueryParam("deptName")
        deptName: String?,
        @Parameter(description = "centerName", required = false)
        @QueryParam("centerName")
        centerName: String?
    ): Result<List<ProjectVO>>

    @GET
    @Path("/getProjectByName")
    @Operation(summary = "根据名称查询项目信息,组织限制")
    fun getProjectByName(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "组织类型", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE)
        organizationType: String,
        @Parameter(description = "组织Id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_ID)
        organizationId: Long,
        @Parameter(description = "项目名称,精准匹配", required = true)
        @QueryParam("name")
        name: String,
        @Parameter(description = "名称类型: 中文名称、英文名称", required = true)
        @QueryParam("nameType")
        nameType: ProjectValidateType,
        @Parameter(description = "是否过滤保密项目", required = false)
        @QueryParam("showSecrecy")
        showSecrecy: Boolean?
    ): Result<ProjectVO?>

    @GET
    @Path("/getProjectByGroupId")
    @Operation(summary = "根据组织架构查询所有项目")
    fun getProjectByGroupId(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "bgId", required = false)
        @QueryParam("bgId")
        bgId: Long?,
        @Parameter(description = "deptId", required = false)
        @QueryParam("deptId")
        deptId: Long?,
        @Parameter(description = "centerId", required = false)
        @QueryParam("centerId")
        centerId: Long?
    ): Result<List<ProjectVO>>

    @GET
    @Path("/preBuild/userProject/{userId}")
    @Operation(summary = "查询用户项目")
    fun getPreUserProject(
        @Parameter(description = "用户ID", required = true)
        @PathParam("userId")
        userId: String,
        @Parameter(description = "accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String
    ): Result<ProjectVO?>

    @GET
    @Path("/remotedev/userProject/{userId}")
    @Operation(summary = "查询用户项目")
    fun getRemoteDevUserProject(
        @Parameter(description = "用户ID", required = true)
        @PathParam("userId")
        userId: String
    ): Result<ProjectVO?>

    @GET
    @Path("/enNames/organization")
    @Operation(summary = "查询用户项目")
    fun getProjectEnNamesByOrganization(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "BG_ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BG_ID)
        bgId: Long,
        @Parameter(description = "部门名称", required = true)
        @QueryParam("deptName")
        deptName: String?,
        @Parameter(description = "中心名称", required = true)
        @QueryParam("centerName")
        centerName: String?
    ): Result<List<String>>

    @GET
    @Path("/enNames/dept")
    @Operation(summary = "查询用户项目")
    fun getProjectEnNamesByDeptIdAndCenterName(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "部门ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_DEPT_ID)
        deptId: Long?,
        @Parameter(description = "中心名称", required = true)
        @QueryParam("centerName")
        centerName: String?
    ): Result<List<String>>

    @GET
    @Path("/enNames/center")
    @Operation(summary = "查询用户项目")
    fun getProjectEnNamesByCenterId(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "中心ID", required = true)
        @QueryParam("centerId")
        centerId: Long?
    ): Result<List<String>>

    @GET
    @Path("/rds/getOrCreate")
    @Operation(summary = "查询用户项目")
    fun getOrCreateRdsProject(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "蓝盾项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "蓝盾项目名称", required = true)
        @QueryParam("projectName")
        projectName: String
    ): Result<ProjectVO?>

    @POST
    @Path("/newProject")
    @Operation(summary = "创建项目")
    fun create(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "PAAS_CC Token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @Parameter(description = "项目信息", required = true)
        projectCreateInfo: ProjectCreateInfo,
        @QueryParam("routerTag")
        routerTag: String?
    ): Result<String>

    @GET
    @Path("/projects/{projectCode}/managers")
    @Operation(summary = " 查询项目的管理员")
    fun getProjectManagers(
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<List<String>>

    @GET
    @Path("/{projectCode}/users/{userId}/verifyWithToken")
    @Operation(summary = " 校验用户是否项目成员")
    fun verifyUserProjectPermission(
        @Parameter(description = "PAAS_CC Token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "用户ID", required = true)
        @PathParam("userId")
        userId: String
    ): Result<Boolean>

    @GET
    @Path("/{projectCode}/verifyProjectByOrganization")
    @Operation(summary = " 校验项目是否数据某组织架构")
    fun verifyProjectByOrganization(
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "组织类型", required = true)
        @QueryParam(AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE)
        organizationType: String,
        @Parameter(description = "组织ID", required = true)
        @QueryParam(AUTH_HEADER_DEVOPS_ORGANIZATION_ID)
        organizationId: Int
    ): Result<Boolean>

    @POST
    @Path("/gitci/{gitProjectId}/{userId}")
    @Operation(summary = "创建gitCI项目")
    fun createGitCIProject(
        @Parameter(description = "工蜂项目id", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long,
        @Parameter(description = "用户名", required = true)
        @PathParam("userId")
        userId: String,
        @Parameter(description = "工蜂项目名称", required = false)
        @QueryParam("gitProjectName")
        gitProjectName: String?,
        @Parameter(description = "项目运营归属", required = false)
        @QueryParam("productId")
        productId: Int? = null
    ): Result<ProjectVO>

    @POST
    @Path("/addManager")
    @Operation(summary = " 为项目添加管理员")
    fun addManagerForProject(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "管理员", required = true)
        addManagerRequest: AddManagerRequest
    ): Result<Boolean>

    @POST
    @Path("/createProjectUser")
    fun createProjectUser(
        @Parameter(description = "执行人Id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        createUser: String?,
        @Parameter(description = "是否校验管理员", required = true)
        @QueryParam("checkManager")
        checkManager: Boolean,
        @Parameter(description = "添加信息", required = true)
        createInfo: ProjectCreateUserDTO
    ): Result<Boolean>

    @POST
    @Path("/create/permission/")
    fun createPipelinePermission(
        @Parameter(description = "执行人Id", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        createUser: String?,
        @Parameter(description = "是否校验管理员", required = true)
        @QueryParam("checkManager")
        checkManager: Boolean,
        @Parameter(description = "添加信息", required = true)
        createInfo: PipelinePermissionInfo
    ): Result<Boolean>

    @GET
    @Path("{projectId}/roles")
    fun getProjectRoles(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectId")
        projectCode: String
    ): Result<List<BKAuthProjectRolesResources>>

    @PUT
    @Path("{projectCode}/relation/bind")
    fun bindRelationSystem(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "关联系统ID", required = true)
        @QueryParam("relationId")
        relationId: String
    ): Result<Boolean>

    @POST
    @Path("{projectCode}/update/name")
    @Operation(summary = "修改项目名称")
    fun updateProjectName(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "关联系统ID", required = true)
        @QueryParam("projectName")
        projectName: String
    ): Result<Boolean>

    @GET
    @Path("/getProjectInfoByProjectName")
    @Operation(summary = "根据项目名称查询项目信息")
    fun getProjectInfoByProjectName(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String
    ): Result<ProjectVO>?

    @PUT
    @Path("/{projectCode}/bind/organization")
    @Operation(summary = "绑定项目组织信息")
    fun bindProjectOrganization(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目名称", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "项目组织信息", required = true)
        projectDeptInfo: ProjectDeptInfo
    ): Result<Boolean>

    @PUT
    @Path("/updateRemotedev")
    @Operation(summary = "修改项目云桌面信息")
    fun updateRemotedev(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @Parameter(description = "云桌面配额", required = false)
        @QueryParam("desktopNum")
        addcloudDesktopNum: Int?,
        @Parameter(description = "开启或关闭云研发", required = false)
        @QueryParam("enable")
        enable: Boolean?
    ): Result<Boolean>

    @Operation(summary = "按项目扩展系统设置consul Tag")
    @PUT
    @Path("/ext/system/setTagByProject")
    fun setExtSystemTagByProject(
        @Parameter(description = "consulTag请求入参", required = true)
        extSystemTagDTO: ProjectExtSystemTagDTO
    ): com.tencent.devops.common.api.pojo.Result<Boolean>

    @Operation(summary = "更新项目运营归属信息")
    @Path("/productIds/{productId}/update")
    @PUT
    fun batchUpdateProjectProductId(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目运营归属ID", required = true)
        @PathParam("productId")
        productId: Int,
        @Parameter(description = "项目ID列表", required = true)
        projectIds: List<String>
    ): Result<Boolean>

    @Operation(summary = "查询开启了云研发的项目")
    @GET
    @Path("/projectEnableRemotedev")
    fun projectEnableRemotedev(
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectCode")
        projectCode: String?
    ): Result<Map<String, String>>
}
