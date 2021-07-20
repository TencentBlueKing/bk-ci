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

package com.tencent.bk.codecc.apiquery.api;

import com.tencent.bk.codecc.apiquery.vo.CodeRepoStatReqVO;
import com.tencent.bk.codecc.apiquery.vo.CodeRepoStatisticVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * op代码仓库查询接口
 *
 * @version V3.0
 * @date 2021/2/24
 */

@Api(tags = {"OP_CODE_REPO"}, description = "代码仓库查询接口")
@Path("/op/codeRepo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface OpCodeRepoRestResource {

    @ApiOperation("获取代码库总表数据")
    @Path("/codeRepo/list")
    @POST
    Result<Page<CodeRepoStatisticVO>> queryCodeRepoList(
            @ApiParam(value = "代码库总表信息请求体") CodeRepoStatReqVO reqVO,
            @ApiParam(value = "页数") @QueryParam(value = "pageNum") Integer pageNum,
            @ApiParam(value = "每页多少条") @QueryParam(value = "pageSize") Integer pageSize,
            @ApiParam(value = "排序字段") @QueryParam(value = "sortField") String sortField,
            @ApiParam(value = "排序类型") @QueryParam(value = "sortType") String sortType);

    @ApiOperation("获取新增代码库/代码分支数折线图数据")
    @Path("/codeRepoStatTrend")
    @POST
    Result<List<CodeRepoStatisticVO>> queryCodeRepoStatTrend(
            @ApiParam(value = "代码库总表信息请求体") CodeRepoStatReqVO reqVO);
}
