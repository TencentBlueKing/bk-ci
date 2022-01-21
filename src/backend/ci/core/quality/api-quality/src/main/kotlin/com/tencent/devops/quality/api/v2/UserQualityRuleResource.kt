/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_RULE_V2"], description = "质量红线-拦截规则v2")
@Path("/user/rules/v2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserQualityRuleResource {
    @ApiOperation("是否有创建拦截规则权限")
    @Path("/{projectId}/hasCreatePermission")
    @GET
    fun hasCreatePermission(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>

    @ApiOperation("创建拦截规则")
    @Path("/{projectId}/")
    @POST
    fun create(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("规则内容", required = true)
        rule: RuleCreateRequest
    ): Result<String>

    @ApiOperation("更新拦截规则列表")
    @Path("/{projectId}/{ruleHashId}")
    @PUT
    fun update(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("规则ID", required = true)
        @PathParam("ruleHashId")
        ruleHashId: String,
        @ApiParam("规则内容", required = true)
        rule: RuleUpdateRequest
    ): Result<Boolean>

    @ApiOperation("开启拦截规则")
    @Path("/{projectId}/{ruleHashId}/enable")
    @PUT
    fun enable(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("规则ID", required = true)
        @PathParam("ruleHashId")
        ruleHashId: String
    ): Result<Boolean>

    @ApiOperation("停用拦截规则")
    @Path("/{projectId}/{ruleHashId}/disable")
    @PUT
    fun disable(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("规则ID", required = true)
        @PathParam("ruleHashId")
        ruleHashId: String
    ): Result<Boolean>

    @ApiOperation("删除拦截规则列表")
    @Path("/{projectId}/{ruleHashId}")
    @DELETE
    fun delete(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("规则ID", required = true)
        @PathParam("ruleHashId")
        ruleHashId: String
    ): Result<Boolean>

    @ApiOperation("获取拦截规则")
    @Path("/{projectId}/{ruleHashId}")
    @GET
    fun get(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("规则ID", required = true)
        @PathParam("ruleHashId")
        ruleHashId: String
    ): Result<UserQualityRule>

    @ApiOperation("获取拦截历史列表")
    @Path("/{projectId}/{ruleHashId}/interceptHistory")
    @GET
    fun getInterceptHistory(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("规则ID", required = true)
        @PathParam("ruleHashId")
        ruleHashId: String,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目(不传默认全部返回)", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<RuleInterceptHistory>>

    @ApiOperation("获取拦截规则列表")
    @Path("/{projectId}/list")
    @GET
    fun list(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("页目", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<QualityRuleSummaryWithPermission>>

    @ApiOperation("匹配拦截规则")
    @Path("/{projectId}/matchRuleList")
    @GET
    fun matchRuleList(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = false, defaultValue = "1")
        @QueryParam("pipelineId")
        pipelineId: String
    ): Result<List<QualityRuleMatchTask>>

    @ApiOperation("匹配模板拦截规则")
    @Path("/{projectId}/matchTemplateRuleList")
    @GET
    fun matchTemplateRuleList(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线模板ID", required = false, defaultValue = "1")
        @QueryParam("templateId")
        templateId: String?
    ): Result<List<QualityRuleMatchTask>>

    @ApiOperation("获取规则模板")
    @Path("/listTemplates")
    @GET
    fun listTemplates(
        @ApiParam("项目ID", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String
    ): Result<List<RuleTemplate>>

    @ApiOperation("获取规则模板")
    @Path("/project/{projectId}/listTemplates")
    @GET
    fun listProjectTemplates(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<RuleTemplate>>

    @ApiOperation("查询生效范围数据")
    @Path("/listPipelineRangeDetail")
    @POST
    fun listPipelineRangeDetail(
        request: PipelineRangeDetailRequest
    ): Result<List<RulePipelineRange>>

    @ApiOperation("查询模板生效范围数据")
    @Path("/listTemplateRangeDetail")
    @POST
    fun listTemplateRangeDetail(
        request: TemplateRangeDetailRequest
    ): Result<List<RuleTemplateRange>>

    @ApiOperation("获取拦截规则列表")
    @Path("/project/{projectId}/pipeline/{pipelineId}/listAtomRule")
    @GET
    fun listAtomRule(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("插件code", required = false, defaultValue = "1")
        @QueryParam("atomCode")
        atomCode: String,
        @ApiParam("插件版本", required = false, defaultValue = "1")
        @QueryParam("atomVersion")
        atomVersion: String
    ): Result<AtomRuleResponse>

    @ApiOperation("获取拦截规则列表")
    @Path("/project/{projectId}/template/{templateId}/listTemplateAtomRule")
    @GET
    fun listTemplateAtomRule(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @ApiParam("插件code", required = false, defaultValue = "1")
        @QueryParam("atomCode")
        atomCode: String,
        @ApiParam("插件版本", required = false, defaultValue = "1")
        @QueryParam("atomVersion")
        atomVersion: String
    ): Result<AtomRuleResponse>
}
