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

import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.bk.codecc.task.vo.RepoInfoVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 基础数据服务接口
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Api(tags = {"SERVICE_BASEDATA"}, description = "基础数据服务接口")
@Path("/service/baseData")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceBaseDataResource
{
    @ApiOperation("获取任务信息")
    @Path("/paramType/{paramType}/paramCode/{paramCode}")
    @GET
    Result<List<BaseDataVO>> getInfoByTypeAndCode(
            @ApiParam(value = "参数类型", required = true)
            @PathParam(value = "paramType")
                    String paramType,
            @ApiParam(value = "参数代码", required = true)
            @PathParam(value = "paramCode")
                    String paramCode);


    @ApiOperation("根据蓝盾项目ID批量查询仓库地址")
    @Path("/repoUrl")
    @POST
    Result<Map<String, RepoInfoVO>> getRepoUrlByProjects(
            @ApiParam(value = "蓝盾项目ID", required = true)
                    Set<String> bkProjectIds);

    @ApiOperation("根据参数类型获取参数列表")
    @Path("/paramType/{paramType}/params")
    @GET
    Result<List<BaseDataVO>> getParamsByType(
            @ApiParam(value = "参数类型", required = true)
            @PathParam(value = "paramType")
                    String paramType);

    @ApiOperation("批量保存元数据信息")
    @Path("/batchSave")
    @POST
    Result<Integer> batchSave(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String uerId,
            List<BaseDataVO> baseDataVOList);

    @ApiOperation("删除元数据信息")
    @Path("/{id}/id/delete")
    @DELETE
    Result<Integer> deleteById(
        @PathParam("id")
        String id);


    @ApiOperation("更新屏蔽用户名单")
    @Path("/excludeUserMember/update")
    @POST
    Result<Boolean> updateExcludeUserMember(
            @ApiParam(value = "当前用户", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName,
            @ApiParam(value = "屏蔽名单请求体", required = true) @Valid BaseDataVO baseDataVO);


    @ApiOperation("获取屏蔽用户名单")
    @Path("/excludeUserMember/list")
    @GET
    Result<List<String>> queryExcludeUserMember();


    @ApiOperation("更新管理员名单")
    @Path("/adminMember/update")
    @POST
    Result<Boolean> updateAdminMember(
            @ApiParam(value = "当前用户", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName,
            @ApiParam(value = "屏蔽名单请求体", required = true) @Valid BaseDataVO baseDataVO);


    @ApiOperation("获取管理员名单")
    @Path("/adminMember/list")
    @GET
    Result<List<String>> queryAdminMember();

    @ApiOperation("获取语言元数据")
    @Path("/")
    @POST
    Result<List<BaseDataVO>> findBaseData();
}
