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

package com.tencent.devops.support.api.op

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.core.MediaType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.support.model.app.pojo.AppVersion
import com.tencent.devops.support.model.app.AppVersionRequest
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces

/**
 * Created by Freyzheng on 2018/9/26.
 */

@Api(tags = ["OP_APP_VERSION"], description = "OP-APP-VERSION")
@Path("/op/app/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAppVersionResource {

    @ApiOperation("获取所有app版本日志")
    @GET
    @Path("/")
    fun getAllAppVersion(): Result<List<AppVersion>>

    @ApiOperation("获取app版本日志")
    @GET
    @Path("/{appVersionId}")
    fun getAppVersion(
        @ApiParam(value = "app版本号id", required = true)
        @PathParam("appVersionId")
        appVersionId: Long
    ): Result<AppVersion?>

    @ApiOperation("新增app版本日志")
    @POST
    @Path("/")
    fun addAppVersion(
        @ApiParam(value = "APP版本", required = true)
        appVersionRequest: AppVersionRequest
    ): Result<Int>

    @ApiOperation("新增多个app版本日志")
    @POST
    @Path("/multi")
    fun addAppVersions(
        @ApiParam(value = "APP版本", required = true)
        appVersionRequests: List<AppVersionRequest>
    ): Result<Int>

    @ApiOperation("更新app版本日志")
    @PUT
    @Path("/{appVersionId}")
    fun updateAppVersion(
        @ApiParam(value = "app版本号id", required = true)
        @PathParam("appVersionId")
        appVersionId: Long,
        @ApiParam(value = "APP版本", required = true)
        appVersionRequest: AppVersionRequest
    ): Result<Int>

    @ApiOperation("删除app版本日志")
    @DELETE
    @Path("/{appVersionId}")
    fun deleteAppVersion(
        @ApiParam(value = "app版本号id", required = true)
        @PathParam("appVersionId")
        appVersionId: Long
    ): Result<Int>
}
