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

import com.tencent.bk.codecc.apiquery.vo.CodeLineStatisticVO;
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.bk.codecc.apiquery.vo.ToolDefectRspVO;
import com.tencent.bk.codecc.apiquery.vo.op.FileStatusVO;
import com.tencent.bk.codecc.apiquery.vo.op.TaskDefectSummaryVO;
import com.tencent.bk.codecc.apiquery.vo.op.TaskDefectVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * op告警查询接口
 *
 * @version V3.0
 * @date 2020/9/2
 */

@Api(tags = {"OP_DEFECT"}, description = "告警查询接口")
@Path("/op/defect")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface OpDefectRestResource {

    @ApiOperation("运营数据:按条件获取任务告警统计信息")
    @Path("/deptTaskDefect")
    @POST
    Result<Page<TaskDefectVO>> queryDeptTaskDefect(
            @ApiParam(value = "用户名", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName,
            @ApiParam(value = "按组织架构查询任务告警请求", required = true) @Valid TaskToolInfoReqVO reqVO,
            @ApiParam(value = "页数") @QueryParam(value = "pageNum") Integer pageNum,
            @ApiParam(value = "每页多少条") @QueryParam(value = "pageSize") Integer pageSize,
            @ApiParam(value = "排序字段") @QueryParam(value = "sortField") String sortField,
            @ApiParam(value = "排序类型") @QueryParam(value = "sortType") String sortType
    );

    @ApiOperation("按条件批量获取告警信息列表")
    @Path("/detail/list")
    @POST
    Result<ToolDefectRspVO> queryDeptDefectList(
            @ApiParam(value = "任务ID", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName,
            @ApiParam(value = "按组织架构查询任务告警请求", required = true) @Valid TaskToolInfoReqVO reqVO,
            @ApiParam(value = "页数") @QueryParam(value = "pageNum") Integer pageNum,
            @ApiParam(value = "页面大小") @QueryParam(value = "pageSize") Integer pageSize
    );

    @ApiOperation("获取代码总量和每日分析代码总量趋势图的数据")
    @Path("/codeLineStatistic")
    @POST
    Result<List<CodeLineStatisticVO>> codeLineTotalAndCodeLineDailyStatData(
            @ApiParam(value = "代码总量信息请求体") TaskToolInfoReqVO reqVO);


    @ApiOperation("运营数据:按条件获取任务告警统计信息")
    @Path("/dimension/sum")
    @POST
    Result<Page<TaskDefectSummaryVO>> queryTaskDefectSumPage(
            @ApiParam(value = "用户名", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName,
            @ApiParam(value = "按组织架构查询任务告警请求", required = true) @Valid TaskToolInfoReqVO reqVO,
            @ApiParam(value = "页数") @QueryParam(value = "pageNum") Integer pageNum,
            @ApiParam(value = "每页多少条") @QueryParam(value = "pageSize") Integer pageSize,
            @ApiParam(value = "排序字段") @QueryParam(value = "sortField") String sortField,
            @ApiParam(value = "排序类型") @QueryParam(value = "sortType") String sortType
    );

    @ApiOperation("导出运营数据:按维度统计任务告警信息")
    @Path("/dimension/export")
    @POST
    Result<String> exportDimensionToFile(
            @ApiParam(value = "用户名", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName,
            @ApiParam(value = "按组织架构查询任务告警请求", required = true) @Valid TaskToolInfoReqVO reqVO
    );

    @ApiOperation("查询导出接口状态是否繁忙")
    @Path("/dimension/export/flag")
    @GET
    Result<String> queryDimensionExportFlag();

    @ApiOperation("更新导出接口状态")
    @Path("/dimension/export/flag/update")
    @PUT
    Result<String> updateDimensionExportFlag();

    @ApiOperation("获取导出文件的状态(轮询)")
    @Path("/dimension/file/flag")
    @GET
    Result<FileStatusVO> getDimensionFileFlag();

}
