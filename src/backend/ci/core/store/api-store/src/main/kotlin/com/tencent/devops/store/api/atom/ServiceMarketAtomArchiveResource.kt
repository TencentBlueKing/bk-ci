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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.atom.AtomPkgInfoUpdateRequest
import com.tencent.devops.store.pojo.atom.GetAtomConfigResult
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
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
@Suppress("ALL")
interface ServiceMarketAtomArchiveResource {

    @ApiOperation("校验用户上传的插件包是否合法")
    @GET
    @Path("/users/{userId}/atoms/{atomCode}/versions/{version}/package/verify")
    fun verifyAtomPackageByUserId(
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String,
        @ApiParam("插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("版本号", required = true)
        @PathParam("version")
        version: String,
        @ApiParam("项目代码", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @ApiParam("发布类型", required = false)
        @QueryParam("releaseType")
        releaseType: ReleaseTypeEnum?,
        @ApiParam("支持的操作系统", required = false)
        @QueryParam("os")
        os: String?
    ): Result<Boolean>

    @ApiOperation("校验用户上传的插件包中的taskJson是否正确")
    @GET
    @Path("/users/{userId}/atoms/{atomCode}/versions/{version}/json/verify")
    fun verifyAtomTaskJson(
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String,
        @ApiParam("插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("版本号", required = true)
        @PathParam("version")
        version: String,
        @ApiParam("项目代码", required = true)
        @QueryParam("projectCode")
        projectCode: String
    ): Result<GetAtomConfigResult?>

    @ApiOperation("校验插件发布类型是否合法")
    @GET
    @Path("/users/{userId}/atoms/{atomCode}/versions/{version}/releaseType/verify")
    fun validateReleaseType(
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String,
        @ApiParam("插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("版本号", required = true)
        @PathParam("version")
        version: String,
        @ApiParam("项目代码", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @ApiParam("插件字段校验确认标识", required = false)
        @QueryParam("fieldCheckConfirmFlag")
        fieldCheckConfirmFlag: Boolean?
    ): Result<Boolean>

    @ApiOperation("更新插件执行包相关信息")
    @PUT
    @Path("/users/{userId}/atoms/{atomId}/pkg/info/update")
    fun updateAtomPkgInfo(
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String,
        @ApiParam("插件ID", required = true)
        @PathParam("atomId")
        atomId: String,
        @ApiParam("插件执行包相关信息修改请求报文体", required = true)
        atomPkgInfoUpdateRequest: AtomPkgInfoUpdateRequest
    ): Result<Boolean>
}
