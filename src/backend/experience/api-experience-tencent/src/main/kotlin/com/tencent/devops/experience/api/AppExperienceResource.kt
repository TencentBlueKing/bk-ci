package com.tencent.devops.experience.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.AppExperience
import com.tencent.devops.experience.pojo.AppExperienceDetail
import com.tencent.devops.experience.pojo.AppExperienceSummary
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.ExperienceCreate
import com.tencent.devops.experience.pojo.ProjectGroupAndUsers
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

@Api(tags = ["APP_EXPERIENCE"], description = "版本体验-发布体验")
@Path("/app/experiences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AppExperienceResource {

    @ApiOperation("获取体验列表")
    @Path("/list")
    @GET
    fun list(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<AppExperience>>

    @ApiOperation("获取体验详情")
    @Path("/{experienceHashId}/detail")
    @GET
    fun detail(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("体验ID", required = true)
        @PathParam("experienceHashId")
        experienceHashId: String
    ): Result<AppExperienceDetail>

    @ApiOperation("创建外部直接下载链接")
    @Path("/{experienceHashId}/downloadUrl")
    @POST
    fun downloadUrl(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("体验ID", required = true)
        @PathParam("experienceHashId")
        experienceHashId: String
    ): Result<DownloadUrl>

    @ApiOperation("获取项目体验列表")
    @Path("/{projectId}/history")
    @GET
    fun history(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<AppExperienceSummary>>

    @ApiOperation("获取项目用户组信息和组所有人员")
    @Path("/{projectId}/projectGroupAndUsers")
    @GET
    fun projectGroupAndUsers(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<ProjectGroupAndUsers>>

    @ApiOperation("创建体验")
    @Path("{projectId}")
    @POST
    fun creat(
        @ApiParam("用户Id", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目Id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("体验详情", required = true)
        experience: ExperienceCreate
    ): Result<Boolean>
}