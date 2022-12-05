/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

import com.tencent.bk.codecc.defect.vo.QueryTaskLogVO;
import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.bk.codecc.task.vo.QueryLogRepVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import org.springframework.data.domain.PageImpl;

/**
 * task interface
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Api(tags = {"TASK_LOG"}, description = "任务分析记录接口")
@Path("/user/tasklog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserTaskLogRestResource
{

    @ApiOperation("获取分析记录")
    @Path("/")
    @GET
    Result<QueryTaskLogVO> getTaskLogs(
            @ApiParam(value = "工具名称", required = true)
            @QueryParam("toolName")
                    String toolName,
            @ApiParam(value = "第几页", required = true)
            @QueryParam("page")
                    int page,
            @ApiParam(value = "每页多少条", required = true)
            @QueryParam("pageSize")
                    int pageSize
    );

    @ApiOperation("获取分析记录 V2 接口")
    @Path("/overview")
    @GET
    Result<PageImpl<TaskLogOverviewVO>> getTaskLogs(
            @ApiParam(value = "任务ID")
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "第几页")
            @QueryParam("page")
                    Integer page,
            @ApiParam(value = "每页多少条")
            @QueryParam("pageSize")
                    Integer pageSize
    );

    @ApiOperation("获取分析记录日志")
    @Path("/analysis/logs/{projectId}/{pipelineId}/{buildId}")
    @GET
    Result<QueryLogRepVO> getAnalysisLogs(
            @ApiParam(value = "用户ID")
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId,
            @ApiParam(value = "项目ID", required = true)
            @PathParam("projectId")
                    String projectId,
            @ApiParam(value = "流水线ID", required = true)
            @PathParam("pipelineId")
                    String pipelineId,
            @ApiParam(value = "构建号ID", required = true)
            @PathParam("buildId")
                    String buildId,
            @ApiParam(value = "搜索关键字")
            @QueryParam("queryKeywords")
                    String queryKeywords,
            @ApiParam(value = "对应elementId")
            @QueryParam("tag")
                    String tag
    );

    @ApiOperation("获取更多日志")
    @GET
    @Path("/analysis/logs/{projectId}/{pipelineId}/{buildId}/more")
    // NOCC:ParameterNumber(设计如此:)
    Result<QueryLogRepVO> getMoreLogs(
            @ApiParam(value = "用户ID")
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId,
            @ApiParam(value = "项目ID", required = true)
            @PathParam("projectId")
                    String projectId,
            @ApiParam(value = "流水线ID", required = true)
            @PathParam("pipelineId")
                    String pipelineId,
            @ApiParam(value = "构建ID", required = true)
            @PathParam("buildId")
                    String buildId,
            @ApiParam(value = "日志行数")
            @QueryParam("num")
                    Integer num,
            @ApiParam(value = "是否正序输出")
            @QueryParam("fromStart")
                    Boolean fromStart,
            @ApiParam(value = "起始行号", required = true)
            @QueryParam("start")
                    Long start,
            @ApiParam(value = "结尾行号", required = true)
            @QueryParam("end")
                    Long end,
            @ApiParam(value = "对应elementId")
            @QueryParam("tag")
                    String tag,
            @ApiParam(value = "执行次数")
            @QueryParam("executeCount")
                    Integer executeCount
    );


    @ApiOperation("下载日志接口")
    @GET
    @Path("/analysis/logs/{projectId}/{pipelineId}/{buildId}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    void downloadLogs(
            @ApiParam(value = "用户ID")
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId,
            @ApiParam(value = "项目ID", required = true)
            @PathParam("projectId")
                    String projectId,
            @ApiParam(value = "流水线ID", required = true)
            @PathParam(value = "pipelineId")
                    String pipelineId,
            @ApiParam(value = "构建ID", required = true)
            @PathParam("buildId")
                    String buildId,
            @ApiParam(value = "对应element ID")
            @QueryParam("tag")
                    String tag,
            @ApiParam(value = "执行次数")
            @QueryParam("executeCount")
                    Integer executeCount
    );


    @ApiOperation("获取某行后的日志")
    @GET
    @Path("/analysis/logs/{projectId}/{pipelineId}/{buildId}/after")
    // NOCC:ParameterNumber(设计如此:)
    Result<QueryLogRepVO> getAfterLogs(
            @ApiParam(value = "用户ID")
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId,
            @ApiParam(value = "项目ID", required = true)
            @PathParam("projectId")
                    String projectId,
            @ApiParam(value = "流水线ID", required = true)
            @PathParam("pipelineId")
                    String pipelineId,
            @ApiParam(value = "构建ID", required = true)
            @PathParam("buildId")
                    String buildId,
            @ApiParam(value = "起始行号", required = true)
            @QueryParam("start")
                    Long start,
            @ApiParam(value = "搜索关键字")
            @QueryParam("queryKeywords")
                    String queryKeywords,
            @ApiParam(value = "对应elementId")
            @QueryParam("tag")
                    String tag,
            @ApiParam(value = "执行次数")
            @QueryParam("executeCount")
                    Integer executeCount
    );


}
