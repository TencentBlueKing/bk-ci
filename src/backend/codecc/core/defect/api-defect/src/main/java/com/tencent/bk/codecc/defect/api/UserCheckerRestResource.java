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

import com.tencent.bk.codecc.defect.vo.*;
import com.tencent.bk.codecc.defect.vo.checkerset.*;
import com.tencent.bk.codecc.defect.vo.enums.CheckerListSortType;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.data.domain.Sort;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 配置规则包服务
 *
 * @version V1.0
 * @date 2019/5/29
 */
@Api(tags = {"USER_CHECKER"}, description = " 配置规则包接口")
@Path("/user/checker")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserCheckerRestResource
{

    @ApiOperation("获取配置规则包")
    @Path("/tasks/{taskId}/toolName/{toolName}/checkers")
    @GET
    Result<GetCheckerListRspVO> checkerPkg(
            @ApiParam(value = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @ApiParam(value = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName
    );


    @ApiOperation("打开或者关闭配置规则包")
    @Path("/tasks/{taskId}/toolName/{toolName}/checkers/configuration")
    @POST
    Result<Boolean> configCheckerPkg(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @ApiParam(value = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @ApiParam(value = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName,
            @ApiParam(value = "配置规则包参数", required = true)
                    ConfigCheckersPkgReqVO packageVo
    );

    @ApiOperation("修改规则集")
    @Path("/tasks/{taskId}/tools/{toolName}/checkerSets/{checkerSetId}")
    @PUT
    Result<Boolean> updateCheckerSet(
            @ApiParam(value = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @ApiParam(value = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName,
            @ApiParam(value = "规则集ID", required = true)
            @PathParam("checkerSetId")
                    String checkerSetId,
            @ApiParam(value = "修改规则集请求参数", required = true)
                    UpdateCheckerSetReqVO updateCheckerSetReqVO,
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId
    );


    @ApiOperation("任务关联规则集")
    @Path("/tasks/{taskId}/checkerSets/relationship")
    @POST
    Result<Boolean> addCheckerSet2Task(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @ApiParam(value = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @ApiParam(value = "任务关联规则集请求参数", required = true)
                    AddCheckerSet2TaskReqVO addCheckerSet2TaskReqVO
    );


    @ApiOperation("查询用户创建的规则集列表")
    @Path("/tools/{toolName}/userCreatedCheckerSets")
    @GET
    Result<UserCreatedCheckerSetsVO> getUserCreatedCheckerSet(
            @ApiParam(value = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName,
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId
    );


    @ApiOperation("查询规则集指定版本的差异")
    @Path("/tools/{toolName}/checkerSets/{checkerSetId}/versions/difference")
    @POST
    Result<CheckerSetDifferenceVO> getCheckerSetVersionDifference(
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName,
            @ApiParam(value = "规则集ID", required = true)
            @PathParam("checkerSetId")
                    String checkerSetId,
            @ApiParam(value = "规则集指定版本差异请求体", required = true)
                    CheckerSetDifferenceVO checkerSetDifferenceVO
    );

    @ApiOperation("更新规则参数配置")
    @Path("/taskId/{taskId}/tools/{toolName}/param/{paramValue}")
    @PUT
    Result<Boolean>  updateCheckerConfigParam(
            @ApiParam(value = "任务Id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @ApiParam(value = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName,
            @ApiParam(value = "规则键", required = true)
            @QueryParam("checkerKey")
                    String checkerName,
            @ApiParam(value = "参数值", required = true)
            @PathParam("paramValue")
                    String paramValue,
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user
    );


    @ApiOperation("获取规则详情")
    @Path("/detail/toolName/{toolName}")
    @GET
    Result<CheckerDetailVO>  queryCheckerDetail(
            @ApiParam(value = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName,
            @ApiParam(value = "规则键", required = true)
            @QueryParam("checkerKey")
                    String checkerKey
    );

    @ApiOperation("获取规则详情")
    @Path("/list")
    @POST
    Result<List<CheckerDetailVO>> queryCheckerDetailList(
            @ApiParam(value = "规则清单查询条件", required = true)
                    CheckerListQueryReq checkerListQueryReq,
            @ApiParam(value = "项目id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam("页数")
            @QueryParam("pageNum")
                    Integer pageNum,
            @ApiParam("页数")
            @QueryParam("pageSize")
            Integer pageSize,
            @ApiParam("升序或降序")
            @QueryParam("sortType")
            Sort.Direction sortType,
            @ApiParam("排序字段")
            @QueryParam("sortField")
            CheckerListSortType sortField);

    @ApiOperation("获取规则数量")
    @Path("/count")
    @POST
    Result<List<CheckerCommonCountVO>> queryCheckerCountList(
            @ApiParam(value = "规则数量查询条件", required = true)
            CheckerListQueryReq checkerListQueryReq,
            @ApiParam(value = "项目id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId);

    @ApiOperation("获取规则详情")
    @Path("/toolName/{toolName}/queryChecker")
    @GET
    Result<List<CheckerDetailVO>> queryCheckerByTool(
        @ApiParam(value = "工具名称", required = true)
        @PathParam("toolName")
            String toolName
    );

    /**
     * 根据checkerKey和ToolName更新规则详情
     *
     * @param checkerDetailVO
     * @return
     */
    @ApiOperation("编辑规则详情")
    @Path("/update")
    @POST
    Result<Boolean> updateCheckerByCheckerKey(
            @ApiParam(value = "规则详情请求体", required = true) CheckerDetailVO checkerDetailVO);
}
