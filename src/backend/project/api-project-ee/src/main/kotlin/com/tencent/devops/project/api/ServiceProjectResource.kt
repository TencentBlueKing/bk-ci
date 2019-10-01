package com.tencent.devops.project.api

import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_PROJECT"], description = "项目列表接口")
@Path("/service/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceProjectResource {

    @GET
    @Path("/getAllProject")
    @ApiOperation("查询所有项目")
    fun getAllProject(): Result<List<ProjectVO>>
}