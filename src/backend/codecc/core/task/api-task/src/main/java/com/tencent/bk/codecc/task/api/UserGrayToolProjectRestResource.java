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
import com.tencent.bk.codecc.task.vo.TriggerGrayToolVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.bk.codecc.task.vo.GrayToolReportVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;


import javax.validation.Valid;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;

/**
 * 灰度工具项目调用接口
 *
 * @version V2.0
 * @date 2020/12/29
 */
@Api(tags = {"GRAY_TOOL_PROJECT"}, description = "灰度工具项目调用接口")
@Path("/user/gray/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserGrayToolProjectRestResource {
    @ApiOperation("灰度项目注册")
    @Path("/projects/register")
    @POST
    Result<Boolean> register(
            @ApiParam(value = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId,
            @ApiParam(value = "灰度项目数据信息", required = true)
                    GrayToolProjectVO grayToolProjectVO
    );

    @ApiOperation("灰度项目数据查询列表")
    @Path("/projects/list")
    @POST
    Result<Page<GrayToolProjectVO>> queryGrayToolProjectList(
            @ApiParam(value = "按灰度项目查询任务告警请求", required = true) @Valid
                    GrayToolProjectVO grayToolProjectVO,
            @ApiParam(value = "页数") @QueryParam(value = "pageNum") Integer pageNum,
            @ApiParam(value = "每页多少条") @QueryParam(value = "pageSize") Integer pageSize,
            @ApiParam(value = "排序字段") @QueryParam(value = "sortField") String sortField,
            @ApiParam(value = "排序类型") @QueryParam(value = "sortType") String sortType
    );

    @ApiOperation("灰度项目数据更新")
    @Path("/projects/update")
    @POST
    Result<Boolean> updateGrayToolProject(
            @ApiParam(value = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId,
            @ApiParam(value = "灰度项目数据信息", required = true)
                    GrayToolProjectVO grayToolProjectVO
    );

    @ApiOperation("灰度项目数据查询")
    @Path("/projects/findByProjectId")
    @GET
    Result<GrayToolProjectVO> findByProjectId(
            @ApiParam(value = "灰度项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId
    );

    @ApiOperation("创建灰度池")
    @Path("/task/pool/toolName/{toolName}")
    @POST
    Result<Boolean> createGrayTaskPool(
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
            String toolName,
            @ApiParam(value = "工具名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String user);

    @ApiOperation("触发灰度池项目")
    @Path("/task/pool/trigger/toolName/{toolName}")
    @POST
    Result<TriggerGrayToolVO> triggerGrayTaskPool(
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
            String toolName);

    @ApiOperation("根据工具名查询灰度项目信息")
    @Path("/project/toolName/{toolName}")
    @GET
    Result<GrayToolProjectVO> findGrayToolProjInfoByToolName(
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
                    String toolName);
}
