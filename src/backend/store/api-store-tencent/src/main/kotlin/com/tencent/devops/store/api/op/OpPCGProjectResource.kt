package com.tencent.devops.store.api.op

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_PCG_PROJECT"], description = "OP-PCG-PROJECT")
@Path("/op/pipeline/pcg/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPCGProjectResource {

    @ApiOperation("这是项目下pcg构建机可见")
    @POST
    @Path("/{projectId}")
    fun add(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>

    @ApiOperation("取消项目下PCG构建机可见")
    @DELETE
    @Path("/{projectId}")
    fun delete(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>
}