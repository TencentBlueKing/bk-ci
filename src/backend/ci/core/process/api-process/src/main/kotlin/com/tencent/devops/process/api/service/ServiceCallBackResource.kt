package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.process.pojo.ProjectPipelineCallBack
import com.tencent.devops.process.pojo.ProjectPipelineCallBackHistory
import com.tencent.devops.process.pojo.pipeline.enums.CallBackNetWorkRegionType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_CALLBACK"], description = "服务-回调")
@Path("/service/callBacks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceCallBackResource {
    @ApiOperation("创建callback回调")
    @POST
    @Path("/projects/{projectId}")
    fun create(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("url", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam("region", required = true)
        @QueryParam("region")
        region: CallBackNetWorkRegionType?,
        @ApiParam("event", required = true)
        @QueryParam("event")
        event: CallBackEvent,
        @ApiParam("secretToken", required = false)
        @QueryParam("secretToken")
        secretToken: String?
    ): Result<Boolean>

    @ApiOperation("callback回调列表")
    @GET
    @Path("/projects/{projectId}")
    fun list(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ProjectPipelineCallBack>>

    @ApiOperation("callback回调移除")
    @DELETE
    @Path("/projects/{projectId}/{id}")
    fun remove(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("id", required = true)
        @PathParam("id")
        id: Long
    ): Result<Boolean>

    @ApiOperation("callback回调执行历史记录")
    @GET
    @Path("/history/{projectId}")
    fun listHistory(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("回调url", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam("事件类型", required = true)
        @QueryParam("event")
        event: CallBackEvent,
        @ApiParam("开始时间(时间戳形式)", required = false)
        @QueryParam("startTime")
        startTime: Long?,
        @ApiParam("结束时间(时间戳形式)", required = false)
        @QueryParam("endTime")
        endTime: Long?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ProjectPipelineCallBackHistory>>

    @ApiOperation("callback回调重试")
    @POST
    @Path("/history/{projectId}/{id}/retry")
    fun retry(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("id", required = true)
        @PathParam("id")
        id: Long
    ): Result<Boolean>
}
