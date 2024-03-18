package com.tencent.devops.project.api.service

import com.tencent.devops.project.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_PROJECT", description = "项目迁移")
@Path("/service/projects/move")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceMoveProjectResource {
    @PUT
    @Path("/{projectCode}/relationIam")
    fun relationIamProject(
        @Parameter(description = "项目Code", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "IAM关联Id", required = true)
        @QueryParam("relationId")
        relationId: String
    ): Result<Boolean>
}
