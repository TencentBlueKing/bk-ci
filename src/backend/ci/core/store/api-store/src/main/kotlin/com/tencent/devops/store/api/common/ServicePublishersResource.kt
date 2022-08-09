package com.tencent.devops.store.api.common

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.PublishersRequest
import com.tencent.devops.store.pojo.common.StoreDockingPlatformRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_PUBLISHER"], description = "service-publisher")
@Path("/service/publisher/sync")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePublishersResource {

    @ApiOperation("同步新增发布者信息")
    @POST
    @Path("/publisher/add")
    fun synAddPublisherData(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("用户ID", required = true)
        userId: String,
        publishers: List<PublishersRequest>
    ): Result<Int>

    @ApiOperation("同步删除发布者信息")
    @DELETE
    @Path("/publisher/delete")
    fun synDeletePublisherData(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("用户ID", required = true)
        userId: String,
        publishers: List<PublishersRequest>
    ): Result<Int>

    @ApiOperation("同步更新发布者信息")
    @POST
    @Path("/publisher/update")
    fun synUpdatePublisherData(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("用户ID", required = true)
        userId: String,
        publishers: List<PublishersRequest>
    ): Result<Int>

    @ApiOperation("同步新增工具平台信息")
    @POST
    @Path("/platforms/add")
    fun synAddPlatformsData(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("用户ID", required = true)
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Result<Int>

    @ApiOperation("同步删除工具平台信息")
    @DELETE
    @Path("/platforms/delete")
    fun synDeletePlatformsData(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("用户ID", required = true)
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Result<Int>

    @ApiOperation("同步更新工具平台信息")
    @POST
    @Path("/platforms/update")
    fun synUpdatePlatformsData(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("用户ID", required = true)
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Result<Int>
}