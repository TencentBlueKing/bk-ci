package com.tencent.devops.store.api.op

import com.tencent.devops.store.pojo.app.ContainerAppVersion
import com.tencent.devops.store.pojo.app.ContainerAppVersionCreate
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

@Api(tags = ["OP_CONTAINER_APP_VERSION"], description = "OP-容器-编译环境版本")
@Path("/op/pipeline/container/app/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpContainerAppVersionResource {

    @ApiOperation("添加编译环境版本")
    @POST
    @Path("/")
    fun addContainerAppVersion(
        @ApiParam("容器编译环境版本请求实体", required = true)
         containerAppVersionRequest: ContainerAppVersionCreate
    ): Result<Boolean>

    @ApiOperation("根据ID删除编译环境版本")
    @DELETE
    @Path("/{id}")
    fun deleteContainerAppVersionById(
        @ApiParam("编译环境版本ID", required = true)
        @PathParam("id")
        id: Int
    ): Result<Boolean>

    @ApiOperation("更新编译环境版本")
    @PUT
    @Path("/{id}")
    fun updateContainerAppVersion(
        @ApiParam("编译环境版本ID", required = true)
        @PathParam("id")
        id: Int,
        @ApiParam("容器编译环境版本请求实体", required = true)
        containerAppVersionRequest: ContainerAppVersionCreate
    ): Result<Boolean>

    @ApiOperation("根据appId获取所有编译环境版本信息")
    @GET
    @Path("/list/{appId}")
    fun listContainerAppVersionsByAppId(
        @ApiParam("编译环境ID", required = true)
        @PathParam("appId")
        appId: Int
    ): Result<List<ContainerAppVersion>>

    @ApiOperation("根据ID获取编译环境版本信息")
    @GET
    @Path("/{id}")
    fun getContainerAppVersionById(
        @ApiParam("编译环境版本ID", required = true)
        @PathParam("id")
        id: Int
    ): Result<ContainerAppVersion?>
}