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

import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.IgnoreCheckerVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

import static com.tencent.devops.common.api.auth.CodeCCHeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

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
    @Path("/opened")
    @POST
    Result<Map<String, CheckerDetailVO>> queryOpenChecker(
            @ApiParam(value = "工具信息", required = true)
                    ToolConfigInfoVO toolConfigInfoVO);

    @ApiOperation("获取全量的规则")
    @Path("/all")
    @POST
    Result<List<CheckerDetailVO>> queryAllChecker(
            @ApiParam(value = "工具特殊参数信息")
                    ToolConfigInfoVO toolConfigInfoVO);

    @ApiOperation("添加忽略规则")
    @Path("/ignored/taskId/{taskId}/toolName/{toolName}")
    @PUT
    Result<Boolean> mergeIgnoreChecker(
            @ApiParam(value = "任务id")
            @PathParam("taskId")
            long taskId,
            @ApiParam(value = "工具名")
            @PathParam("toolName")
            String toolName,
            @ApiParam(value = "忽略规则")
            List<String> ignoreCheckers);


    @ApiOperation("新建默认忽略规则")
    @Path("/ignored")
    @POST
    Result<Boolean> createDefaultIgnoreChecker(
            @ApiParam(value = "忽略规则信息")
            IgnoreCheckerVO ignoreCheckerVO,
            @ApiParam(value = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName);

    @ApiOperation("新建默认忽略规则")
    @Path("/ignored/taskId/{taskId}/toolName/{toolName}")
    @GET
    Result<IgnoreCheckerVO> getIgnoreCheckerInfo(
            @ApiParam(value = "任务id")
            @PathParam("taskId")
            Long taskId,
            @ApiParam(value = "工具名")
            @PathParam("toolName")
            String toolName);



}
