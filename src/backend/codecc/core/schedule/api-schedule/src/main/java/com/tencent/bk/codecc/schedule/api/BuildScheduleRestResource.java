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

package com.tencent.bk.codecc.schedule.api;

import com.tencent.bk.codecc.schedule.vo.TailLogRspVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;

/**
 * 分析服务调度接口
 *
 * @version V2.0
 * @date 2019/09/28
 */
@Api(tags = {"SERVICE_DISPATCH"}, description = "分析服务调度接口")
@Path("/build")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildScheduleRestResource {
    @ApiOperation(value = "推入分析任务")
    @Path("/push/streamName/{streamName}/toolName/{toolName}/buildId/{buildId}")
    @GET
    Result<Boolean> push(
            @ApiParam(value = "任务英文名", required = true)
            @PathParam("streamName")
                    String streamName,
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
                    String toolName,
            @ApiParam(value = "构建ID", required = true)
            @PathParam("buildId")
                    String buildId,
            @ApiParam(value = "构建ID")
            @QueryParam("createFrom")
                    String createFrom,
            @ApiParam(value = "任务所属蓝盾项目", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId);

    @ApiOperation(value = "实时获取日志")
    @Path("/log/streamName/{streamName}/toolName/{toolName}/buildId/{buildId}/beginLine/{beginLine}")
    @GET
    Result<TailLogRspVO> tailLog(
            @ApiParam(value = "任务英文名", required = true)
            @PathParam("streamName")
                    String streamName,
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
                    String toolName,
            @ApiParam(value = "构建ID", required = true)
            @PathParam("buildId")
                    String buildId,
            @ApiParam(value = "开始行", required = true)
            @PathParam("beginLine")
                    long beginLine);
}
