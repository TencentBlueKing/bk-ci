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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tencent.bk.codecc.task.enums.TaskSortType;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.bk.codecc.task.vo.FilterPathOutVO;
import com.tencent.bk.codecc.task.vo.NotifyCustomVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.TaskCodeLibraryVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskIdVO;
import com.tencent.bk.codecc.task.vo.TaskListReqVO;
import com.tencent.bk.codecc.task.vo.TaskListVO;
import com.tencent.bk.codecc.task.vo.TaskMemberVO;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO;
import com.tencent.bk.codecc.task.vo.TaskOwnerAndMemberVO;
import com.tencent.bk.codecc.task.vo.TaskStatusVO;
import com.tencent.bk.codecc.task.vo.TaskUpdateVO;
import com.tencent.bk.codecc.task.vo.TreeNodeTaskVO;
import com.tencent.bk.codecc.task.vo.path.CodeYmlFilterPathVO;
import com.tencent.bk.codecc.task.vo.perm.MgrResourceResult;
import com.tencent.bk.codecc.task.vo.scanconfiguration.ScanConfigurationVO;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import static com.tencent.devops.common.api.auth.CodeCCHeaderKt.CODECC_AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.CodeCCHeaderKt.CODECC_AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.CodeCCHeaderKt.CODECC_AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 任务权限接口
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Api(tags = {"USER_PERMISSION"}, description = "任务权限接口")
@Path("/user/permission")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserTaskPermissionRestResource {

    @ApiOperation("")
    @Path("/project/{projectId}/mgr/resource/list")
    @GET
    CodeCCResult<MgrResourceResult> getMgrResource(
        @ApiParam(value = "", required = true)
        @PathParam("projectId")
            String projectId,
        @ApiParam(value = "", required = true)
        @QueryParam("resourceTypeCode")
            String resourceTypeCode,
        @ApiParam(value = "", required = true)
        @QueryParam("resourceCode")
            String resourceCode);
}
