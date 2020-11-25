package com.tencent.devops.experience.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.ExperienceServiceCreate
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_EXPERIENCE"], description = "版本体验-发布体验")
@Path("/build/experiences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildExperienceResource {
    @ApiOperation("创建体验")
    @Path("/projects/{projectId}/users/{userId}/")
    @POST
    fun create(
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("发布详情", required = true)
        experience: ExperienceServiceCreate
    ): Result<Boolean>
}
