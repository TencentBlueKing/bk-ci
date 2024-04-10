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

package com.tencent.devops.experience.api.op

import ExperiencePublicExternalAdd
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.enums.PlatformEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.ExperienceExtendBanner
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "EXPERIENCE_OP", description = "版本体验-OP")
@Path("/op/experience")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpExperienceResource {
    @Operation(summary = "修改鹅厂必备")
    @Path("/public/switchNecessary")
    @POST
    fun switchNecessary(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "公开体验记录ID", required = true)
        @QueryParam("id")
        id: Long
    ): Result<String>

    @Operation(summary = "修改鹅厂必备顺序")
    @Path("/public/setNecessaryIndex")
    @POST
    fun setNecessaryIndex(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "公开体验记录ID", required = true)
        @QueryParam("id")
        id: Long,
        @Parameter(description = "鹅厂必备顺序", required = true)
        @QueryParam("necessaryIndex")
        necessaryIndex: Int
    ): Result<String>

    @Operation(summary = "修改公开体验banner")
    @Path("/public/setBannerUrl")
    @POST
    fun setBannerUrl(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "公开体验记录ID", required = true)
        @QueryParam("id")
        id: Long,
        @Parameter(description = "banner地址", required = true)
        @QueryParam("bannerUrl")
        bannerUrl: String
    ): Result<String>

    @Operation(summary = "修改公开体验banner顺序")
    @Path("/public/setBannerIndex")
    @POST
    fun setBannerIndex(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "公开体验记录ID", required = true)
        @QueryParam("id")
        id: Long,
        @Parameter(description = "banner顺序", required = true)
        @QueryParam("bannerIndex")
        bannerIndex: Int
    ): Result<String>

    @Operation(summary = "公开体验上下线")
    @Path("/public/switchOnline")
    @POST
    fun switchOnline(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "公开体验记录ID", required = true)
        @QueryParam("id")
        id: Long
    ): Result<String>

    @Operation(summary = "新增搜索推荐")
    @Path("/search/addRecommend")
    @POST
    fun addRecommend(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "推荐内容", required = true)
        @QueryParam("content")
        content: String,
        @Parameter(description = "平台", required = true)
        @QueryParam("platform")
        platform: PlatformEnum
    ): Result<String>

    @Operation(summary = "删除搜索推荐")
    @Path("/search/removeRecommend")
    @DELETE
    fun removeRecommend(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "搜索推荐ID", required = true)
        @QueryParam("id")
        id: Long
    ): Result<String>

    @Operation(summary = "新增外部链接公开体验")
    @Path("/public/addExternal")
    @POST
    fun addExternal(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "外部体验参数")
        externalAdd: ExperiencePublicExternalAdd
    ): Result<String>

    @Operation(summary = "添加扩展banner")
    @Path("/index/addExtendBanner")
    @POST
    fun addExtendBanner(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "扩展banner")
        experienceExtendBanner: ExperienceExtendBanner
    ): Result<Int>

    @Operation(summary = "修改扩展banner")
    @Path("/index/updateExtendBanner")
    @POST
    fun updateExtendBanner(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "bannerId", required = true)
        @QueryParam("bannerId")
        bannerId: Long,
        @Parameter(description = "扩展banner")
        experienceExtendBanner: ExperienceExtendBanner
    ): Result<Int>
}
