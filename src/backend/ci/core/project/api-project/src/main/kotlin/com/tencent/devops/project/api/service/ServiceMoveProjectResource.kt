package com.tencent.devops.project.api.service

import com.tencent.devops.project.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

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
