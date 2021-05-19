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
import com.tencent.bk.codecc.defect.vo.integrated.ToolCheckerSetToStatusVo;
import com.tencent.devops.common.api.checkerset.AuthManagementPermissionReqVO;
import com.tencent.devops.common.api.checkerset.CheckerSetManagementReqVO;
import com.tencent.devops.common.api.checkerset.CheckerSetParamsVO;
import com.tencent.devops.common.api.checkerset.CheckerSetRelationshipVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.checkerset.CreateCheckerSetReqVO;
import com.tencent.devops.common.api.checkerset.UpdateCheckersOfSetReqVO;
import com.tencent.devops.common.api.checkerset.V3UpdateCheckerSetReqVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.data.domain.Page;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_BUILD_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 规则集接口
 *
 * @version V1.0
 * @date 2020/1/2
 */
@Api(tags = {"BUILD_CHECKER_SET"}, description = " 配置规则集接口")
@Path("/build/checkerSet")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildCheckerSetRestResource {
    @ApiOperation("规则集关联到项目或任务")
    @Path("/relationships")
    @POST
    Result<Boolean> setRelationships(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        String user,
        @QueryParam("type")
        String type,
        @QueryParam("projectId")
        String projectId,
        @QueryParam("taskId")
        Long taskId,
        @ApiParam(value = "规则集列表")
        List<CheckerSetVO> checkerSetVOList
    );

    @ApiOperation("更新规则集状态元数据")
    @Path("/tools/{toolName}/integratedStatus/update")
    @PUT
    Result<String> updateToolCheckerSetToStatus(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String user,
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
            String buildId,
        @ApiParam(value = "工具名称")
        @PathParam("toolName")
            String toolName,
        @ApiParam(value = "状态")
        @QueryParam("status")
            ComConstants.ToolIntegratedStatus status,
            ToolCheckerSetToStatusVo toolCheckerSetToStatusVo
    );

    @ApiOperation("回滚规则集状态元数据")
    @Path("/tools/{toolName}/integratedStatus/revert")
    @PUT
    Result<String> revertToolCheckerSetStatus(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String user,
        @ApiParam(value = "工具名称")
        @PathParam("toolName")
            String toolName,
        @ApiParam(value = "状态")
        @QueryParam("status")
            ComConstants.ToolIntegratedStatus status,
            Set<String> checkerSetIds
    );
}
