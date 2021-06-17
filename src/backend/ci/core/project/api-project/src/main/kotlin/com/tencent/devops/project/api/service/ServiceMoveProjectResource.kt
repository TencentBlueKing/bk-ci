package com.tencent.devops.project.api.service

import com.tencent.devops.project.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_PROJECT"], description = "项目迁移")
@Path("/service/projects/move")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceMoveProjectResource {
    @PUT
    @Path("/{projectCode}/relationIam")
    fun relationIamProject(
        @ApiParam("项目Code", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("IAM关联Id", required = true)
        @QueryParam("relationId")
        relationId: String
    ): Result<Boolean>
}
