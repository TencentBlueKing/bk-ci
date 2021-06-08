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

import com.tencent.bk.codecc.defect.vo.UpdateDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadDefectVO;
import com.tencent.devops.common.api.annotation.ServiceInterface;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Set;

/**
 * 后台微服务（如coverity）告警上报服务接口
 *
 * @version V1.0
 * @date 2019/11/2
 */
@Api(tags = {"SERVICE_DEFECT"}, description = "后台微服务（如coverity）告警上报服务接口")
@Path("/service/defects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ServiceInterface(value = "report")
public interface ServiceReportDefectRestResource
{
    @ApiOperation("查询所有的告警ID")
    @Path("/ids/taskId/{taskId}/toolName/{toolName}")
    @GET
    Result<Set<Long>> queryIds(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
                    long taskId,
            @ApiParam(value = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName
    );

    @ApiOperation("批量更新告警状态")
    @Path("/status")
    @PUT
    Result updateDefectStatus(
            @ApiParam(value = "告警状态映射表", required = true)
                    UpdateDefectVO updateDefectVO);

    @ApiOperation("上报告警")
    @Path("/")
    @POST
    Result reportDefects(
            @ApiParam(value = "告警详细信息", required = true)
                    UploadDefectVO uploadDefectVO);

    @ApiOperation("更新告警详情")
    @Path("/update/detail")
    @POST
    Result updateDefects(
            @ApiParam(value = "告警详细信息", required = true)
                    UpdateDefectVO updateDefectVO);
}
