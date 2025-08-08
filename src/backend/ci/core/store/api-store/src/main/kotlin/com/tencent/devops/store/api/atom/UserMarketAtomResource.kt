/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.atom.AtomDevLanguage
import com.tencent.devops.store.pojo.atom.AtomOutput
import com.tencent.devops.store.pojo.atom.AtomVersion
import com.tencent.devops.store.pojo.atom.AtomVersionListItem
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import com.tencent.devops.store.pojo.atom.MarketAtomResp
import com.tencent.devops.store.pojo.common.MarketMainItem
import com.tencent.devops.store.pojo.atom.MyAtomResp
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.MarketAtomSortTypeEnum
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.common.StoreErrorCodeInfo
import com.tencent.devops.store.pojo.common.version.StoreShowVersionInfo
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_MARKET_ATOM", description = "插件市场-插件")
@Path("/user/market/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserMarketAtomResource {

    @Operation(summary = "获取插件市场首页的数据")
    @Path("/atom/list/main")
    @GET
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data[*].records[*].code}", "{data[*].records[*].version}", "releaseInfo"]
    )
    fun mainPageList(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<MarketMainItem>>

    @Operation(summary = "插件市场搜索插件")
    @GET
    @Path("/atom/list/")
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

    @Operation(summary = "根据用户获取插件工作台插件列表")
    @GET
    @Path("/desk/atom/list/")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.records[*].atomCode}", "{data.records[*].version}", "releaseInfo"]
    )
    fun listMyAtoms(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "插件名称", required = false)
        @QueryParam("atomName")
        atomName: String?,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE, required = true)
        pageSize: Int = 10
    ): Result<MyAtomResp?>

    @Operation(summary = "根据插件版本ID获取插件详情")
    @GET
    @Path("/desk/atom/{atomId}")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.atomCode}", "{data.version}", "releaseInfo"]
    )
    fun getAtomById(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "atomId", required = true)
        @PathParam("atomId")
        atomId: String
    ): Result<AtomVersion?>

    @Operation(summary = "根据插件标识获取插件正式版本详情")
    @GET
    @Path("/atom/{atomCode}")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.atomCode}", "{data.version}", "releaseInfo"]
    )
    fun getAtomByCode(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<AtomVersion?>

    @Operation(summary = "根据插件标识获取插件版本列表")
    @GET
    @Path("/atom/version/list/")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.records[*].atomCode}", "{data.records[*].version}", "releaseInfo"]
    )
    fun getAtomVersionsByCode(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "atomCode", required = true)
        @QueryParam("atomCode")
        atomCode: String,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE, required = true)
        pageSize: Int = 10
    ): Result<Page<AtomVersionListItem>>

    @Operation(summary = "安装插件到项目")
    @POST
    @Path("/atom/install")
    fun installAtom(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "安装插件到项目请求报文体", required = true)
        installAtomReq: InstallAtomReq
    ): Result<Boolean>

    @Operation(summary = "根据插件标识获取已安装的项目列表")
    @GET
    @Path("/atom/installedProjects/{atomCode}")
    fun getInstalledProjects(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<List<InstalledProjRespItem?>>

    @Operation(summary = "获取插件支持的语言列表")
    @GET
    @Path("/desk/atom/language")
    fun listLanguage(): Result<List<AtomDevLanguage?>>

    @Operation(summary = "删除工作台插件")
    @DELETE
    @Path("/desk/atoms/{atomCode}")
    fun deleteAtom(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<Boolean>

    @Operation(summary = "根据插件标识获取插件回显版本信息")
    @GET
    @Path("/atoms/{atomCode}/showVersionInfo")
    fun getAtomShowVersionInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<StoreShowVersionInfo>

    @Operation(summary = "查看插件的yml信息")
    @GET
    @Path("/atoms/{atomCode}/yml/detail")
    fun getAtomYmlInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "是否展示系统自带的yml信息", required = false)
        @QueryParam("defaultShowFlag")
        defaultShowFlag: Boolean?
    ): Result<String?>

    @Operation(summary = "查看插件的yml 2.0信息")
    @GET
    @Path("/atoms/{atomCode}/yml/2.0/detail")
    fun getAtomYmlV2Info(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "是否展示系统自带的yml信息", required = false)
        @QueryParam("defaultShowFlag")
        defaultShowFlag: Boolean?
    ): Result<String?>

    @Operation(summary = "展示插件的outPut参数")
    @GET
    @Path("/atoms/{atomCode}/output")
    fun getAtomOutput(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<List<AtomOutput>>

    @Operation(summary = "更新插件自定义错误码信息")
    @PUT
    @Path("/{projectCode}/atom/errorCodeInfo")
    fun updateAtomErrorCodeInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectCode", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "插件自定义错误码信息", required = true)
        storeErrorCodeInfo: StoreErrorCodeInfo
    ): Result<Boolean>
}
