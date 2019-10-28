package com.tencent.devops.project.api.service

import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_PROJECT"], description = "蓝盾项目列表接口")
@Path("/build/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildProjectResource {

    @GET
    @Path("/{projectCode}")
    @ApiOperation("查询指定项目")
    fun listByProjectCode(
            @ApiParam(value = "项目id", required = true)
            @PathParam(value = "projectCode")
            projectCode: String
    ): Result<List<ProjectVO>>
}