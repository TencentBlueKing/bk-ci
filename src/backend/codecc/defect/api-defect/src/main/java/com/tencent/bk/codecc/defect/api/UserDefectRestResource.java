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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.GetFileContentSegmentReqVO;
import com.tencent.bk.codecc.defect.vo.common.*;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.data.domain.Sort;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import static com.tencent.devops.common.api.auth.CodeCCHeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;

/**
 * lint类告警查询服务
 */
@Api(tags = {"USER_WARN"}, description = "lint类告警查询服务接口")
@Path("/user/warn")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserDefectRestResource
{
    @ApiOperation("初始化告警管理页面的缺陷类型、作者以及树")
    @Path("/checker/authors/toolName/{toolName}")
    @GET
    Result<QueryWarningPageInitRspVO> queryCheckersAndAuthors(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "工具名称", required = true)
            @PathParam(value = "toolName")
                    String toolName
    );


    @ApiOperation("查询告警详情清单")
    @Path("/list")
    @POST
    Result<CommonFileQueryRspVO> queryFileList(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "查询参数详情", required = true)
            @Valid
                    CommonFileQueryReqVO commonFileQueryReqVO,
            @ApiParam(value = "页数")
            @QueryParam(value = "pageNum")
                    Integer pageNum,
            @ApiParam(value = "页面大小")
            @QueryParam(value = "pageSize")
                    Integer pageSize,
            @ApiParam(value = "排序字段")
            @QueryParam(value = "sortField")
                    String sortField,
            @ApiParam(value = "排序方式")
            @QueryParam(value = "sortType")
                    Sort.Direction sortType);


    @ApiOperation("查询告警详情")
    @Path("/detail")
    @POST
    Result<CommonDefectQueryRspVO> queryDefectDetail(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "查询参数详情", required = true)
            @Valid
                    CommonDefectQueryReqVO commonDefectQueryReqVO,
            @ApiParam(value = "排序字段")
            @QueryParam(value = "sortField")
                    String sortField,
            @ApiParam(value = "排序方式")
            @QueryParam(value = "sortType")
                    Sort.Direction sortType);

    @ApiOperation("获取文件片段")
    @Path("/fileContentSegment")
    @POST
    Result<CommonDefectQueryRspVO> getFileContentSegment(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "获取文件片段", required = true)
            @Valid
                    GetFileContentSegmentReqVO getFileContentSegmentReqVO);

    @ApiOperation("作者批量转换")
    @Path("/author")
    @PUT
    Result<Boolean> authorTransfer(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "作者批量转换信息", required = true)
            @Valid
                    CommonAuthorTransVO commonAuthorTransVO);


}
