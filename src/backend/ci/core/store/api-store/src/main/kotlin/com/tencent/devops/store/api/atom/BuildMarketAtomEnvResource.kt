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

package com.tencent.devops.store.api.atom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.atom.AtomEnv
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "BUILD_MARKET_ATOM_ENV", description = "插件市场-插件执行环境")
@Path("/build/market/atom/env/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildMarketAtomEnvResource {

    @Operation(summary = "根据插件代码和版本号查看指定项目下插件执行环境信息")
    @GET
    @Path("/{projectCode}/{atomCode}/{version}")
    fun getAtomEnv(
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "版本号", required = true)
        @PathParam("version")
        version: String,
        @Parameter(description = "插件状态", required = false)
        @QueryParam("atomStatus")
        atomStatus: Byte? = null,
        @Parameter(description = "操作系统名称", required = false)
        @QueryParam("osName")
        osName: String? = null,
        @Parameter(description = "操作系统架构", required = false)
        @QueryParam("osArch")
        osArch: String? = null,
        @Parameter(description = "是否需要转换操作系统相关信息", required = false)
        @QueryParam("convertOsFlag")
        convertOsFlag: Boolean? = null
    ): Result<AtomEnv?>

    @Operation(summary = "插件工作台-更新插件执行环境信息")
    @PUT
    @Path("/{projectCode}/{atomCode}/{version}")
    fun updateMarketAtom(
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "版本号", required = true)
        @PathParam("version")
        version: String,
        @Parameter(description = "插件市场工作台-更新插件执行环境信息请求报文体", required = true)
        atomEnvRequest: AtomEnvRequest
    ): Result<Boolean>
}
