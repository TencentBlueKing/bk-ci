package com.tencent.devops.project.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BG_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_PROJECT"], description = "蓝盾项目列表接口")
@Path("/service/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceProjectResource {

    @GET
    @Path("/")
    @ApiOperation("查询所有项目")
    fun list(
        @ApiParam("PAAS_CC Token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String
    ): Result<List<ProjectVO>>

    @POST
    @Path("/")
    @ApiOperation("查询指定项目")
    fun listByProjectCode(
        @ApiParam(value = "项目id", required = true)
        projectCodes: Set<String>
    ): Result<List<ProjectVO>>

    @GET
    @Path("/getProjectByUser")
    @ApiOperation("查询所有项目")
    fun getProjectByUser(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userName: String
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
        centerName: String
    ): Result<List<ProjectVO>>

    @GET
    @Path("/{projectCode}/users/{userId}/verify")
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
    @Path("/getNameByCode")
    @ApiOperation("根据项目Code获取对应的名称")
    fun getNameByCode(
        @ApiParam("projectCodes，多个以英文逗号分隔", required = true)
        @QueryParam("projectCodes")
        projectCodes: String
    ): Result<HashMap<String, String>>

    @GET
    @Path("/{projectId}")
    @ApiOperation("查询所有项目")
    fun get(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        englishName: String
    ): Result<ProjectVO?>

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
//    @Path("/{projectCode}/users/{userId}/verify")
    @Path("/projectCode/{projectCode}/users/{userId}/verify")
    @ApiOperation(" 校验用户是否项目成员")
    fun verifyUserProjectPermissionV2(
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
//    @Path("/{projectId}")
    @Path("/projectId/{projectId}")
    @ApiOperation("查询所有项目")
    fun getV2(
            @ApiParam("项目ID", required = true)
            @PathParam("projectId")
            englishName: String
    ): Result<ProjectVO?>

    @GET
//    @Path("/preBuild/userProject/{userId}")
    @Path("/preBuild/userProject/userId/{userId}")
    @ApiOperation("查询用户项目")
    fun getPreUserProjectV2(
            @ApiParam("用户ID", required = true)
            @PathParam("userId")
            userId: String,
            @ApiParam("accessToken", required = true)
            @QueryParam("accessToken")
            accessToken: String
    ): Result<ProjectVO?>

    @POST
    @Path("/newProject")
    @ApiOperation("创建项目")
    fun create(
            @ApiParam("userId", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            userId: String,
            @ApiParam(value = "项目信息", required = true)
            projectCreateInfo: ProjectCreateInfo
    ): Result<String>
}