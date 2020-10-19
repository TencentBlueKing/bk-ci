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
import com.tencent.devops.common.api.pojo.CodeCCResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import static com.tencent.devops.common.api.auth.CodeCCHeaderKt.CODECC_AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.CodeCCHeaderKt.CODECC_AUTH_HEADER_DEVOPS_USER_ID;

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
    CodeCCResult<Boolean> updateToolPlatformInfo(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_USER_ID)
                    String userName,
            @ApiParam(value = "请求体", required = true)
                    ToolConfigPlatformVO toolConfigPlatformVO
    );


    @ApiOperation("更新任务组织架构(临时)")
    @Path("/taskOrg/refresh")
    @POST
    CodeCCResult<Boolean> refreshTaskOrgInfo(
            @ApiParam(value = "当前用户", required = true) @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_USER_ID) String userName,
            @ApiParam(value = "请求体", required = true) TaskDetailVO reqVO
    );


    @ApiOperation("批量更新工具跟进状态")
    @Path("/followstatus/refresh")
    @GET
    CodeCCResult<Boolean> refreshToolFollowStatus(
            @ApiParam(value = "当前用户", required = true) @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_USER_ID) String userName,
            @ApiParam(value = "工具名称") @QueryParam("pageSize") Integer pageSize
    );

}
