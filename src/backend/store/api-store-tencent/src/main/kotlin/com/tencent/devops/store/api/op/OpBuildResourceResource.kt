package com.tencent.devops.store.api.op

import com.tencent.devops.store.pojo.container.BuildResource
import com.tencent.devops.store.pojo.container.BuildResourceRequest
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_PIPELINE_BUILD_RESOURCE"], description = "OP-流水线-构建资源")
@Path("/op/pipeline/build/resource")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpBuildResourceResource {

    @ApiOperation("添加流水线构建资源信息")
    @POST
    @Path("/")
    fun add(
        @ApiParam(value = "流水线构建资源请求体", required = true)
        buildResourceRequest: BuildResourceRequest
    ): Result<Boolean>

    @ApiOperation("更新流水线构建资源信息")
    @PUT
    @Path("/{id}")
    fun update(
        @ApiParam("流水线构建资源ID", required = true)
        @PathParam("id")
        id: String,
        @ApiParam(value = "流水线构建资源请求体", required = true)
        buildResourceRequest: BuildResourceRequest
    ): Result<Boolean>

    @ApiOperation("获取所有流水线构建资源信息")
    @GET
    @Path("/")
    fun listAllPipelineBuildResources(): Result<List<BuildResource>>

    @ApiOperation("根据ID获取流水线构建资源信息")
    @GET
    @Path("/{id}")
    fun getPipelineBuildResourceById(
        @ApiParam("流水线构建资源ID", required = true)
        @PathParam("id")
        id: String
    ): Result<BuildResource?>

    @ApiOperation("根据ID获取流水线构建资源信息")
    @DELETE
    @Path("/{id}")
    fun deletePipelineBuildResourceById(
        @ApiParam("流水线构建资源ID", required = true)
        @PathParam("id")
        id: String
    ): Result<Boolean>
}