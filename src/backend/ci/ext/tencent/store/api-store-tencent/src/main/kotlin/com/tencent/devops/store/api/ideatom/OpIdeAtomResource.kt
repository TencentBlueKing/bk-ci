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
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.ideatom.IdeAtom
import com.tencent.devops.store.pojo.ideatom.IdeAtomCreateRequest
import com.tencent.devops.store.pojo.ideatom.IdeAtomReleaseRequest
import com.tencent.devops.store.pojo.ideatom.IdeAtomUpdateRequest
import com.tencent.devops.store.pojo.ideatom.OpIdeAtomItem
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OP_MARKET_IDE_ATOM", description = "IDE插件")
@Path("/op/market/ideAtom")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpIdeAtomResource {

    @Operation(summary = "新增IDE插件")
    @POST
    @Path("/")
    fun addIdeAtom(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "新增IDE插件请求报文体", required = true)
        ideAtomCreateRequest: IdeAtomCreateRequest
    ): Result<Boolean>

    @Operation(summary = "更新IDE插件信息")
    @PUT
    @Path("/{atomId}")
    fun updateIdeAtom(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "插件ID", required = true)
        @PathParam("atomId")
        atomId: String,
        @Parameter(description = "更新IDE插件请求报文体", required = true)
        ideAtomUpdateRequest: IdeAtomUpdateRequest
    ): Result<Boolean>

    @Operation(summary = "根据ID删除IDE插件信息")
    @DELETE
    @Path("/atomIds/{atomId}")
    fun deleteIdeAtomById(
        @Parameter(description = "插件ID", required = true)
        @PathParam("atomId")
        atomId: String
    ): Result<Boolean>

    @Operation(summary = "根据ID获取IDE插件信息")
    @GET
    @Path("/atomIds/{atomId}")
    fun getIdeAtomById(
        @Parameter(description = "插件ID", required = true)
        @PathParam("atomId")
        atomId: String
    ): Result<IdeAtom?>

    @Operation(summary = "根据插件代码获取IDE插件信息")
    @GET
    @Path("/atomCodes/{atomCode}")
    fun getIdeAtomsByCode(
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "版本号", required = false)
        @QueryParam("version")
        version: String?
    ): Result<IdeAtom?>

    @Operation(summary = "根据插件代码获取IDE插件版本列表")
    @GET
    @Path("/atomCodes/{atomCode}/versions/list")
    fun getIdeAtomVersionsByCode(
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<OpIdeAtomItem>?>

    @Operation(summary = "获取IDE插件列表")
    @GET
    @Path("/")
    fun listIdeAtoms(
        @Parameter(description = "插件名称", required = false)
        @QueryParam("atomName")
        atomName: String?,
        @Parameter(description = "插件类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发", required = false)
        @QueryParam("atomType")
        atomType: IdeAtomTypeEnum?,
        @Parameter(description = "插件分类代码", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @Parameter(description = "应用范畴(多个用逗号分隔)", required = false)
        @QueryParam("categoryCodes")
        categoryCodes: String?,
        @Parameter(description = "功能标签(多个用逗号分隔)", required = false)
        @QueryParam("labelCodes")
        labelCodes: String?,
        @Parameter(description = "是否处于流程中", required = false)
        @QueryParam("processFlag")
        processFlag: Boolean?,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<Page<OpIdeAtomItem>?>

    @Operation(summary = "发布IDE插件")
    @PUT
    @Path("/release/atomIds/{atomId}")
    fun releaseIdeAtom(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "插件ID", required = true)
        @PathParam("atomId")
        atomId: String,
        @Parameter(description = "IDE插件发布请求报文体", required = true)
        ideAtomReleaseRequest: IdeAtomReleaseRequest
    ): Result<Boolean>

    @Operation(summary = "下架IDE插件")
    @PUT
    @Path("/offline/atomCodes/{atomCode}/versions")
    fun offlineIdeAtom(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "版本号", required = false)
        @QueryParam("version")
        version: String?,
        @Parameter(description = "原因", required = false)
        @QueryParam("reason")
        reason: String?
    ): Result<Boolean>
}
