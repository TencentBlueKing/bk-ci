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

import com.tencent.bk.codecc.defect.vo.CheckerPkgRspVO;
import com.tencent.bk.codecc.defect.vo.ConfigCheckersPkgReqVO;
import com.tencent.bk.codecc.defect.vo.checkerset.AddCheckerSet2TaskReqVO;
import com.tencent.bk.codecc.defect.vo.checkerset.GetCheckerSetsReqVO;
import com.tencent.bk.codecc.defect.vo.checkerset.PipelineCheckerSetVO;
import com.tencent.bk.codecc.defect.vo.checkerset.UserCheckerSetsVO;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.bk.codecc.task.vo.checkerset.ClearTaskCheckerSetReqVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 多工具规则接口
 *
 * @version V1.0
 * @date 2019/5/23
 */
@Api(tags = {"SERVICE_CHECKER"}, description = "多工具规则接口")
@Path("/service/checker")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceCheckerRestResource
{

    @ApiOperation("获取打开的规则")
    @Path("/tasks/{taskId}/tools/{toolName}/configuration")
    @POST
    Result<List<CheckerPkgRspVO>> queryCheckerConfiguration(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId,
            @ApiParam(value = "工具名称", required = true)
            @PathParam(value = "toolName")
                    String toolName,
            @ApiParam(value = "项目语言")
            @QueryParam(value = "codeLang")
                    Long codeLang,
            @ApiParam(value = "工具配置信息", required = true)
                    ToolConfigInfoVO toolConfig);


    @ApiOperation("打开或者关闭配置规则包")
    @Path("/tasks/{taskId}/toolName/{toolName}/checkers/configuration")
    @POST
    Result<Boolean> configCheckerPkg(
            @ApiParam(value = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @ApiParam(value = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName,
            @ApiParam(value = "配置规则包参数", required = true)
                    ConfigCheckersPkgReqVO packageVo
    );

    @ApiOperation("任务关联规则集")
    @Path("/tasks/{taskId}/checkerSets/relationship")
    @POST
    Result<Boolean> addCheckerSet2Task(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @ApiParam(value = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @ApiParam(value = "任务关联规则集请求参数", required = true)
                    AddCheckerSet2TaskReqVO addCheckerSet2TaskReqVO
    );

    @ApiOperation("查询规则集列表")
    @Path("/tasks/{taskId}/checkerSets")
    @POST
    Result<UserCheckerSetsVO> getCheckerSets(
            @ApiParam(value = "查询规则集列表请求参数", required = true)
                    GetCheckerSetsReqVO getCheckerSetsReqVO,
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId
    );

    @ApiOperation("查询规则集列表")
    @Path("/tools/{toolName}/pipelineCheckerSets")
    @GET
    Result<PipelineCheckerSetVO> getPipelineCheckerSets(
            @ApiParam(value = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName,
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId
    );

    @ApiOperation("清除任务和工具关联的规则集")
    @Path("/tasks/{taskId}/checkerSets/relationships")
    @DELETE
    Result<Boolean> clearCheckerSet(
            @ApiParam(value = "任务id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @ApiParam(value = "清除规则集ID请求体", required = true)
                    ClearTaskCheckerSetReqVO clearTaskCheckerSetReqVO,
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user
    );

}
