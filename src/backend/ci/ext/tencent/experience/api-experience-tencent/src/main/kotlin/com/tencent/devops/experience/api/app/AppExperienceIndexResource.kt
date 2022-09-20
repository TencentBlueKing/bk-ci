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

package com.tencent.devops.experience.api.app

import com.tencent.devops.common.api.auth.AUTH_HEADER_PLATFORM
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.index.HotCategoryParam
import com.tencent.devops.experience.pojo.index.IndexAppInfoVO
import com.tencent.devops.experience.pojo.index.IndexBannerVO
import com.tencent.devops.experience.pojo.index.NewCategoryParam
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.BeanParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["APP_EXPERIENCE_INDEX"], description = "版本体验-首页")
@Path("/app/experiences/index")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AppExperienceIndexResource {

    @ApiOperation("banner列表")
    @Path("/banners")
    @GET
    fun banners(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("平台", required = true)
        @HeaderParam(AUTH_HEADER_PLATFORM)
        platform: Int,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int,
        @ApiParam("每页数目", required = false)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<Pagination<IndexBannerVO>>

    @ApiOperation("热门推荐")
    @Path("/hots")
    @GET
    fun hots(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("平台", required = true)
        @HeaderParam(AUTH_HEADER_PLATFORM)
        platform: Int,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int,
        @ApiParam("每页数目", required = false)
        @QueryParam("pageSize")
        pageSize: Int,
        @ApiParam("是否包含外部跳转", required = false)
        @QueryParam("includeExternalUrl")
        includeExternalUrl: Boolean? = false
    ): Result<Pagination<IndexAppInfoVO>>

    @ApiOperation("鹅厂必备")
    @Path("/necessary")
    @GET
    fun necessary(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("平台", required = true)
        @HeaderParam(AUTH_HEADER_PLATFORM)
        platform: Int,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int,
        @ApiParam("每页数目", required = false)
        @QueryParam("pageSize")
        pageSize: Int,
        @ApiParam("是否包含外部跳转", required = false)
        @QueryParam("includeExternalUrl")
        includeExternalUrl: Boolean? = false
    ): Result<Pagination<IndexAppInfoVO>>

    @ApiOperation("本周最新")
    @Path("/newest")
    @GET
    fun newest(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("平台", required = true)
        @HeaderParam(AUTH_HEADER_PLATFORM)
        platform: Int,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int,
        @ApiParam("每页数目", required = false)
        @QueryParam("pageSize")
        pageSize: Int,
        @ApiParam("是否包含外部跳转", required = false)
        @QueryParam("includeExternalUrl")
        includeExternalUrl: Boolean? = false
    ): Result<Pagination<IndexAppInfoVO>>

    @ApiOperation("分类列表--热门")
    @Path("/category/{categoryId}/hot")
    @GET
    fun hotCategory(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("平台", required = true)
        @HeaderParam(AUTH_HEADER_PLATFORM)
        platform: Int,
        @BeanParam
        hotCategoryParam: HotCategoryParam

    ): Result<Pagination<IndexAppInfoVO>>

    @ApiOperation("分类列表--最新")
    @Path("/category/{categoryId}/new")
    @GET
    fun newCategory(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("平台", required = true)
        @HeaderParam(AUTH_HEADER_PLATFORM)
        platform: Int,
        @BeanParam
        newCategoryParam: NewCategoryParam
    ): Result<Pagination<IndexAppInfoVO>>

    @ApiOperation("MiniGame--公开体验")
    @Path("/minigame")
    @GET
    fun miniGameExperience(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("平台", required = true)
        @HeaderParam(AUTH_HEADER_PLATFORM)
        platform: Int
    ): Result<List<IndexAppInfoVO>>

    @ApiOperation("MiniGame--展示图")
    @Path("/minigame/picture")
    @GET
    fun miniGamePicture(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<String>
}
