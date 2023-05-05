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
package com.tencent.devops.store.api.image.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.image.response.ImageRepoInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_MARKET_IMAGE"], description = "SERVICE-研发商店-镜像")
@Path("/service/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceStoreImageResource {
    @ApiOperation("查询镜像是否已安装到项目")
    @GET
    @Path("/image/projectCodes/{projectCode}/imageCodes/{imageCode}/isInstalled")
    fun isInstalled(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像标识", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("镜像标识", required = true)
        @PathParam("imageCode")
        imageCode: String
    ): Result<Boolean>

    @ApiOperation("根据code查询镜像详情")
    @GET
    @Path("/image/projectCodes/{projectCode}/imageCodes/{imageCode}/imageVersions/{imageVersion}")
    fun getImageRepoInfoByCodeAndVersion(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目标识", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("镜像标识", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @ApiParam("镜像版本", required = false)
        @PathParam("imageVersion")
        imageVersion: String?,
        @ApiParam("流水线Id", required = true)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam("构建Id", required = true)
        @QueryParam("buildId")
        buildId: String?
    ): Result<ImageRepoInfo>

    @ApiOperation("获取所有的自研公共镜像")
    @GET
    @Path("/image/self_develop/public_images")
    fun getSelfDevelopPublicImages(): Result<List<ImageRepoInfo>>

    @ApiOperation("根据code和版本号查询镜像状态")
    @GET
    @Path("/image/imageCodes/{imageCode}/imageVersions/{imageVersion}/imageStatus")
    fun getImageStatusByCodeAndVersion(
        @ApiParam("镜像标识", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @ApiParam("镜像版本", required = false)
        @PathParam("imageVersion")
        imageVersion: String
    ): Result<String>
}
