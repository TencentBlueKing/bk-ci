/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.quality.api.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.api.v2.pojo.QualityHisMetadata
import com.tencent.devops.quality.api.v2.pojo.request.BuildCheckParams
import com.tencent.devops.quality.api.v2.pojo.request.CopyRuleRequest
import com.tencent.devops.quality.api.v2.pojo.request.RuleCreateRequest
import com.tencent.devops.quality.api.v2.pojo.request.RuleUpdateRequest
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleMatchTask
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleSummaryWithPermission
import com.tencent.devops.common.quality.pojo.RuleCheckResult
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_RULE_V2", description = "质量红线-拦截规则v2")
@Path("/service/rules/v2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceQualityRuleResource {
    @Operation(summary = "获取匹配原子")
    @Path("/{projectId}/{pipelineId}/matchRuleList")
    @GET
    fun matchRuleList(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "模板id", required = true)
        @QueryParam("templateId")
        templateId: String?,
        @Parameter(description = "构建启动时间", required = true)
        @QueryParam("startTime")
        startTime: Long
    ): Result<List<QualityRuleMatchTask>>

    @Operation(summary = "回去审批用户列表")
    @Path("/{projectId}/{pipelineId}/{buildId}/auditUserList")
    @GET
    fun getAuditUserList(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "原子ID", required = true)
        @QueryParam("taskId")
        taskId: String
    ): Result<Set<String>>

    @Operation(summary = "检测是否通过控制点")
    @Path("/check")
    @POST
    fun check(
        @Parameter(description = "构建检查参数", required = true)
        buildCheckParams: BuildCheckParams
    ): Result<RuleCheckResult>

    @Operation(summary = "复制红线到某个项目下面去")
    @Path("/copyRule")
    @POST
    fun copyRule(
        request: CopyRuleRequest
    ): Result<List<String>>

    @Operation(summary = "获取历史元数据")
    @Path("/builds/{buildId}/getHisMetadata")
    @GET
    fun getHisMetadata(
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<List<QualityHisMetadata>>

    @Operation(summary = "创建拦截规则")
    @Path("/{projectId}/")
    @POST
    fun create(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "规则内容", required = true)
        rule: RuleCreateRequest
    ): Result<String>

    @Operation(summary = "更新拦截规则列表")
    @Path("/{projectId}/{ruleHashId}")
    @PUT
    fun update(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "规则ID", required = true)
        @PathParam("ruleHashId")
        ruleHashId: String,
        @Parameter(description = "规则内容", required = true)
        rule: RuleUpdateRequest
    ): Result<Boolean>

    @Operation(summary = "删除拦截规则列表")
    @Path("/{projectId}/{ruleHashId}")
    @DELETE
    fun delete(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "规则ID", required = true)
        @PathParam("ruleHashId")
        ruleHashId: String
    ): Result<Boolean>

    @Operation(summary = "获取拦截规则列表")
    @Path("/{projectId}/list")
    @GET
    fun list(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "页目", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数目", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<QualityRuleSummaryWithPermission>>
}
