/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.store.pojo.atom.AtomPipeline
import com.tencent.devops.store.pojo.atom.AtomVersion
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import com.tencent.devops.store.pojo.atom.MarketAtomResp
import com.tencent.devops.store.pojo.atom.PipelineAtom
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.MarketAtomSortTypeEnum
import com.tencent.devops.store.pojo.common.statistic.StoreStatistic
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_ATOM_V4", description = "OPENAPI-插件资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/atoms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ApigwAtomResourceV4 {
    @Operation(summary = "根据插件代码获取插件详细信息", tags = ["v4_app_atom_get", "v4_user_atom_get"])
    @GET
    @Path("/atom_info")
    fun getAtomByCode(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "插件代码", required = true)
        @QueryParam("atomCode")
        atomCode: String,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<AtomVersion?>

    @Operation(summary = "根据插件代码获取插件统计信息", tags = ["v4_app_atom_statistic", "v4_user_atom_statistic"])
    @GET
    @Path("/atom_statistic")
    fun getAtomStatisticByCode(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "插件代码", required = true)
        @QueryParam("atomCode")
        atomCode: String,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<StoreStatistic>

    @Operation(
        summary = "根据插件代码获取使用的流水线详情",
        tags = ["v4_user_atom_pipeline_list", "v4_app_atom_pipeline_list"]
    )
    @GET
    @Path("/atom_pipelines")
    fun getAtomPipelinesByCode(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "插件代码", required = true)
        @QueryParam("atomCode")
        atomCode: String,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数(默认20, 最大100)", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<AtomPipeline>>

    @Operation(summary = "安装插件到项目", tags = ["v4_user_atom_install", "v4_app_atom_install"])
    @POST
    @Path("/install_atom")
    fun installAtom(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "渠道类型", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode? = ChannelCode.BS,
        @Parameter(description = "安装插件到项目请求报文体", required = true)
        installAtomReq: InstallAtomReq
    ): Result<Boolean>

    @Operation(summary = "根据插件代码获取插件详细信息", tags = ["v4_user_atom_detail"])
    @GET
    @Path("/atom_detail")
    fun getAtomDetail(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "插件代码", required = true)
        @QueryParam("atomCode")
        atomCode: String,
        @Parameter(description = "版本号", required = true)
        @QueryParam("version")
        version: String,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<PipelineAtom?>

    @Operation(summary = "获取所有流水线插件信息", tags = ["v4_user_atom_search"])
    @GET
    @Path("/atom_search")
    fun list(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "搜索关键字", required = false)
        @QueryParam("keyword")
        keyword: String?,
        @Parameter(description = "插件分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @Parameter(description = "功能标签", required = false)
        @QueryParam("labelCode")
        labelCode: String?,
        @Parameter(description = "评分", required = false)
        @QueryParam("score")
        score: Int?,
        @Parameter(description = "研发来源", required = false)
        @QueryParam("rdType")
        rdType: AtomTypeEnum?,
        @Parameter(description = "yaml是否可用", required = false)
        @QueryParam("yamlFlag")
        yamlFlag: Boolean?,
        @Parameter(description = "是否推荐标识 true：推荐，false：不推荐", required = false)
        @QueryParam("recommendFlag")
        recommendFlag: Boolean?,
        @Parameter(description = "是否有红线指标", required = false)
        @QueryParam("qualityFlag")
        qualityFlag: Boolean?,
        @Parameter(description = "排序", required = false)
        @QueryParam("sortType")
        sortType: MarketAtomSortTypeEnum? = MarketAtomSortTypeEnum.CREATE_TIME,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<MarketAtomResp>
}
