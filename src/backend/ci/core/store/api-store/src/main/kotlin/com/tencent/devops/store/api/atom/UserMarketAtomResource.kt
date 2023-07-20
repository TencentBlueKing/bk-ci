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
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
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
import com.tencent.devops.store.pojo.atom.MarketMainItem
import com.tencent.devops.store.pojo.atom.MyAtomResp
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.MarketAtomSortTypeEnum
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.common.StoreErrorCodeInfo
import com.tencent.devops.store.pojo.common.StoreShowVersionInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_MARKET_ATOM"], description = "插件市场-插件")
@Path("/user/market/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserMarketAtomResource {

    @ApiOperation("获取插件市场首页的数据")
    @Path("/atom/list/main")
    @GET
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data[*].records[*].code}", "{data[*].records[*].version}", "releaseInfo"]
    )
    fun mainPageList(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<MarketMainItem>>

    @ApiOperation("插件市场搜索插件")
    @GET
    @Path("/atom/list/")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.records[*].code}", "{data.records[*].version}", "releaseInfo"]
    )
    fun list(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("搜索关键字", required = false)
        @QueryParam("keyword")
        keyword: String?,
        @ApiParam("插件分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @ApiParam("功能标签", required = false)
        @QueryParam("labelCode")
        labelCode: String?,
        @ApiParam("评分", required = false)
        @QueryParam("score")
        score: Int?,
        @ApiParam("研发来源", required = false)
        @QueryParam("rdType")
        rdType: AtomTypeEnum?,
        @ApiParam("yaml是否可用", required = false)
        @QueryParam("yamlFlag")
        yamlFlag: Boolean?,
        @ApiParam("是否推荐标识 true：推荐，false：不推荐", required = false)
        @QueryParam("recommendFlag")
        recommendFlag: Boolean?,
        @ApiParam("是否有红线指标", required = false)
        @QueryParam("qualityFlag")
        qualityFlag: Boolean?,
        @ApiParam("排序", required = false)
        @QueryParam("sortType")
        sortType: MarketAtomSortTypeEnum? = MarketAtomSortTypeEnum.CREATE_TIME,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<MarketAtomResp>

    @ApiOperation("根据用户获取插件工作台插件列表")
    @GET
    @Path("/desk/atom/list/")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.records[*].atomCode}", "{data.records[*].version}", "releaseInfo"]
    )
    fun listMyAtoms(
        @ApiParam("token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("插件名称", required = false)
        @QueryParam("atomName")
        atomName: String?,
        @ApiParam("页码", required = true)
        @QueryParam("page")
        page: Int = 1,
        @ApiParam("每页数量", required = true)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE, required = true)
        pageSize: Int = 10
    ): Result<MyAtomResp?>

    @ApiOperation("根据插件版本ID获取插件详情")
    @GET
    @Path("/desk/atom/{atomId}")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.atomCode}", "{data.version}", "releaseInfo"]
    )
    fun getAtomById(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("atomId", required = true)
        @PathParam("atomId")
        atomId: String
    ): Result<AtomVersion?>

    @ApiOperation("根据插件标识获取插件正式版本详情")
    @GET
    @Path("/atom/{atomCode}")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.atomCode}", "{data.version}", "releaseInfo"]
    )
    fun getAtomByCode(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<AtomVersion?>

    @ApiOperation("根据插件标识获取插件版本列表")
    @GET
    @Path("/atom/version/list/")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.records[*].atomCode}", "{data.records[*].version}", "releaseInfo"]
    )
    fun getAtomVersionsByCode(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("atomCode", required = true)
        @QueryParam("atomCode")
        atomCode: String,
        @ApiParam("页码", required = true)
        @QueryParam("page")
        page: Int = 1,
        @ApiParam("每页数量", required = true)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE, required = true)
        pageSize: Int = 10
    ): Result<Page<AtomVersionListItem>>

    @ApiOperation("安装插件到项目")
    @POST
    @Path("/atom/install")
    fun installAtom(
        @ApiParam("token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("安装插件到项目请求报文体", required = true)
        installAtomReq: InstallAtomReq
    ): Result<Boolean>

    @ApiOperation("根据插件标识获取已安装的项目列表")
    @GET
    @Path("/atom/installedProjects/{atomCode}")
    fun getInstalledProjects(
        @ApiParam("token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<List<InstalledProjRespItem?>>

    @ApiOperation("获取插件支持的语言列表")
    @GET
    @Path("/desk/atom/language")
    fun listLanguage(): Result<List<AtomDevLanguage?>>

    @ApiOperation("删除工作台插件")
    @DELETE
    @Path("/desk/atoms/{atomCode}")
    fun deleteAtom(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<Boolean>

    @ApiOperation("根据插件标识获取插件回显版本信息")
    @GET
    @Path("/atoms/{atomCode}/showVersionInfo")
    fun getAtomShowVersionInfo(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<StoreShowVersionInfo>

    @ApiOperation("查看插件的yml信息")
    @GET
    @Path("/atoms/{atomCode}/yml/detail")
    fun getAtomYmlInfo(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("是否展示系统自带的yml信息", required = false)
        @QueryParam("defaultShowFlag")
        defaultShowFlag: Boolean?
    ): Result<String?>

    @ApiOperation("查看插件的yml 2.0信息")
    @GET
    @Path("/atoms/{atomCode}/yml/2.0/detail")
    fun getAtomYmlV2Info(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("是否展示系统自带的yml信息", required = false)
        @QueryParam("defaultShowFlag")
        defaultShowFlag: Boolean?
    ): Result<String?>

    @ApiOperation("展示插件的outPut参数")
    @GET
    @Path("/atoms/{atomCode}/output")
    fun getAtomOutput(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<List<AtomOutput>>

    @ApiOperation("更新插件自定义错误码信息")
    @PUT
    @Path("/{projectCode}/atom/errorCodeInfo")
    fun updateAtomErrorCodeInfo(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectCode", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("插件自定义错误码信息", required = true)
        storeErrorCodeInfo: StoreErrorCodeInfo
    ): Result<Boolean>
}
