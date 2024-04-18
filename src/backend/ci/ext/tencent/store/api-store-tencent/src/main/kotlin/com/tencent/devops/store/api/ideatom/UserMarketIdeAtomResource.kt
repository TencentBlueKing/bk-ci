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

package com.tencent.devops.store.api.ideatom

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.ideatom.IdeAtomDetail
import com.tencent.devops.store.pojo.ideatom.MarketIdeAtomMainItem
import com.tencent.devops.store.pojo.ideatom.MarketIdeAtomResp
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import com.tencent.devops.store.pojo.ideatom.enums.MarketIdeAtomSortTypeEnum
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "USER_MARKET_IDE_ATOM", description = "IDE插件")
@Path("/user/market/ideAtom")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserMarketIdeAtomResource {

    @Operation(summary = "获取IDE插件市场首页的数据")
    @Path("/list/main")
    @GET
    fun mainPageList(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<MarketIdeAtomMainItem>>

    @Operation(summary = "IDE插件市场搜索插件")
    @GET
    @Path("/atom/list/")
    fun queryIdeAtomList(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "搜索关键字", required = false)
        @QueryParam("keyword")
        keyword: String?,
        @Parameter(description = "插件范畴", required = false)
        @QueryParam("categoryCode")
        categoryCode: String?,
        @Parameter(description = "插件分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @Parameter(description = "功能标签(多个用逗号分隔)", required = false)
        @QueryParam("labelCode")
        labelCode: String?,
        @Parameter(description = "评分", required = false)
        @QueryParam("score")
        score: Int?,
        @Parameter(description = "研发来源(蓝盾 第三方)", required = false)
        @QueryParam("rdType")
        rdType: IdeAtomTypeEnum?,
        @Parameter(description = "排序", required = false)
        @QueryParam("sortType")
        sortType: MarketIdeAtomSortTypeEnum? = MarketIdeAtomSortTypeEnum.DOWNLOAD_COUNT,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<MarketIdeAtomResp>

    @Operation(summary = "根据IDE插件标识获取插件正式版本详情")
    @GET
    @Path("/atomCodes/{atomCode}")
    fun getIdeAtomByCode(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<IdeAtomDetail?>
}
