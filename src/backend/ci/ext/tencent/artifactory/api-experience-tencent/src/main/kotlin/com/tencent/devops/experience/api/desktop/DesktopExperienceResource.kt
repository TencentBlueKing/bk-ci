package com.tencent.devops.experience.api.desktop

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.AppExperienceDetail
import com.tencent.devops.experience.pojo.AppExperienceInstallPackage
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.ExperienceChangeLog
import com.tencent.devops.experience.pojo.ExperienceList
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "DESKTOP_EXPERIENCE", description = "版本体验-桌面端公网体验")
@Path("/desktop/experiences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SuppressWarnings("LongParameterList")
interface DesktopExperienceResource {

    @Operation(summary = "获取体验列表")
    @Path("/list")
    @GET
    fun list(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组织", required = false)
        @QueryParam("organization")
        organization: String? = null
    ): Result<ExperienceList>

    @Operation(summary = "获取公网体验详情")
    @Path("/{experienceHashId}/detail")
    @GET
    fun detail(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "体验ID", required = true)
        @PathParam("experienceHashId")
        experienceHashId: String,
        @Parameter(description = "组织", required = false)
        @QueryParam("organization")
        organization: String? = null
    ): Result<AppExperienceDetail>

    @Operation(summary = "获取公网体验历史版本")
    @Path("/{experienceHashId}/changeLog")
    @GET
    fun changeLog(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "体验ID", required = true)
        @PathParam("experienceHashId")
        experienceHashId: String,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页数目", required = true)
        @QueryParam("pageSize")
        pageSize: Int,
        @Parameter(description = "版本号", required = false)
        @QueryParam("version")
        version: String? = null,
        @Parameter(description = "组织", required = false)
        @QueryParam("organization")
        organization: String? = null,
        @Parameter(description = "体验发起时间--起始时间(秒级)", required = false)
        @QueryParam("createDateBegin")
        createDateBegin: Long? = null,
        @Parameter(description = "体验发起时间--终止时间(秒级)", required = false)
        @QueryParam("createDateEnd")
        createDateEnd: Long? = null,
        @Parameter(description = "体验结束时间--起始时间(秒级)", required = false)
        @QueryParam("endDateBegin")
        endDateBegin: Long? = null,
        @Parameter(description = "体验结束时间--终止时间(秒级)", required = false)
        @QueryParam("endDateEnd")
        endDateEnd: Long? = null,
    ): Result<Pagination<ExperienceChangeLog>>

    @Operation(summary = "获取公网体验安装包列表")
    @Path("/{experienceHashId}/installPackages")
    @GET
    fun installPackages(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "体验ID", required = true)
        @PathParam("experienceHashId")
        experienceHashId: String,
        @Parameter(description = "组织", required = false)
        @QueryParam("organization")
        organization: String? = null,
    ): Result<Pagination<AppExperienceInstallPackage>>

    @Operation(summary = "获取公网体验下载链接")
    @Path("/{experienceHashId}/downloadUrl")
    @POST
    fun downloadUrl(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "体验ID", required = true)
        @PathParam("experienceHashId")
        experienceHashId: String,
        @Parameter(description = "组织", required = false)
        @QueryParam("organization")
        organization: String? = null,
    ): Result<DownloadUrl>
}