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

import com.tencent.bk.codecc.task.vo.BuildEnvVO;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.vo.OpenScanAndEpcToolNameMapVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

/**
 * 元数据的接口类
 *
 * @version V1.0
 * @date 2019/4/19
 */
@Api(tags = {"META"}, description = "元数据查询")
@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserMetaRestResource
{
    @ApiOperation("查询工具列表")
    @Path("/toolList")
    @GET
    Result<List<ToolMetaBaseVO>> toolList(
            @ApiParam(value = "是否查询详细信息")
            @QueryParam("isDetail")
                    Boolean isDetail);

    @ApiOperation("查询工具详情")
    @Path("/toolDetail")
    @GET
    Result<ToolMetaDetailVO> toolDetail(
            @ApiParam(value = "工具名（唯一标志）", required = true)
            @QueryParam("toolName")
                    String toolName);

    @ApiOperation("查询元数据")
    @Path("/metadatas")
    @GET
    Result<Map<String, List<MetadataVO>>> metadatas(
            @ApiParam(value = "元数据类型", required = true)
            @QueryParam("metadataType")
                    String metadataType);

    @ApiOperation("查询编译工具")
    @Path("/buildEnv")
    @GET
    Result<List<BuildEnvVO>> getBuildEnv(
            @ApiParam(value = "操作系统类型", required = true)
            @QueryParam("os")
                    String os);

    @ApiOperation("查询开源治理/EPC对应工具列表映射")
    @Path("/getOpenScanAndEpcToolNameMap")
    @GET
    Result<OpenScanAndEpcToolNameMapVO> getOpenScanAndEpcToolNameMap();
}
