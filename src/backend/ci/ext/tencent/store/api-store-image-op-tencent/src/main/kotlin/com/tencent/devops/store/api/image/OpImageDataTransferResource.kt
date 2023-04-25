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

package com.tencent.devops.store.api.image

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_DATATRANSFER_IMAGE"], description = "OP-数据迁移-镜像")
@Path("/op/datatransfer/image")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpImageDataTransferResource {

    @ApiOperation("初始化系统分类与范畴")
    @PUT
    @Path("/initClassifyAndCategory")
    fun initClassifyAndCategory(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "分类代码", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @ApiParam(value = "分类名称", required = false)
        @QueryParam("classifyName")
        classifyName: String?,
        @ApiParam(value = "范畴代码", required = false)
        @QueryParam("categoryCode")
        categoryCode: String?,
        @ApiParam(value = "范畴名称", required = false)
        @QueryParam("categoryName")
        categoryName: String?
    ): Result<Int>

    @ApiOperation("按项目迁移已拷贝为构建镜像的数据")
    @PUT
    @Path("/transferImage")
    fun transferImage(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目编码", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @ApiParam(value = "分类代码", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @ApiParam(value = "范畴代码", required = false)
        @QueryParam("categoryCode")
        categoryCode: String?
    ): Result<Int>

    @ApiOperation("按项目批量重新验证")
    @PUT
    @Path("/batchRecheckByProject")
    fun batchRecheckByProject(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目编码", required = true)
        @QueryParam("projectCode")
        projectCode: String
    ): Result<Int>

    @ApiOperation("批量重新验证")
    @PUT
    @Path("/batchRecheckAll")
    fun batchRecheckAll(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<Int>
}
