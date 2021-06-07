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

package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.CheckerSetListQueryReq;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.devops.common.api.checkerset.*;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 规则集接口
 *
 * @version V1.0
 * @date 2020/1/2
 */
@Api(tags = {"SERVICE_CHECKER_SET"}, description = " 配置规则集接口")
@Path("/service/checkerSet")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceCheckerSetRestResource
{
    @ApiOperation("根据分类获取规则集清单")
    @Path("/categoryList")
    @GET
    Result<Map<String, List<CheckerSetVO>>> getCheckerSetListByCategory(
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId
    );

    @ApiOperation("任务关联规则集")
    @Path("/project/{projectId}/tasks/{taskId}/checkerSets/relationship")
    @POST
    Result<Boolean> batchRelateTaskAndCheckerSet(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @ApiParam(value = "项目Id", required = true)
            @PathParam("projectId")
                    String projectId,
            @ApiParam(value = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @ApiParam(value = "任务关联的规则集", required = true)
                    List<CheckerSetVO> checkerSetList,
            @ApiParam(value = "是否开源")
            @QueryParam("isOpenSource")
                    Boolean isOpenSource
    );

    @ApiOperation("根据规则ID列表查询规则集")
    @Path("/project/{projectId}")
    @POST
    Result<List<CheckerSetVO>> queryCheckerSets(
            @ApiParam(value = "规则集列表", required = true)
                    Set<String> checkerSetList,
            @ApiParam(value = "项目Id", required = true)
            @PathParam("projectId")
                    String projectId);

    @ApiOperation("根据任务Id查询任务已经关联的规则集列表")
    @Path("/tasks/{taskId}/list")
    @POST
    Result<List<CheckerSetVO>> getCheckerSets(
            @ApiParam(value = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId
    );

    @ApiOperation("根据任务和语言解绑相应的规则集")
    @Path("/task/{taskId}/codeLang/{codeLang}")
    @POST
    Result<Boolean> updateCheckerSetAndTaskRelation(
            @ApiParam(value = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @ApiParam(value = "项目语言", required = true)
            @PathParam("codeLang")
                    Long codeLang,
            @ApiParam(value = "项目语言", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user);


    @ApiOperation("获取规则和规则集数量")
    @Path("/count/task/{taskId}/projectId/{projectId}")
    @POST
    Result<TaskBaseVO> getCheckerAndCheckerSetCount(
            @ApiParam(value = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @ApiParam(value = "项目Id", required = true)
            @PathParam("projectId")
                    String projectId
    );

    @ApiOperation("规则集关联到项目或任务")
    @Path("/{checkerSetId}/relationships")
    @POST
    Result<Boolean> setRelationships(
            @ApiParam(value = "规则集Id", required = true)
            @PathParam("checkerSetId")
                    String checkerSetId,
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @ApiParam(value = "规则集关联到项目或任务", required = true)
                    CheckerSetRelationshipVO checkerSetRelationshipVO
    );

    @ApiOperation("根据规则ID列表查询规则集")
    @Path("/project/openscan/{projectId}")
    @POST
    Result<List<CheckerSetVO>> queryCheckerSetsForOpenScan(
            @ApiParam(value = "规则集列表", required = true)
                    Set<CheckerSetVO> checkerSetList,
            @ApiParam(value = "项目Id", required = true)
            @PathParam("projectId")
                    String projectId);

    @ApiOperation("查询规则集列表")
    @Path("/list")
    @POST
    Result<List<CheckerSetVO>> getCheckerSets(
        @ApiParam(value = "配置规则包参数", required = true)
            CheckerSetListQueryReq queryCheckerSetReq
    );
}
