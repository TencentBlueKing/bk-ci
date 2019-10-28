package com.tencent.devops.store.api.op

import com.tencent.devops.store.pojo.container.Container
import com.tencent.devops.store.pojo.container.ContainerRequest
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

@Api(tags = ["OP_PIPELINE_CONTAINER"], description = "OP-流水线-构建容器")
@Path("/op/pipeline/container")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpContainerResource {

    @ApiOperation("添加流水线构建容器")
    @POST
    @Path("/")
    fun add(
        @ApiParam(value = "流水线构建容器请求报文体", required = true)
        pipelineContainerRequest: ContainerRequest
    ): Result<Boolean>

    @ApiOperation("更新流水线构建容器信息")
    @PUT
    @Path("/{id}")
    fun update(
        @ApiParam("容器ID", required = true)
        @PathParam("id")
        id: String,
        @ApiParam(value = "流水线构建容器请求报文体", required = true)
        pipelineContainerRequest: ContainerRequest
    ): Result<Boolean>

    @ApiOperation("获取所有的流水线构建容器信息")
    @GET
    @Path("/")
    fun listAllContainers(): Result<List<Container>>

    @ApiOperation("根据ID获取流水线构建容器信息")
    @GET
    @Path("/{id}")
    fun getContainerById(
        @ApiParam("容器ID", required = true)
        @PathParam("id")
        id: String
    ): Result<Container?>

    @ApiOperation("根据ID删除流水线构建容器信息")
    @DELETE
    @Path("/{id}")
    fun deleteContainerById(
        @ApiParam("容器ID", required = true)
        @PathParam("id")
        id: String
    ): Result<Boolean>
}