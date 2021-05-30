/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.apiquery.api;

import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.bk.codecc.apiquery.vo.ToolAnalyzeStatVO;
import com.tencent.bk.codecc.apiquery.vo.ToolAnalyzeVO;
import com.tencent.bk.codecc.apiquery.vo.ToolRegisterStatisticsVO;
import com.tencent.bk.codecc.apiquery.vo.ToolRegisterVO;
import com.tencent.bk.codecc.apiquery.vo.op.TaskAndToolStatChartVO;
import com.tencent.bk.codecc.apiquery.vo.op.ToolElapseTimeVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

/**
 * op工具接口实现
 */
@Api(tags = {"OP_TOOL"}, description = "工具管理接口")
@Path("/op/tool")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface OpToolRestResource {

    @ApiOperation("获取工具注册明细信息")
    @Path("/register/detail")
    @POST
    Result<Page<ToolRegisterVO>> getToolRegisterInfoList(
            @ApiParam(value = "工具注册明细请求体") @Valid TaskToolInfoReqVO reqVO,
            @ApiParam(value = "页数") @QueryParam(value = "pageNum") Integer pageNum,
            @ApiParam(value = "每页多少条") @QueryParam(value = "pageSize") Integer pageSize,
            @ApiParam(value = "排序字段") @QueryParam(value = "sortField") String sortField,
            @ApiParam(value = "排序类型") @QueryParam(value = "sortType") String sortType);

    @ApiOperation("获取工具注册统计信息")
    @Path("/register/stat")
    @POST
    Result<List<ToolRegisterStatisticsVO>> getToolRegisterStatisticsList(
            @ApiParam(value = "任务工具信息请求体", required = true) @Valid TaskToolInfoReqVO taskToolInfoReqVO);

    @ApiOperation("获取工具数量和活跃工具数量的折线图数据")
    @Path("/toolStatistic")
    @POST
    Result<List<TaskAndToolStatChartVO>> toolAndActiveToolStatData(
            @ApiParam(value = "任务工具信息请求体") @Valid TaskToolInfoReqVO reqVO);

    @ApiOperation("获取工具分析次数折线图数据")
    @Path("/analyzeStatistic")
    @POST
    Result<Map<String, List<TaskAndToolStatChartVO>>> toolAnalyzeCountData(
            @ApiParam(value = "任务工具信息请求体") @Valid TaskToolInfoReqVO reqVO);

    @ApiOperation("获取工具执行统计数据")
    @Path("/toolAnalyzeStatList")
    @POST
    Result<List<ToolAnalyzeStatVO>> toolAnalyzeStatData(
            @ApiParam(value = "任务工具信息请求体") @Valid TaskToolInfoReqVO reqVO);

    @ApiOperation("获取工具执行明细信息")
    @Path("/analyze/detail")
    @POST
    Result<Page<ToolAnalyzeVO>> getToolAnalyzeInfoList(@ApiParam(value = "工具执行明细请求体") @Valid TaskToolInfoReqVO reqVO,
            @ApiParam(value = "页数") @QueryParam(value = "pageNum") Integer pageNum,
            @ApiParam(value = "每页多少条") @QueryParam(value = "pageSize") Integer pageSize,
            @ApiParam(value = "排序字段") @QueryParam(value = "sortField") String sortField,
            @ApiParam(value = "排序类型") @QueryParam(value = "sortType") String sortType);

    @ApiOperation("获取工具分析耗时趋势图")
    @Path("/elapseTime/chart")
    @POST
    Result<Map<String, List<ToolElapseTimeVO>>> getToolElapseTimeChart(
            @ApiParam(value = "任务批量查询模型", required = true) TaskToolInfoReqVO reqVO
    );

}
