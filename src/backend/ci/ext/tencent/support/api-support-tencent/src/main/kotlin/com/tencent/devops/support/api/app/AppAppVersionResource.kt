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

package com.tencent.devops.support.api.app

import com.tencent.devops.common.api.auth.AUTH_HEADER_APP_VERSION
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_NAME
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.support.model.app.pojo.AppVersion
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

/**
 * Created by Freyzheng on 2018/9/26.
 */

@Api(tags = ["APP_APP_VERSION"], description = "APP-APP-VERSION")
@Path("/app/app/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AppAppVersionResource {

    @ApiOperation("获取最新的app版本号")
    @GET
    @Path("/last")
    fun getLastAppVersion(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("版本号", required = true)
        @HeaderParam(AUTH_HEADER_APP_VERSION)
        appVersion: String?,
        @ApiParam("组织", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_NAME)
        organization: String? = null,
        @ApiParam(value = "渠道类型（1:\"安卓\", 2:\"IOS\", 3:\"WEB\"）", required = true)
        @QueryParam(value = "channelType")
        channelType: Byte
    ): Result<AppVersion?>

    @ApiOperation("获取所有的app版本号")
    @GET
    @Path("/")
    fun getAllAppVersion(
        @ApiParam(value = "渠道类型（1:\"安卓\", 2:\"IOS\", 3:\"WEB\"）", required = true)
        @QueryParam(value = "channelType")
        channelType: Byte
    ): Result<List<AppVersion>>
}
