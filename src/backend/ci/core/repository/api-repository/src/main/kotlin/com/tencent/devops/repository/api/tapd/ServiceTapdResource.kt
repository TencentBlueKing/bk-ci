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

package com.tencent.devops.repository.api.tapd

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.sdk.tapd.request.StatusMapRequest
import com.tencent.devops.scm.pojo.tapd.TapdBug
import com.tencent.devops.scm.pojo.tapd.TapdBugFieldConfig
import com.tencent.devops.scm.pojo.tapd.TapdStory
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_TAPD", description = "tapd服务接口")
@Path("/service/tapd")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceTapdResource {

    @Operation(summary = "获取工作流状态中英文名对应关系")
    @POST
    @Path("/getWorkflowStatusMap")
    fun getWorkflowStatusMap(request: StatusMapRequest): Result<Map<String, String>>

    @Operation(summary = "查询 TAPD 需求详情")
    @GET
    @Path("/stories")
    fun getStoryInfo(
        @Parameter(description = "TAPD 项目 ID", required = true)
        @QueryParam("workspaceId")
        workspaceId: String,
        @Parameter(description = "TAPD 需求 ID", required = true)
        @QueryParam("storyId")
        storyId: String
    ): Result<TapdStory?>

    @Operation(summary = "查询 TAPD 缺陷详情")
    @GET
    @Path("/bugs")
    fun getBugInfo(
        @Parameter(description = "TAPD 项目 ID", required = true)
        @QueryParam("workspaceId")
        workspaceId: String,
        @Parameter(description = "TAPD 缺陷 ID", required = true)
        @QueryParam("bugId")
        bugId: String
    ): Result<TapdBug?>

    @Operation(summary = "查询 TAPD 缺陷所有字段及候选值")
    @GET
    @Path("/bugFields")
    fun getBugFieldsInfo(
        @Parameter(description = "TAPD 项目 ID", required = true)
        @QueryParam("workspaceId")
        workspaceId: String
    ): Result<TapdBugFieldConfig?>
}
