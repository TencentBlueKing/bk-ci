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

import com.tencent.devops.common.api.pojo.CodeCCResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * 解析告警服务
 *
 * @date 2020/1/17
 * @version V1.0
 */
@Api(tags = {"SERVICE_PARSE"}, description = "解析告警服务")
@Path("/build/parse")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildParseDefectRestResource
{

    @ApiOperation("工具侧通知上报告警")
    @Path("/notify/streamName/{streamName}/toolName/{toolName}/buildId/{buildId}")
    @POST
    CodeCCResult<Boolean> notifyReportDefects(
            @ApiParam(value = "项目英文名", required = true)
            @PathParam("streamName")
                    String streamName,
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
                    String toolName,
            @ApiParam(value = "构建id", required = true)
            @PathParam("buildId")
                    String buildId
    );


    @ApiOperation("工具侧通知上报告警")
    @Path("/reportStatus/streamName/{streamName}/toolName/{toolName}/buildId/{buildId}")
    @GET
    CodeCCResult<String> querydefectReportStatus(
            @ApiParam(value = "项目英文名", required = true)
            @PathParam("streamName")
            String streamName,
            @ApiParam(value = "工具名", required = true)
            @PathParam("toolName")
            String toolName,
            @ApiParam(value = "构建id", required = true)
            @PathParam("buildId")
            String buildId
    );

}
