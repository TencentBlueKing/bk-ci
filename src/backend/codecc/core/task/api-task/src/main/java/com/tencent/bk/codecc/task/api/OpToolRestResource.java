/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.task.api;

import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigPlatformVO;
import com.tencent.bk.codecc.task.vo.TaskAndToolCountScriptVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * op工具接口
 *
 * @version V1.0
 * @date 2020/3/11
 */
@Api(tags = {"OP_TOOL"}, description = "工具管理接口")
@Path("/op/tool")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface OpToolRestResource
{
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

    @ApiOperation("停用任务")
    @Path("/stop")
    @PUT
    Result<Boolean> stopTask(
            @ApiParam(value = "任务ID", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID) Long taskId,
            @ApiParam(value = "停用原因") String disabledReason,
            @ApiParam(value = "当前用户", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName
    );


    @ApiOperation("启用任务")
    @Path("/start")
    @PUT
    Result<Boolean> startTask(
            @ApiParam(value = "任务ID", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID) Long taskId,
            @ApiParam(value = "当前用户", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName
    );


    @ApiOperation("更新任务组织架构(临时)")
    @Path("/taskOrg/refresh")
    @POST
    Result<Boolean> refreshTaskOrgInfo(
            @ApiParam(value = "当前用户", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName,
            @ApiParam(value = "请求体", required = true) TaskDetailVO reqVO
    );


    @ApiOperation("批量更新工具跟进状态")
    @Path("/followstatus/refresh")
    @GET
    Result<Boolean> refreshToolFollowStatus(
            @ApiParam(value = "当前用户", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName,
            @ApiParam(value = "工具名称") @QueryParam("pageSize") Integer pageSize
    );


    @ApiOperation("批量刷存量用户日志统计")
    @Path("/userlogstat/refresh")
    @GET
    Result<Boolean> intiUserLogInfoStatScript();


    @ApiOperation("仅用于初始化查询工具数量")
    @Path("/initToolCount/sumData")
    @GET
    Result<Boolean> initToolCountScript(
            @ApiParam(value = "查询时间天数") @QueryParam(value = "day") Integer day);


    @ApiOperation("仅用于初始化查询任务数量脚本")
    @Path("/initTaskCount/sumData")
    @GET
    Result<Boolean> initTaskCountScript(
            @ApiParam(value = "查询时间天数") @QueryParam(value = "day") Integer day);


    @ApiOperation("为开源扫描开启微信通知(遗留问题处理人)")
    @Path("/setGongFengNotify")
    @GET
    Result<String> setGongFengNotify(
            @ApiParam(value = "事业部Id")
            @QueryParam(value = "bgId")
            @Nullable
            Integer bgId,
            @ApiParam(value = "部门Id")
            @QueryParam(value = "deptId")
            @Nullable
            Integer deptId,
            @ApiParam(value = "中心Id")
            @QueryParam(value = "centerId")
            @Nullable
            Integer centerId);
}
