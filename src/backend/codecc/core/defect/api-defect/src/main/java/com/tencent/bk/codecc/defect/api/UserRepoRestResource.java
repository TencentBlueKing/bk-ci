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

package com.tencent.bk.codecc.defect.api;

import com.tencent.devops.common.api.CodeRepoVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Set;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 代码库前端接口
 *
 * @date 2019/12/23
 * @version V1.0
 */
@Api(tags = {"USER_REPO"}, description = "代码库前端接口")
@Path("/user/repo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserRepoRestResource
{
    @ApiOperation("根据任务集获取代码库清单")
    @Path("/list")
    @POST
    Result<Map<Long, Set<CodeRepoVO>>> getCodeRepoListByTaskIds(
            @ApiParam("任务id清单")
                    Set<Long> taskIds,
            @ApiParam("项目id清单")
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId);

    @ApiOperation("获取oauth跳转链接")
    @Path("/oauth/url")
    @GET
    Result<String> getOauthUrl(
        @ApiParam("用户Id")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId,
        @ApiParam("项目Id")
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
        @ApiParam("任务Id")
        @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            long taskId,
        @ApiParam("工具英文名")
        @QueryParam("toolName")
            String toolName);
}
