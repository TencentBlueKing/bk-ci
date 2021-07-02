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

import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.bk.codecc.task.vo.GrayToolReportVO;
import com.tencent.bk.codecc.task.vo.TriggerGrayToolVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Set;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 灰度工具项目调用接口
 *
 * @version V2.0
 * @date 2020/12/29
 */
@Api(tags = {"GRAY_TOOL_PROJECT"}, description = "灰度工具项目调用接口")
@Path("/build/gray/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildGrayToolProjectRestResource {
    @ApiOperation("灰度项目数据更新")
    @Path("/pool/toolName/{toolName}")
    @POST
    Result<Boolean> createGrayTaskPool(
        @ApiParam(value = "工具名", required = true)
        @PathParam("toolName")
            String toolName,
        @ApiParam(value = "工具名", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String user);

    @ApiOperation("触发灰度池项目")
    @Path("/pool/trigger/toolName/{toolName}")
    @POST
    Result<TriggerGrayToolVO> triggerGrayTaskPool(
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
            String toolName);

    @ApiOperation("查询灰度报告")
    @Path("/report/toolName/{toolName}/codeccBuildId/{codeccBuildId}")
    @GET
    Result<GrayToolReportVO> findGrayReportByToolNameAndCodeCCBuildId(
        @ApiParam(value = "工具名", required = true)
        @PathParam("toolName")
            String toolName,
        @ApiParam(value = "codecc构建id", required = true)
        @PathParam("codeccBuildId")
            String codeccBuildId);

    @ApiOperation("查询任务清单")
    @Path("/task/list/toolName/{toolName}")
    @GET
    Result<Set<Long>> findTaskListByToolName(
        @ApiParam(value = "工具名", required = true)
        @PathParam("toolName")
            String toolName);
}
