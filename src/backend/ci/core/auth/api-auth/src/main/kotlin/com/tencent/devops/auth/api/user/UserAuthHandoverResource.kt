package com.tencent.devops.auth.api.user

import com.tencent.devops.auth.pojo.request.HandoverDetailsQueryReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewBatchUpdateReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewQueryReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewUpdateReq
import com.tencent.devops.auth.pojo.request.ResourceType2CountOfHandoverQuery
import com.tencent.devops.auth.pojo.vo.HandoverAuthorizationDetailVo
import com.tencent.devops.auth.pojo.vo.HandoverGroupDetailVo
import com.tencent.devops.auth.pojo.vo.HandoverOverviewVo
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverConditionRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "USER_RESOURCE_AUTHORIZATION", description = "用户-权限-交接相关")
@Path("/user/auth/handover/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserAuthHandoverResource {
    @POST
    @Path("/{projectId}/handoverAuthorizationsApplication")
    @Operation(summary = "交接授权申请")
    fun handoverAuthorizationsApplication(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源授权交接条件实体", required = true)
        condition: ResourceAuthorizationHandoverConditionRequest
    ): Result<String>

    @POST
    @Path("/listHandoverOverviews")
    @Operation(summary = "权限交接总览列表")
    fun listHandoverOverviews(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "权限交接总览查询", required = true)
        queryRequest: HandoverOverviewQueryReq
    ): Result<SQLPage<HandoverOverviewVo>>

    @POST
    @Path("/getResourceType2CountOfHandover")
    @Operation(summary = "获取资源授权管理数量")
    fun getResourceType2CountOfHandover(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "查询请求体", required = true)
        queryReq: ResourceType2CountOfHandoverQuery
    ): Result<List<ResourceType2CountVo>>

    @POST
    @Path("/listAuthorizationsOfHandover")
    @Operation(summary = "获取交接单中授权相关")
    fun listAuthorizationsOfHandover(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "权限交接详细查询请求体", required = true)
        queryReq: HandoverDetailsQueryReq
    ): Result<SQLPage<HandoverAuthorizationDetailVo>>

    @POST
    @Path("/listGroupsOfHandover")
    @Operation(summary = "获取交接单中用户组相关")
    fun listGroupsOfHandover(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "权限交接详细查询请求体", required = true)
        queryReq: HandoverDetailsQueryReq
    ): Result<SQLPage<HandoverGroupDetailVo>>

    @POST
    @Path("/handleHanoverApplication")
    @Operation(summary = "处理交接审批单")
    fun handleHanoverApplication(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "更新权限交接总览请求体", required = true)
        request: HandoverOverviewUpdateReq
    ): Result<Boolean>

    @POST
    @Path("/batchHandleHanoverApplications")
    @Operation(summary = "批量处理交接审批单")
    fun batchHandleHanoverApplications(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "批量更新权限交接总览请求体", required = true)
        request: HandoverOverviewBatchUpdateReq
    ): Result<Boolean>
}
