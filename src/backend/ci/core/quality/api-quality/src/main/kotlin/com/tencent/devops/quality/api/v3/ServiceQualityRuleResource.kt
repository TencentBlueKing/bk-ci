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

package com.tencent.devops.quality.api.v3

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.api.v3.pojo.request.BuildCheckParamsV3
import com.tencent.devops.quality.api.v3.pojo.request.RuleCreateRequestV3
import com.tencent.devops.quality.api.v3.pojo.response.RuleCreateResponseV3
import com.tencent.devops.common.quality.pojo.RuleCheckResult
import com.tencent.devops.quality.pojo.RuleInterceptHistory
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

@Api(tags = ["SERVICE_RULE_V3"], description = "质量红线-规则v3")
@Path("/service/rules/v3")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceQualityRuleResource {

    @ApiOperation("检测是否通过控制点")
    @Path("/check")
    @POST
    fun check(
        @ApiParam("构建检查参数", required = true)
        buildCheckParams: BuildCheckParamsV3
    ): Result<RuleCheckResult>

    @ApiOperation("创建拦截规则")
    @Path("/project/{projectId}/pipeline/{pipelineId}/create")
    @POST
    fun create(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("规则内容", required = true)
        ruleList: List<RuleCreateRequestV3>
    ): Result<List<RuleCreateResponseV3>>

    @ApiOperation("获取Stream红线列表")
    @Path("/{projectId}/listRuleBuildHis")
    @GET
    fun listQualityRuleBuildHis(
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
}
