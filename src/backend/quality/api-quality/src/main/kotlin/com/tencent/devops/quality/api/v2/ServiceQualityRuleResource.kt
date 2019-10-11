package com.tencent.devops.quality.api.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.api.v2.pojo.request.BuildCheckParams
import com.tencent.devops.quality.api.v2.pojo.request.CopyRuleRequest
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleMatchTask
import com.tencent.devops.quality.pojo.RuleCheckResult
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_RULE_V2"], description = "质量红线-拦截规则v2")
@Path("/service/rules/v2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceQualityRuleResource {
    @ApiOperation("获取匹配原子")
    @Path("/{projectId}/{pipelineId}/matchRuleList")
    @GET
    fun matchRuleList(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("模板id", required = true)
        @QueryParam("templateId")
        templateId: String?,
        @ApiParam("构建启动时间", required = true)
        @QueryParam("startTime")
        startTime: Long
    ): Result<List<QualityRuleMatchTask>>

    @ApiOperation("回去审批用户列表")
    @Path("/{projectId}/{pipelineId}/{buildId}/auditUserList")
    @GET
    fun getAuditUserList(
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

    @ApiOperation("检测是否通过控制点")
    @Path("/check")
    @POST
    fun check(
        @ApiParam("构建检查参数", required = true)
        buildCheckParams: BuildCheckParams
    ): Result<RuleCheckResult>

    @ApiOperation("复制红线到某个项目下面去")
    @Path("/copyRule")
    @POST
    fun copyRule(
        request: CopyRuleRequest
    ): Result<List<String>>
}