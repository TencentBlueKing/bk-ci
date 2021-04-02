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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.ideatom.ExternalIdeAtomResp
import com.tencent.devops.store.pojo.ideatom.IdeAtom
import com.tencent.devops.store.pojo.ideatom.InstallIdeAtomReq
import com.tencent.devops.store.pojo.ideatom.InstallIdeAtomResp
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import com.tencent.devops.store.pojo.ideatom.enums.MarketIdeAtomSortTypeEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["EXTERNAL_MARKET_IDE_ATOM"], description = "IDE中调用的接口")
@Path("/external/market/ideAtom")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalMarketIdeAtomResource {

    @ApiOperation("插件列表")
    @GET
    @Path("/list/")
    fun list(
        @ApiParam("IDE类型", required = true)
        @QueryParam("categoryCode")
        categoryCode: String,
        @ApiParam("插件名称", required = false)
        @QueryParam("atomName")
        atomName: String?,
        @ApiParam("插件分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @ApiParam("功能标签(多个用逗号分隔)", required = false)
        @QueryParam("labelCodes")
        labelCodes: String?,
        @ApiParam("评分", required = false)
        @QueryParam("score")
        score: Int?,
        @ApiParam("研发来源(蓝盾 第三方)", required = false)
        @QueryParam("rdType")
        rdType: IdeAtomTypeEnum?,
        @ApiParam("排序", required = false)
        @QueryParam("sortType")
        sortType: MarketIdeAtomSortTypeEnum? = MarketIdeAtomSortTypeEnum.DOWNLOAD_COUNT,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<ExternalIdeAtomResp>

    @ApiOperation("根据插件代码获取IDE插件信息")
    @GET
    @Path("/atomCodes/{atomCode}")
    fun getIdeAtomsByCode(
        @ApiParam("插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<IdeAtom?>

    @ApiOperation("安装IDE插件")
    @POST
    @Path("/install")
    fun installIdeAtom(
        @ApiParam("安装IDE插件请求报文体", required = true)
        installIdeAtomReq: InstallIdeAtomReq
    ): Result<InstallIdeAtomResp?>
}
