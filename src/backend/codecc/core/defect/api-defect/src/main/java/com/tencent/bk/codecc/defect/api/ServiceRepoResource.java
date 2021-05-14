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

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;

import com.tencent.bk.codecc.defect.vo.CodeRepoVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * 代码库信息接口
 *
 * @version V1.0
 * @date 2019/12/3
 */
@Api(tags = {"SERVICE_CODEREPO"}, description = "告警相关接口")
@Path("/service/repo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceRepoResource {

    @ApiOperation("获取指定任务的代码库清单")
    @Path("/list")
    @GET
    Result<Set<CodeRepoVO>> getCodeRepoByTaskIdAndBuildId(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "构建id", required = true)
            @QueryParam(value = "buildId")
                    String buildId);

    @ApiOperation("获取指定任务的代码库清单")
    @Path("/lists")
    @POST
    Result<Map<Long, Set<CodeRepoVO>>> getCodeRepoByTaskIds(
            @ApiParam(value = "任务ID", required = true)
                    Collection<Long> taskIds);
}
