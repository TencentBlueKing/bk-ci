package com.tencent.devops.remotedev.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.remotedev.pojo.bkvision.BkVisionDatasetQueryBody
import com.tencent.devops.remotedev.pojo.bkvision.BkVisionResp
import com.tencent.devops.remotedev.pojo.bkvision.QueryFieldDataBody
import com.tencent.devops.remotedev.pojo.bkvision.QueryVariableDataBody
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_BK_VISION", description = "用户-BK-VISION")
@Path("/user/bkvision/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserBkVisionResource {

    @Operation(summary = "query_meta")
    @GET
    @Path("/bkvision/api/v1/meta/query")
    fun metaQuery(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @QueryParam("share_uid")
        shareId: String,
        @QueryParam("type")
        type: String
    ): BkVisionResp

    @Operation(summary = "query_dataset")
    @POST
    @Path("/bkvision/api/v1/dataset/query")
    fun datasetQuery(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        data: BkVisionDatasetQueryBody
    ): BkVisionResp

    @Operation(summary = "query_field_data")
    @POST
    @Path("/bkvision/api/v1/field/{uid}/preview_data")
    fun queryFieldData(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @PathParam("uid")
        uid: String,
        data: QueryFieldDataBody
    ): BkVisionResp

    @Operation(summary = "query_variable_data")
    @POST
    @Path("/bkvision/api/v1/variable/query")
    fun queryVariableData(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        data: QueryVariableDataBody
    ): BkVisionResp
}
