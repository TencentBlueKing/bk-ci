package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.GcloudConfReq
import com.tencent.devops.plugin.pojo.GcloudConfResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_GCLOUD_CONF"], description = "OP-GCLOUD配置")
@Path("/op/gcloud")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpGcloudConfResource {

    @ApiOperation("新增GCLOUD配置")
    @POST
    @Path("/conf/create")
    fun create(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("gcloud请求", required = true)
        gcloudConfReq: GcloudConfReq
    ): Result<Map<String, Int>>

    @ApiOperation("编辑GCLOUD配置")
    @PUT
    @Path("/conf/edit")
    fun edit(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("gcloud请求", required = true)
        gcloudConfReq: GcloudConfReq
    ): Result<Int>

    @ApiOperation("删除GCLOUD配置")
    @DELETE
    @Path("/conf/id/{confId}/delete")
    fun delete(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("gcloud配置id", required = true)
        @PathParam("confId")
        confId: Int
    ): Result<Int>

    @ApiOperation("查询GCLOUD配置")
    @GET
    @Path("/conf/list")
    fun getList(
        @ApiParam(value = "开始页数，从1开始", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam(value = "每页数据条数(默认10条)", required = false, defaultValue = "12")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<GcloudConfResponse>
}