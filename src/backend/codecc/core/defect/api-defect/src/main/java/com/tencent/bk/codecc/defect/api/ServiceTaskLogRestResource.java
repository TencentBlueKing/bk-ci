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

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

import com.tencent.bk.codecc.defect.vo.TaskLogRepoInfoVO;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.GetLastAnalysisResultsVO;
import com.tencent.devops.common.api.analysisresult.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * task interface
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Api(tags = {"SERVICE_TASKLOG"}, description = "工具侧上报任务分析记录接口")
@Path("/service/tasklog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceTaskLogRestResource
{
    @ApiOperation("停止正在运行的任务")
    @Path("/runningTask/pipelineId/{pipelineId}/streamName/{streamName}")
    @POST
    Result<Boolean> stopRunningTask(
            @ApiParam(value = "流水线id", required = true)
            @PathParam("pipelineId")
                    String pipelineId,
            @ApiParam(value = "任务名称", required = true)
            @PathParam("streamName")
                    String streamName,
            @ApiParam(value = "工具清单", required = true)
                    Set<String> toolSet,
            @ApiParam(value = "项目id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    long taskId,
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName);


    @ApiOperation("获取最新分析记录")
    @Path("/latest/toolName/{toolName}/taskId/{taskId}")
    @GET
    Result<TaskLogVO> getLatestTaskLog(
            @ApiParam(value = "任务id", required = true)
            @PathParam("taskId")
                    long taskId,
            @ApiParam(value = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName
    );

    @ApiOperation("平台侧获取任务所有有效工具的最近一次分析结果")
    @Path("/lastAnalysisResults")
    @POST
    Result<List<ToolLastAnalysisResultVO>> getLastAnalysisResults(
            @ApiParam(value = "获取最近一次分析结果的请求对象", required = true)
                    GetLastAnalysisResultsVO getLastAnalysisResultsVO);

    @ApiOperation("平台侧获取任务所有有效工具的某一次分析结果")
    @Path("/analysisResults")
    @POST
    Result<List<ToolLastAnalysisResultVO>> getAnalysisResults(
        @ApiParam(value = "获取某一次分析结果的请求对象", required = true)
            GetLastAnalysisResultsVO getLastAnalysisResultsVO);

    @ApiOperation("获取最近统计信息")
    @Path("/lastStatisticResult")
    @POST
    Result<BaseLastAnalysisResultVO> getLastStatisticResult(
            @ApiParam(value = "获取最近统计信息的请求对象", required = true)
                    ToolLastAnalysisResultVO toolLastAnalysisResultVO);


    @ApiOperation("批量获取最新分析记录")
    @Path("/latest/batch/taskId/{taskId}")
    @POST
    Result<List<ToolLastAnalysisResultVO>> getBatchLatestTaskLog(
            @ApiParam(value = "任务id", required = true)
            @PathParam("taskId")
                    long taskId,
            @ApiParam(value = "工具名称清单", required = true)
                    Set<String> toolSet);

    @ApiOperation("任务维度批量获取最新分析记录")
    @Path("/latest/batchTask")
    @POST
    Result<Map<String, List<ToolLastAnalysisResultVO>>> getBatchTaskLatestTaskLog(
            @ApiParam(value = "任务id及工具集映射参数", required = true)
                    List<TaskDetailVO> taskDetailVOList);

    @ApiOperation("批量获取最新分析记录")
    @Path("/suggest/param")
    @PUT
    Result<Boolean> uploadDirStructSuggestParam(
            @ApiParam(value = "上传参数建议值信息", required = true)
                    UploadTaskLogStepVO uploadTaskLogStepVO);


    @ApiOperation("批量获取最新分析记录")
    @Path("/pipeline")
    @PUT
    Result<Boolean> refreshTaskLogByPipeline(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @ApiParam(value = "工具集合", required = true)
                    Set<String> toolNames);

    @ApiOperation("批量获取最新分析记录")
    @Path("/latest/repo")
    @PUT
    Result<Map<String, TaskLogRepoInfoVO>> getLastAnalyzeRepoInfo(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId);
}
