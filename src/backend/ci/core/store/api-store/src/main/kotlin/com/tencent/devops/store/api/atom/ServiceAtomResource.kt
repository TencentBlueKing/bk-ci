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

package com.tencent.devops.store.api.atom

import com.tencent.devops.common.api.annotation.BkInterfaceI18n
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.atom.AtomClassifyInfo
import com.tencent.devops.store.pojo.atom.AtomCodeVersionReqItem
import com.tencent.devops.store.pojo.atom.AtomProp
import com.tencent.devops.store.pojo.atom.AtomRunInfo
import com.tencent.devops.store.pojo.atom.InstalledAtom
import com.tencent.devops.store.pojo.atom.MarketAtomResp
import com.tencent.devops.store.pojo.atom.PipelineAtom
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.MarketAtomSortTypeEnum
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

@Tag(name = "SERVICE_PIPELINE_ATOM", description = "流水线-插件")
@Path("/service/pipeline/atom")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceAtomResource {

    @Operation(summary = "插件市场搜索插件")
    @GET
    @Path("/list/")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.records[*].code}", "{data.records[*].version}", "releaseInfo"]
    )
    fun list(
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

    @Operation(summary = "获取项目下已安装的插件列表")
    @GET
    @Path("/projectCodes/{projectCode}/list")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data[*].atomCode}", "{data[*].version}", "releaseInfo"]
    )
    fun getInstalledAtoms(
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<List<InstalledAtom>>

    @Operation(summary = "根据插件代码和版本号获取插件详细信息")
    @GET
    @Path("/codes/{atomCode}/versions/{version}")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.atomCode}", "{data.version}", "releaseInfo"]
    )
    fun getAtomVersionInfo(
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "版本号", required = true)
        @PathParam("version")
        version: String
    ): Result<PipelineAtom?>

    @Operation(summary = "根据插件代码和版本号集合批量获取插件信息")
    @POST
    @Path("/list/atomInfos")
    fun getAtomInfos(
        @Parameter(description = "插件代码版本集合", required = true)
        codeVersions: Set<AtomCodeVersionReqItem>
    ): Result<List<AtomRunInfo>>

    @Operation(summary = "获取插件真实版本号")
    @GET
    @Path("/projects/{projectCode}/codes/{atomCode}/versions/{version}/real")
    fun getAtomRealVersion(
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "版本号", required = true)
        @PathParam("version")
        version: String
    ): Result<String?>

    @Operation(summary = "获取插件属性列表")
    @POST
    @Path("/prop/list")
    fun getAtomProps(
        @Parameter(description = "插件标识列表", required = true)
        atomCodes: Set<String>
    ): Result<Map<String, AtomProp>?>

    @Operation(summary = "获取插件分类信息")
    @GET
    @Path("/codes/{atomCode}/classify/info")
    @BkInterfaceI18n(keyPrefixNames = ["ATOM", "{data.atomCode}", "{data.version}", "releaseInfo"])
    fun getAtomClassifyInfo(
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<AtomClassifyInfo?>
}
