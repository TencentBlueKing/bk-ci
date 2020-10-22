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

import com.tencent.devops.common.api.pojo.CodeCCResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * 任务接口
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Api(tags = {"USER_TASK"}, description = "任务管理接口")
@Path("/user/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserTaskOpenScanResource {
    @ApiOperation("保存定制化报告信息")
    @Path("/dataSynchronization")
    @GET
    CodeCCResult<Boolean> syncKafkaTaskInfo(
            @ApiParam(value = "是否首次触发")
            @QueryParam("dataType")
                    String dataType,
            @ApiParam(value = "是否首次触发")
            @QueryParam("washTime")
                    String washTime
    );

    @ApiOperation("手动触发流水线")
    @Path("/manual/pipeline/trigger")
    @POST
    CodeCCResult<Boolean> manualTriggerPipeline(
            @ApiParam(value = "任务id清单")
                    List<Long> taskIdList);

    @ApiOperation("动态添加开源扫描任务")
    @Path("/openScan/startPage/{startPage}/endPage/{endPage}/startHour/{startHour}/startMinute/{startMinute}")
    @POST
    CodeCCResult<Boolean> extendGongfengScanRange(
        @ApiParam(value = "开始页面", required = true)
        @PathParam("startPage")
            Integer startPage,
        @ApiParam(value = "结束页面", required = true)
        @PathParam("endPage")
            Integer endPage,
        @ApiParam(value = "开始小时数", required = true)
        @PathParam("startHour")
            Integer startHour,
        @ApiParam(value = "开始分钟数", required = true)
        @PathParam("startMinute")
            Integer startMinute);

    @ApiOperation("触发蓝盾插件打分任务")
    @Path("/bkplugin/trigger")
    @GET
    CodeCCResult<Boolean> triggerBkPluginScoring();
}
