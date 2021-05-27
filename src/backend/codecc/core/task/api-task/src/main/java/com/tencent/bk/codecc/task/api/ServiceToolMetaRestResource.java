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

import com.tencent.devops.common.api.RefreshDockerImageHashReqVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * 工具元数据注册接口
 *
 * @version V2.0
 * @date 2020/4/8
 */
@Api(tags = {"SERVICE_TOOL_META"}, description = "工具元数据注册接口")
@Path("/service/toolmeta")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceToolMetaRestResource {

    @ApiOperation("工具类型验证")
    @Path("/tool/{toolType}/validate")
    @GET
    Result<Boolean> validateToolType(
            @PathParam("toolType")
                    String toolType
    );

    @ApiOperation("工具支持语言验证")
    @Path("/language/validate")
    @POST
    Result<Boolean> validateLanguage(List<String> languages);

    @ApiOperation("获取工具元数据")
    @Path("/tool/toolName/{toolName}")
    @GET
    Result<ToolMetaDetailVO> obtainToolDetail(
            @PathParam("toolName")
                    String toolName
    );

    @ApiOperation("刷新工具docker镜像的hash值")
    @Path("/dockerImageHash")
    @POST
    Result<Boolean> refreshDockerImageHash(RefreshDockerImageHashReqVO refreshDockerImageHashReqVO);
}
