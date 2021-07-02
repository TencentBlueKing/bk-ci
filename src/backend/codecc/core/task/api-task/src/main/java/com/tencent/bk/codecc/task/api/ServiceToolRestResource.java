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

package com.tencent.bk.codecc.task.api;

import com.tencent.bk.codecc.task.vo.*;
import com.tencent.bk.codecc.task.vo.checkerset.UpdateCheckerSet2TaskReqVO;
import com.tencent.bk.codecc.task.vo.checkerset.ClearTaskCheckerSetReqVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineBuildInfoVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * task interface
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Api(tags = {"SERVICE_TOOL"}, description = "工具管理接口")
@Path("/service/tool")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceToolRestResource
{
    @ApiOperation("更新工具分析步骤及状态")
    @Path("/")
    @PUT
    Result updateToolStepStatus(
            @ApiParam(value = "需要更新的工具基本信息", required = true)
                    ToolConfigBaseVO toolConfigBaseVO
    );


    @ApiOperation("根据任务id获取工具信息")
    @Path("/tool/{toolName}")
    @GET
    Result<ToolConfigInfoVO> getToolByTaskIdAndName(
            @ApiParam(value = "任务id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    long taskId,
            @ApiParam(value = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName);


    @ApiOperation("根据任务id获取带名称的工具信息")
    @Path("/tool/name/{toolName}")
    @GET
    Result<ToolConfigInfoWithMetadataVO> getToolWithMetadataByTaskIdAndName(
            @ApiParam(value = "任务id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    long taskId,
            @ApiParam(value = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName);


    @ApiOperation("获取工具顺序")
    @Path("/order")
    @GET
    Result<String> findToolOrder();

    @ApiOperation("更新流水线工具配置")
    @Path("/pipeline/tools")
    @PUT
    Result<Boolean> updatePipelineTool(
            @ApiParam(value = "任务id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName,
            @ApiParam(value = "工具信息", required = true)
                    List<String> toolList);

    @ApiOperation("清除任务和工具关联的规则集")
    @Path("/tasks/{taskId}/checkerSets/relationships")
    @DELETE
    Result<Boolean> clearCheckerSet(
            @ApiParam(value = "任务id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @ApiParam(value = "清除规则集ID请求体", required = true)
                    ClearTaskCheckerSetReqVO clearTaskCheckerSetReqVO);

    @ApiOperation("设置任务和工具关联的规则集")
    @Path("/tasks/{taskId}/checkerSets/relationships")
    @POST
    Result<Boolean> addCheckerSet2Task(
            @ApiParam(value = "任务id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @ApiParam(value = "设置规则集ID请求体", required = true)
                    UpdateCheckerSet2TaskReqVO addCheckerSet2TasklReqVO);

    @ApiOperation("获取分析配置信息")
    @Path("/config/streamName/{streamName}/toolType/{toolName}")
    @POST
    Result<AnalyzeConfigInfoVO> getAnalyzeConfig(
            @ApiParam(value = "任务英文名", required = true)
            @PathParam("streamName")
                    String streamName,
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
                    String toolName,
            @ApiParam(value = "上传分析任务详情", required = true)
                    PipelineBuildInfoVO pipelineBuildInfoVO
    );

    @ApiOperation("获取分析配置信息")
    @Path("/tasks/{taskId}/toolConfiguration")
    @POST
    Result<Boolean> updateTools(
            @ApiParam(value = "任务id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @ApiParam(value = "更新工具配置请求体", required = true)
                    BatchRegisterVO batchRegisterVO
    );

    @ApiOperation("批量查询工具配置信息")
    @Path("/batch/toolConfig/list")
    @POST
    Result<List<ToolConfigInfoVO>> batchGetToolConfigList(
            @ApiParam(value = "任务批量查询模型", required = true)
                    QueryTaskListReqVO queryTaskListReqVO
    );

}
