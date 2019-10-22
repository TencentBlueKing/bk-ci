package com.tencent.devops.quality.api.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.pojo.RuleInterceptHistory
import com.tencent.devops.quality.pojo.enum.RuleInterceptResult
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_INTERCEPT_v2"], description = "质量红线-拦截记录v2")
@Path("/user/intercepts/v2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserQualityInterceptResource {
    @ApiOperation("获取拦截记录")
    @Path("/{projectId}/")
    @GET
    fun list(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam("规则ID", required = false)
        @QueryParam("ruleHashId")
        ruleHashId: String?,
        @ApiParam("状态", required = false)
        @QueryParam("interceptResult")
        interceptResult: RuleInterceptResult?,
        @ApiParam("开始时间", required = false)
        @QueryParam("startTime")
        startTime: Long?,
        @ApiParam("截止时间", required = false)
        @QueryParam("endTime")
        endTime: Long?,
        @ApiParam("页号", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("页数", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<RuleInterceptHistory>>

    @ApiOperation("获取审核用户列表")
    @Path("/{projectId}/{pipelineId}/{buildId}/auditUserList")
    @GET
    fun getAuditUserList(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("原子ID", required = true)
        @QueryParam("taskId")
        taskId: String
    ): Result<Set<String>>
}