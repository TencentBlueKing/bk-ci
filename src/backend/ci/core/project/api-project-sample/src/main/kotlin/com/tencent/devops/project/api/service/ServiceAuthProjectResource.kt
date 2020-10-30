package com.tencent.devops.project.api.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["AUTH_PROJECT"], description = "权限中心-蓝盾项目")
@Path("/service/auth/project")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceAuthProjectResource {

    @GET
    @Path("/list")
    @ApiOperation("分页获取项目信息")
    fun list(
        @ApiParam("")
        @QueryParam("limit")
        limit: Int,
        @ApiParam("")
        @QueryParam("offset")
        offset: Int
    ): Result<Page<ProjectVO>>

    @GET
    @Path("/get")
    @ApiOperation("获取数据")
    fun getByIds(
        @ApiParam("")
        @QueryParam("ids")
        ids: Set<String>
    ): Result<List<ProjectVO>>
}