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

package com.tencent.devops.metrics.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.metrics.pojo.`do`.AtomExecutionStatisticsInfoDO
import com.tencent.devops.metrics.pojo.vo.AtomStatisticsInfoReqVO
import com.tencent.devops.metrics.pojo.vo.AtomTrendInfoVO
import com.tencent.devops.metrics.pojo.vo.ListPageVO
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_ATOM_STATISTICS", description = "插件-统计信息")
@Path("/user/atom/statistics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserAtomStatisticsResource {
    @Operation(summary = "查询插件趋势信息")
    @Path("/trend/info")
    @POST
    fun queryAtomTrendInfo(
        @Parameter(description = "项目ID", required = true)
        @BkField(required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "userId", required = true)
        @BkField(required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "查询条件", required = true)
        atomStatisticsInfoReq: AtomStatisticsInfoReqVO
    ): Result<AtomTrendInfoVO>

    @Operation(summary = "查询插件执行统计信息")
    @Path("/execute/info")
    @POST
    fun queryAtomExecuteStatisticsInfo(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        @BkField(required = true)
        projectId: String,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(required = true)
        userId: String,
        @Parameter(description = "查询条件", required = true)
        atomStatisticsInfoReq: AtomStatisticsInfoReqVO,
        @Parameter(description = "页码", required = true, example = "1")
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页大小", required = true, example = "10")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE, required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<ListPageVO<AtomExecutionStatisticsInfoDO>>
}
