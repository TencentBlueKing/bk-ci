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

package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.ToolClocRspVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectRspVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.openapi.TaskOverviewDetailRspVO;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.data.domain.Sort;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import static com.tencent.devops.common.api.auth.CodeCCHeaderKt.CODECC_AUTH_HEADER_DEVOPS_TASK_ID;

/**
 * 告警相关接口
 * 
 * @date 2019/11/15
 * @version V1.0
 */
@Api(tags = {"SERVICE_PKGDEFECT"}, description = "告警相关接口")
@Path("/service/pkgDefect")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceOpenSourcePkgDefectRestResource {

    @ApiOperation("批量统计任务告警概览情况")
    @Path("/statistics/overview")
    @POST
    CodeCCResult<TaskOverviewDetailRspVO> queryTaskOverview(
        @ApiParam(value = "查询参数详情", required = true) @Valid DeptTaskDefectReqVO deptTaskDefectReqVO,
        @ApiParam(value = "页数") @QueryParam(value = "pageNum") Integer pageNum,
        @ApiParam(value = "每页数量") @QueryParam(value = "pageSize") Integer pageSize,
        @ApiParam(value = "排序类型") @QueryParam(value = "sortType") Sort.Direction sortType);

    @ApiOperation("批量获取个性化任务告警概览情况")
    @Path("/statistics/custom")
    @GET
    CodeCCResult<TaskOverviewDetailRspVO> queryCustomTaskOverview(
        @ApiParam(value = "个性化任务创建来源") @QueryParam(value = "customProjSource") String customProjSource,
        @ApiParam(value = "页数") @QueryParam(value = "pageNum") Integer pageNum,
        @ApiParam(value = "每页数量") @QueryParam(value = "pageSize") Integer pageSize,
        @ApiParam(value = "排序类型") @QueryParam(value = "sortType") Sort.Direction sortType);


}
