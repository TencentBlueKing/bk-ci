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

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.api.v2.pojo.RulePipelineRange
import com.tencent.devops.quality.api.v2.pojo.RuleTemplate
import com.tencent.devops.quality.api.v2.pojo.RuleTemplateRange
import com.tencent.devops.quality.api.v2.pojo.request.PipelineRangeDetailRequest
import com.tencent.devops.quality.api.v2.pojo.request.RuleCreateRequest
import com.tencent.devops.quality.api.v2.pojo.request.RuleUpdateRequest
import com.tencent.devops.quality.api.v2.pojo.request.TemplateRangeDetailRequest
import com.tencent.devops.quality.api.v2.pojo.response.AtomRuleResponse
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleMatchTask
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleSummaryWithPermission
import com.tencent.devops.quality.api.v2.pojo.response.UserQualityRule
import com.tencent.devops.quality.pojo.RuleInterceptHistory
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

@Tag(name = "USER_RULE_V2", description = "质量红线-拦截规则v2")
@Path("/user/rules/v2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserQualityRuleResource {
    @Operation(summary = "是否有创建拦截规则权限")
    @Path("/{projectId}/hasCreatePermission")
    @GET
    fun hasCreatePermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>

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

    @Operation(summary = "开启拦截规则")
    @Path("/{projectId}/{ruleHashId}/enable")
    @PUT
    fun enable(
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

    @Operation(summary = "停用拦截规则")
    @Path("/{projectId}/{ruleHashId}/disable")
    @PUT
    fun disable(
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

    @Operation(summary = "获取拦截规则")
    @Path("/{projectId}/{ruleHashId}")
    @GET
    fun get(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "规则ID", required = true)
        @PathParam("ruleHashId")
        ruleHashId: String
    ): Result<UserQualityRule>

    @Operation(summary = "获取拦截历史列表")
    @Path("/{projectId}/{ruleHashId}/interceptHistory")
    @GET
    fun getInterceptHistory(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "规则ID", required = true)
        @PathParam("ruleHashId")
        ruleHashId: String,
        @Parameter(description = "页目", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数目(不传默认全部返回)", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<RuleInterceptHistory>>

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

    @Operation(summary = "匹配拦截规则")
    @Path("/{projectId}/matchRuleList")
    @GET
    fun matchRuleList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = false, example = "1")
        @QueryParam("pipelineId")
        pipelineId: String
    ): Result<List<QualityRuleMatchTask>>

    @Operation(summary = "匹配模板拦截规则")
    @Path("/{projectId}/matchTemplateRuleList")
    @GET
    fun matchTemplateRuleList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线模板ID", required = false, example = "1")
        @QueryParam("templateId")
        templateId: String?
    ): Result<List<QualityRuleMatchTask>>

    @Operation(summary = "获取规则模板")
    @Path("/listTemplates")
    @GET
    fun listTemplates(
        @Parameter(description = "项目ID", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String
    ): Result<List<RuleTemplate>>

    @Operation(summary = "获取规则模板")
    @Path("/project/{projectId}/listTemplates")
    @GET
    fun listProjectTemplates(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<RuleTemplate>>

    @Operation(summary = "查询生效范围数据")
    @Path("/listPipelineRangeDetail")
    @POST
    fun listPipelineRangeDetail(
        request: PipelineRangeDetailRequest
    ): Result<List<RulePipelineRange>>

    @Operation(summary = "查询模板生效范围数据")
    @Path("/listTemplateRangeDetail")
    @POST
    fun listTemplateRangeDetail(
        request: TemplateRangeDetailRequest
    ): Result<List<RuleTemplateRange>>

    @Operation(summary = "获取拦截规则列表")
    @Path("/project/{projectId}/pipeline/{pipelineId}/listAtomRule")
    @GET
    fun listAtomRule(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "插件code", required = false, example = "1")
        @QueryParam("atomCode")
        atomCode: String,
        @Parameter(description = "插件版本", required = false, example = "1")
        @QueryParam("atomVersion")
        atomVersion: String
    ): Result<AtomRuleResponse>

    @Operation(summary = "获取拦截规则列表")
    @Path("/project/{projectId}/template/{templateId}/listTemplateAtomRule")
    @GET
    fun listTemplateAtomRule(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "插件code", required = false, example = "1")
        @QueryParam("atomCode")
        atomCode: String,
        @Parameter(description = "插件版本", required = false, example = "1")
        @QueryParam("atomVersion")
        atomVersion: String
    ): Result<AtomRuleResponse>
}
