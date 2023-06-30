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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.store.pojo.atom.AtomPipeline
import com.tencent.devops.store.pojo.atom.AtomPostReqItem
import com.tencent.devops.store.pojo.atom.AtomPostResp
import com.tencent.devops.store.pojo.atom.AtomVersion
import com.tencent.devops.store.pojo.atom.GetRelyAtom
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_MARKET_ATOM"], description = "插件市场-插件")
@Path("/service/market/atom/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceMarketAtomResource {

    @ApiOperation("设置插件构建结果状态")
    @PUT
    @Path("/atomCodes/{atomCode}/versions/{version}")
    fun setAtomBuildStatusByAtomCode(
        @ApiParam("插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("版本号", required = true)
        @PathParam("version")
        version: String,
        @ApiParam("用户Id", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("插件状态", required = true)
        @QueryParam("atomStatus")
        atomStatus: AtomStatusEnum,
        @ApiParam("状态描述", required = false)
        @QueryParam("msg")
        msg: String?
    ): Result<Boolean>

    @ApiOperation("获取所有流水线插件信息")
    @GET
    @Path("/project/{projectCode}/projectElement")
    fun getProjectElements(
        @ApiParam("项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<Map<String/* atomCode */, String/* cnName */>>

    @ApiOperation("获取所有默认插件和自定义插件信息")
    @GET
    @Path("/project/{projectCode}/projectElementInfo")
    fun getProjectElementsInfo(
        @ApiParam("项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<Map<String/* atomCode */, String/* installType */>>

    @ApiOperation("根据插件代码获取插件详细信息")
    @GET
    @Path("/{atomCode}")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.atomCode}", "{data.version}", "releaseInfo"]
    )
    fun getAtomByCode(
        @ApiParam("插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("用户名", required = true)
        @QueryParam("username")
        username: String
    ): Result<AtomVersion?>

    @ApiOperation("根据插件代码获取使用的流水线详情")
    @GET
    @Path("/{atomCode}/pipelines")
    fun getAtomPipelinesByCode(
        @ApiParam("插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("用户名", required = true)
        @QueryParam("username")
        username: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<AtomPipeline>>

    @ApiOperation("安装插件到项目")
    @POST
    @Path("/atom/install")
    fun installAtom(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("渠道类型", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode? = ChannelCode.BS,
        @ApiParam("安装插件到项目请求报文体", required = true)
        installAtomReq: InstallAtomReq
    ): Result<Boolean>

    @ApiOperation("获取带post属性的插件")
    @POST
    @Path("/project/{projectCode}/getPostAtoms")
    fun getPostAtoms(
        @ApiParam("项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("查询插件信息", required = true)
        atomItems: Set<AtomPostReqItem>
    ): Result<AtomPostResp>

    @ApiOperation("查看插件参数的依赖关系")
    @POST
    @Path("/atoms/rely")
    fun getAtomRely(
        @ApiParam("getRelyAtom", required = false)
        getRelyAtom: GetRelyAtom
    ): Result<Map<String, Map<String, Any>>?>
}
