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

import com.tencent.bk.codecc.task.vo.BatchRegisterVO;
import com.tencent.bk.codecc.task.vo.ParamJsonAndCheckerSetsVO;
import com.tencent.bk.codecc.task.vo.RepoInfoVO;
import com.tencent.bk.codecc.task.vo.ToolConfigPlatformVO;
import com.tencent.bk.codecc.task.vo.ToolStatusUpdateReqVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.*;

/**
 * 工具管理接口
 *
 * @version V1.0
 * @date 2019/5/7
 */
@Api(tags = {"USER_TOOL"}, description = "工具管理接口")
@Path("/user/tool")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserToolRestResource
{

    @ApiOperation("批量注册工具")
    @Path("/")
    @POST
    Result<Boolean> registerTools(
            @ApiParam(value = "工具注册信息", required = true)
            @Valid
                    BatchRegisterVO batchRegisterVO,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            @NotNull(message = "用户信息不能为空")
                    String userName
    );


    @ApiOperation("获取代码库清单")
    @Path("/repos/projCode/{projCode}")
    @GET
    Result<List<RepoInfoVO>> getRepoList(
            @ApiParam(value = "项目code", required = true)
            @PathParam("projCode")
                    String projCode);


    @ApiOperation("获取代码库分支列表")
    @Path("/branches")
    @GET
    Result<List<String>> listBranches(
            @ApiParam(value = "项目code", required = true)
            @QueryParam("projCode")
                    String projCode,
            @ApiParam(value = "仓库地址", required = true)
            @QueryParam("url")
                    String url,
            @ApiParam(value = "仓库类型", required = true)
            @QueryParam("type")
                    String type
    );


    @ApiOperation("工具启用停用")
    @Path("/status")
    @PUT
    Result<Boolean> updateToolStatus(
            @ApiParam(value = "工具名清单", required = true)
                    ToolStatusUpdateReqVO toolStatusUpdateReqVO,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName,
            @ApiParam(value = "任务id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    long taskId);


    @ApiOperation("停用流水线")
    @Path("/delete/pipeline")
    @DELETE
    Result<Boolean> deletePipeline(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );

    @ApiOperation("修改工具特殊参数和规则集")
    @Path("/tasks/{taskId}/tools/paramJsonAndCheckerSets")
    @PUT
    Result<Boolean> updateParamJsonAndCheckerSets(
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "修改工具特殊参数和规则集请求体", required = true)
                    ParamJsonAndCheckerSetsVO paramJsonAndCheckerSetsVO
    );


    @ApiOperation("修改工具特殊配置")
    @Path("/toolConfig/update")
    @PUT
    Result<Boolean> updateToolPlatformInfo(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName,
            @ApiParam(value = "请求体", required = true)
                ToolConfigPlatformVO toolConfigPlatformVO
    );
}
