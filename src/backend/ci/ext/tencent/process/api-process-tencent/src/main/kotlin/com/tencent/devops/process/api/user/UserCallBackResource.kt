package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.process.pojo.pipeline.enums.CallBackNetWorkRegionType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_CALLBACK"], description = "用户-回调")
@Path("/user/callBacks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserCallBackResource {
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
        region: CallBackNetWorkRegionType,
        @ApiParam("event", required = true)
        @QueryParam("event")
        event: CallBackEvent,
        @ApiParam("secretToken", required = false)
        @QueryParam("secretToken")
        secretToken: String?
    ): Result<Boolean>
}