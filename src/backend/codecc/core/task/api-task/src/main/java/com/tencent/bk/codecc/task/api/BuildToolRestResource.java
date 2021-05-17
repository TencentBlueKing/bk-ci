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

import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineBuildInfoVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * 构建机工具接口
 *
 * @version V1.0
 * @date 2019/7/21
 */
@Api(tags = {"BUILD_TOOL"}, description = "工具管理接口")
@Path("/build/tool")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildToolRestResource
{

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

}
