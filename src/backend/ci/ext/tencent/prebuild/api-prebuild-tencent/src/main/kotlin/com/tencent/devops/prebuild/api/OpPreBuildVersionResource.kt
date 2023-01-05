package com.tencent.devops.prebuild.api

import com.tencent.devops.prebuild.pojo.PrePluginVersion
import com.tencent.devops.project.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_PREBUILD"], description = "OP-PREBUILD版本资源")
@Path("/op/prebuild/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPreBuildVersionResource {

    @ApiOperation("新增IDE插件的版本信息")
    @POST
    @Path("/ide")
    fun creatIdeVersion(
        @ApiParam("版本信息", required = true)
        prePluginVersion: PrePluginVersion
    ): Result<Boolean>

    @ApiOperation("修改IDE插件的版本信息")
    @PUT
    @Path("/ide")
    fun updateIdeVersion(
        @ApiParam("版本信息", required = true)
        prePluginVersion: PrePluginVersion
    ): Result<Boolean>

    @ApiOperation("删除IDE插件的版本信息")
    @DELETE
    @Path("/ide")
    fun deleteIdeVersion(
        @ApiParam("/版本号", required = true)
        @QueryParam("version")
        version: String
    ): Result<Boolean>

    @ApiOperation("查询IDE插件的版本信息")
    @GET
    @Path("/ide")
    fun getIdeVersion(): Result<List<PrePluginVersion>>
}