package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.dispatch.pojo.DockerHostZoneWithPage
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = arrayOf("OP_DOCKERHOST_ZONE"), description = "DockerHost母机管理")
@Path("/op/dockerhost")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpDockerHostZoneResource {

    @ApiOperation("添加DockerHost母机")
    @POST
    @Path("/create")
    fun create(
        @ApiParam(value = "DockerHost母机", required = true)
        @QueryParam("hostIp")
        hostIp: String,
        @ApiParam(value = "DockerHost母机区域", required = true)
        @QueryParam("zone")
        zone: Zone,
        @ApiParam(value = "备注", required = true)
        @QueryParam("remark")
        remark: String?
    ): Result<Boolean>

    @ApiOperation("删除DockerHost母机")
    @POST
    @Path("/delete")
    fun delete(
        @ApiParam(value = "DockerHost母机IP", required = true)
        @QueryParam("hostIp")
        hostIp: String
    ): Result<Boolean>

    @ApiOperation("列出DockerHost母机")
    @GET
    @Path("/list")
    fun list(
        @ApiParam(value = "第几页，从1开始", required = true)
        @QueryParam("page")
        page: Int,
        @ApiParam(value = "每页条数", required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<DockerHostZoneWithPage>

    @ApiOperation("启用DockerHost当构建机")
    @POST
    @Path("/enable")
    fun enable(
        @ApiParam(value = "DockerHost母机IP", required = true)
        @QueryParam("hostIp")
        hostIp: String,
        @ApiParam(value = "enable", required = true)
        @QueryParam("enable")
        enable: Boolean
    ): Result<Boolean>
}