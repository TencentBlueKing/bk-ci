package com.tencent.devops.remotedev.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.remotedev.pojo.bkvision.BkVisionDatasetQueryBody
import com.tencent.devops.remotedev.pojo.bkvision.BkVisionResp
import com.tencent.devops.remotedev.pojo.bkvision.QueryFieldDataBody
import com.tencent.devops.remotedev.pojo.bkvision.QueryVariableDataBody
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_BK_VISION"], description = "用户-BK-VISION")
@Path("/user/bkvision")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserBkVisionResource {

    @ApiOperation("query_meta")
    @GET
    @Path("/bkvision/api/v1/meta/query")
    fun metaQuery(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @QueryParam("share_uid")
        shareId: String,
        @QueryParam("type")
        type: String
    ): BkVisionResp

    @ApiOperation("query_dataset")
    @POST
    @Path("/bkvision/api/v1/dataset/query")
    fun datasetQuery(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        data: BkVisionDatasetQueryBody
    ): BkVisionResp

    @ApiOperation("query_field_data")
    @POST
    @Path("/bkvision/api/v1/field/{uid}/preview_data")
    fun queryFieldData(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @PathParam("uid")
        uid: String,
        data: QueryFieldDataBody
    ): BkVisionResp

    @ApiOperation("query_variable_data")
    @POST
    @Path("/bkvision/api/v1/variable/query")
    fun queryVariableData(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        data: QueryVariableDataBody
    ): BkVisionResp
}
