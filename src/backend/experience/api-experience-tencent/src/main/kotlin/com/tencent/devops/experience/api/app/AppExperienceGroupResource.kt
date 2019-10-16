package com.tencent.devops.experience.api.app

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.GroupSummaryWithPermission
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["EXPERIENCE_GROUP"], description = "版本体验-用户分组")
@Path("/app/experience/group")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AppExperienceGroupResource {

    @ApiOperation("获取体验用户分组")
    @Path("/{projectId}/list")
    @GET
    fun list(
        @ApiParam("用户Id", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目Id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("页数", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目(不传默认全部返回)", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<GroupSummaryWithPermission>>
}