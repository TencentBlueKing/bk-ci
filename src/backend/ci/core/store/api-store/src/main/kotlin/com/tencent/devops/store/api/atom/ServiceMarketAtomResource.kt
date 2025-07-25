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
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.store.pojo.atom.AtomPipeline
import com.tencent.devops.store.pojo.atom.AtomPostReqItem
import com.tencent.devops.store.pojo.atom.AtomPostResp
import com.tencent.devops.store.pojo.atom.AtomVersion
import com.tencent.devops.store.pojo.atom.ElementThirdPartySearchParam
import com.tencent.devops.store.pojo.atom.GetRelyAtom
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_MARKET_ATOM", description = "插件市场-插件")
@Path("/service/market/atom/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceMarketAtomResource {

    @Operation(summary = "设置插件构建结果状态")
    @PUT
    @Path("/atomCodes/{atomCode}/versions/{version}")
    fun setAtomBuildStatusByAtomCode(
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "版本号", required = true)
        @PathParam("version")
        version: String,
        @Parameter(description = "用户Id", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "插件状态", required = true)
        @QueryParam("atomStatus")
        atomStatus: AtomStatusEnum,
        @Parameter(description = "状态描述", required = false)
        @QueryParam("msg")
        msg: String?
    ): Result<Boolean>

    @Operation(summary = "获取所有流水线插件信息")
    @GET
    @Path("/project/{projectCode}/projectElement")
    fun getProjectElements(
        @Parameter(description = "项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<Map<String/* atomCode */, String/* cnName */>>

    @Operation(summary = "获取所有默认插件和自定义插件信息")
    @GET
    @Path("/project/{projectCode}/projectElementInfo")
    fun getProjectElementsInfo(
        @Parameter(description = "项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<Map<String/* atomCode */, String/* installType */>>

    @Operation(summary = "根据插件代码获取插件详细信息")
    @GET
    @Path("/{atomCode}")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.atomCode}", "{data.version}", "releaseInfo"]
    )
    fun getAtomByCode(
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "用户名", required = true)
        @QueryParam("username")
        username: String
    ): Result<AtomVersion?>

    @Operation(summary = "根据插件代码获取使用的流水线详情")
    @GET
    @Path("/{atomCode}/pipelines")
    fun getAtomPipelinesByCode(
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "用户名", required = true)
        @QueryParam("username")
        username: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<AtomPipeline>>

    @Operation(summary = "安装插件到项目")
    @POST
    @Path("/atom/install")
    fun installAtom(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "渠道类型", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode? = ChannelCode.BS,
        @Parameter(description = "安装插件到项目请求报文体", required = true)
        installAtomReq: InstallAtomReq
    ): Result<Boolean>

    @Operation(summary = "获取带post属性的插件")
    @POST
    @Path("/project/{projectCode}/getPostAtoms")
    fun getPostAtoms(
        @Parameter(description = "项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "查询插件信息", required = true)
        atomItems: Set<AtomPostReqItem>
    ): Result<AtomPostResp>

    @Operation(summary = "查看插件参数的依赖关系")
    @POST
    @Path("/atoms/rely")
    fun getAtomRely(
        @Parameter(description = "getRelyAtom", required = false)
        getRelyAtom: GetRelyAtom
    ): Result<Map<String, Map<String, Any>>?>

    @Operation(summary = "查看插件参数的依赖关系")
    @POST
    @Path("/atom/default_value")
    fun getAtomsDefaultValue(
        atom: ElementThirdPartySearchParam
    ): Result<Map<String, Any>>
}
