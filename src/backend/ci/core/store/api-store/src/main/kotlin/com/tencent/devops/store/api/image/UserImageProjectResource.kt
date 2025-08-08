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
package com.tencent.devops.store.api.image

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.request.InstallImageReq
import com.tencent.devops.store.pojo.image.response.JobImageItem
import com.tencent.devops.store.pojo.image.response.JobMarketImageItem
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_MARKET_IMAGE_PROJECT", description = "研发商店-镜像项目间关系")
@Path("/user/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserImageProjectResource {

    @Operation(summary = "安装镜像到项目")
    @POST
    @Path("/image/install")
    fun installImage(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "安装镜像到项目请求报文体", required = true)
        installImageReq: InstallImageReq
    ): Result<Boolean>

    @Operation(summary = "根据镜像标识获取已安装的项目列表")
    @GET
    @Path("/image/installedProjects/{imageCode}")
    fun getInstalledProjects(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模版代码", required = true)
        @PathParam("imageCode")
        imageCode: String
    ): Result<List<InstalledProjRespItem>>

    @Operation(summary = "根据项目标识获取可用镜像列表（公共+已安装）")
    @GET
    @Path("/image/availableImages")
    fun getAvailableImagesByProjectCode(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目标识", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @Parameter(description = "机器类型", required = false)
        @QueryParam("agentType")
        agentType: ImageAgentTypeEnum?,
        @Parameter(description = "是否推荐", required = false)
        @QueryParam("recommendFlag")
        recommendFlag: Boolean?,
        @Parameter(description = "分类ID", required = false)
        @QueryParam("classifyId")
        classifyId: String?,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<JobImageItem>?>

    @Operation(summary = "根据项目标识获取商店镜像列表")
    @GET
    @Path("/image/jobMarketImages")
    fun getJobMarketImagesByProjectCode(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目标识", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @Parameter(description = "机器类型", required = false)
        @QueryParam("agentType")
        agentType: ImageAgentTypeEnum,
        @Parameter(description = "是否推荐", required = false)
        @QueryParam("recommendFlag")
        recommendFlag: Boolean?,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<JobMarketImageItem?>?>

    @Operation(summary = "根据项目标识与镜像名称模糊搜索商店镜像列表（已安装+未安装）")
    @POST
    @Path("/image/jobMarketImages/search")
    fun searchJobMarketImages(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目标识", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @Parameter(description = "机器类型", required = false)
        @QueryParam("agentType")
        agentType: ImageAgentTypeEnum,
        @Parameter(description = "是否推荐", required = false)
        @QueryParam("recommendFlag")
        recommendFlag: Boolean?,
        @Parameter(description = "搜索关键字", required = false)
        @QueryParam("keyword")
        keyword: String?,
        @Parameter(description = "镜像分类Id", required = false)
        @QueryParam("classifyId")
        classifyId: String?,
        @Parameter(description = "应用范畴", required = false)
        @QueryParam("categoryCode")
        categoryCode: String?,
        @Parameter(description = "研发来源", required = false)
        @QueryParam("rdType")
        rdType: ImageRDTypeEnum?,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<JobMarketImageItem?>?>
}
