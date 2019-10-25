package com.tencent.devops.store.api.op

import com.tencent.devops.store.pojo.app.ContainerApp
import com.tencent.devops.store.pojo.app.ContainerAppInfo
import com.tencent.devops.store.pojo.app.ContainerAppRequest
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

@Api(tags = ["OP_CONTAINER_APP"], description = "OP-容器-编译环境")
@Path("/op/pipeline/container/app")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpContainerAppResource {

    @ApiOperation("添加编译环境")
    @POST
    @Path("/")
    fun addContainerApp(
        @ApiParam("容器编译环境请求实体", required = true)
        containerAppRequest: ContainerAppRequest
    ): Result<Boolean>

    @ApiOperation("删除编译环境")
    @DELETE
    @Path("/{id}")
    fun deleteContainerAppById(
        @ApiParam("编译环境ID", required = true)
        @PathParam("id")
        id: Int
    ): Result<Boolean>

    @ApiOperation("更新编译环境")
    @PUT
    @Path("/{id}")
    fun updateContainerApp(
        @ApiParam("编译环境ID", required = true)
        @PathParam("id")
        id: Int,
        @ApiParam("容器编译环境请求实体", required = true)
        containerAppRequest: ContainerAppRequest
    ): Result<Boolean>

    @ApiOperation("获取所有编译环境信息")
    @GET
    @Path("/")
    fun listContainerApps(): Result<List<ContainerApp>>

    @ApiOperation("根据ID获取编译环境信息")
    @GET
    @Path("/{id}")
    fun getContainerAppById(
        @ApiParam("编译环境ID", required = true)
        @PathParam("id")
        id: Int
    ): Result<ContainerAppInfo?>
}