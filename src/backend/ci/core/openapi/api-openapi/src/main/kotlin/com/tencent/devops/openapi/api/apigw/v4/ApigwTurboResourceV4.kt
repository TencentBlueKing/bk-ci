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
package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.turbo.pojo.TurboRecordModel
import com.tencent.devops.turbo.vo.TurboPlanDetailVO
import com.tencent.devops.turbo.vo.TurboPlanStatRowVO
import com.tencent.devops.turbo.vo.TurboRecordHistoryVO
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

@Api(tags = ["OPENAPI_TURBO_V4"], description = "编译加速open api接口")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/turbo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ApigwTurboResourceV4 {

    @GET
    @ApiOperation("获取方案列表", tags = ["v4_app_turbo_plan_list", "v4_user_turbo_plan_list"])
    @Path("/projectId/{projectId}/turbo_plan_list")
    fun getTurboPlanByProjectIdAndCreatedDate(
        @ApiParam(value = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "开始日期", required = false)
        @QueryParam("startTime")
        startTime: String?,
        @ApiParam(value = "结束日期", required = false)
        @QueryParam("endTime")
        endTime: String?,
        @ApiParam(value = "页数", required = false)
        @QueryParam(value = "pageNum")
        pageNum: Int?,
        @ApiParam(value = "每页多少条", required = false)
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam(value = "用户信息", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Response<Page<TurboPlanStatRowVO>>

    @POST
    @ApiOperation("获取加速历史列表", tags = ["v4_app_turbo_history_list", "v4_user_turbo_history_list"])
    @Path("/projectId/{projectId}/history_list")
    fun getTurboRecordHistoryList(
        @ApiParam(value = "页数", required = false)
        @QueryParam(value = "pageNum")
        pageNum: Int?,
        @ApiParam(value = "每页多少条", required = false)
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam(value = "排序字段", required = false)
        @QueryParam("sortField")
        sortField: String?,
        @ApiParam(value = "排序类型", required = false)
        @QueryParam("sortType")
        sortType: String?,
        @ApiParam(value = "编译加速历史请求数据信息", required = true)
        turboRecordModel: TurboRecordModel,
        @ApiParam(value = "蓝盾项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "用户信息", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Response<Page<TurboRecordHistoryVO>>

    @GET
    @ApiOperation("获取加速方案详情", tags = ["v4_app_turbo_plan_detail", "v4_user_turbo_plan_detail"])
    @Path("/projectId/{projectId}/turbo_plan_detail")
    fun getTurboPlanDetailByPlanId(
        @ApiParam(value = "方案id", required = true)
        @QueryParam("planId")
        planId: String,
        @ApiParam(value = "蓝盾项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "用户信息", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Response<TurboPlanDetailVO>
}
