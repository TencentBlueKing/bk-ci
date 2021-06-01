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

import com.tencent.bk.codecc.defect.vo.CheckerCommonCountVO;
import com.tencent.bk.codecc.defect.vo.CheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.OtherCheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.UpdateAllCheckerReq;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetPermissionType;
import com.tencent.devops.common.api.checkerset.*;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.data.domain.Page;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 规则集接口
 *
 * @version V1.0
 * @date 2020/1/2
 */
@Api(tags = {"USER_CHECKER_SET"}, description = " 配置规则集接口")
@Path("/user/checkerSet")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserCheckerSetRestResource
{
    @ApiOperation("查询创建规则集所需的参数选项")
    @Path("/params")
    @GET
    Result<CheckerSetParamsVO> getParams(
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId
    );

    @ApiOperation("创建规则集")
    @Path("/")
    @POST
    Result<Boolean> createCheckerSet(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "创建规则集请求参数", required = true)
                    CreateCheckerSetReqVO createCheckerSetReqVO
    );

    @ApiOperation("更新规则集中的规则")
    @Path("/checkerSets/{checkerSetId}/checkers")
    @PUT
    Result<Boolean> updateCheckersOfSet(
            @ApiParam(value = "规则集Id", required = true)
            @PathParam("checkerSetId")
                    String checkerSetId,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @ApiParam(value = "创建规则集请求参数", required = true)
                    UpdateCheckersOfSetReqVO updateCheckersOfSetReq
    );

    @ApiOperation("查询规则集列表")
    @Path("/list")
    @POST
    Result<List<CheckerSetVO>> getCheckerSets(
            @ApiParam(value = "配置规则包参数", required = true)
                    CheckerSetListQueryReq queryCheckerSetReq
    );

    @ApiOperation("查询任务规则集列表")
    @Path("/task/{taskId}/list")
    @GET
    Result<List<CheckerSetVO>> getTaskCheckerSets(
            @ApiParam(value = "项目id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "任务id", required = true)
            @PathParam("taskId")
            long taskId,
            @ApiParam(value = "工具名称", required = true)
            @QueryParam("toolName")
            String toolName,
            @ApiParam(value = "工具维度", required = true)
            @QueryParam("dimension")
            String dimension
    );

    @ApiOperation("查询规则集列表")
    @Path("/listPageable")
    @POST
    Result<Page<CheckerSetVO>> getCheckerSetsPageable(
        @ApiParam(value = "配置规则包参数", required = true)
            CheckerSetListQueryReq queryCheckerSetReq
    );

    @ApiOperation("查询其他规则集列表")
    @Path("/otherList")
    @POST
    Result<Page<CheckerSetVO>> getOtherCheckerSets(
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "配置规则包参数", required = true)
                    OtherCheckerSetListQueryReq queryCheckerSetReq
    );

    @ApiOperation("查询规则集数量")
    @Path("/count")
    @POST
    Result<List<CheckerCommonCountVO>> queryCheckerSetCountList(
            @ApiParam(value = "规则数量查询条件", required = true)
            CheckerSetListQueryReq checkerSetListQueryReq);

    @ApiOperation("查询单个规则集详情")
    @Path("/{checkerSetId}/versions/{version}/detail")
    @GET
    Result<CheckerSetVO> getCheckerSetDetail(
            @ApiParam(value = "规则集Id", required = true)
            @PathParam("checkerSetId")
                    String checkerSetId,
            @ApiParam(value = "规则集版本号", required = true)
            @PathParam("version")
                    Integer version
    );

    @ApiOperation("更新规则集详情基础信息")
    @Path("/{checkerSetId}/baseInfo")
    @PUT
    Result<Boolean> updateCheckerSetBaseInfo(
            @ApiParam(value = "规则集Id", required = true)
            @PathParam("checkerSetId")
                    String checkerSetId,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "配置规则包参数", required = true)
                    V3UpdateCheckerSetReqVO updateCheckerSetReq
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

    @ApiOperation("一键关联规则集到任务")
    @Path("/relationships/once")
    @POST
    Result<Boolean> setRelationshipsOnce(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        String user,
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        String projectId,
        @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
        long taskId,
        @QueryParam("toolName")
        String toolName
    );

    @ApiOperation("规则集管理")
    @Path("/{checkerSetId}/management")
    @POST
    Result<Boolean> management(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @ApiParam(value = "规则集Id", required = true)
            @PathParam("checkerSetId")
                    String checkerSetId,
            @ApiParam(value = "规则集关联到项目或任务", required = true)
                    CheckerSetManagementReqVO checkerSetManagementReqVO
    );

    @ApiOperation("根据分类获取规则集清单")
    @Path("/categoryList")
    @GET
    Result<Map<String, List<CheckerSetVO>>> getCheckerSetListByCategory(
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId
    );

    @ApiOperation("判断用户是否具有管理项目下的规则集权限")
    @Path("/userManagementPermission")
    @POST
    Result<List<CheckerSetPermissionType>> getUserManagementPermission(
            @ApiParam(value = "规则集关联到项目或任务", required = true)
                    AuthManagementPermissionReqVO authManagementPermissionReqVO
    );

    @ApiOperation("全量更新规则集的规则")
    @Path("/checkers/all")
    @POST
    Result<Boolean> updateCheckersOfSetForAll(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @ApiParam(value = "全量更新规则请求体", required = true)
            UpdateAllCheckerReq updateAllCheckerReq
    );
}
