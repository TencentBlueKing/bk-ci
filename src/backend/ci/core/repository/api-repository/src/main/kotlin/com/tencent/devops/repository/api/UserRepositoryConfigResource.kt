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

package com.tencent.devops.repository.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.RepositoryConfigVisibility
import com.tencent.devops.repository.pojo.RepositoryConfigLogoInfo
import com.tencent.devops.repository.pojo.RepositoryScmConfigReq
import com.tencent.devops.repository.pojo.RepositoryScmConfigVo
import com.tencent.devops.repository.pojo.RepositoryScmProviderVo
import com.tencent.devops.repository.pojo.ScmConfigBaseInfo
import com.tencent.devops.repository.pojo.enums.ScmConfigStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_REPOSITORY_MANAGER", description = "用户-源代码管理")
@Path("/user/repositories/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserRepositoryConfigResource {

    @Operation(summary = "获取代码库配置列表")
    @GET
    @Path("/")
    fun list(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "代码库类型", required = false)
        @QueryParam("scmType")
        scmType: ScmType?
    ): Result<List<ScmConfigBaseInfo>>

    @Operation(summary = "获取代码库提供者")
    @GET
    @Path("/listProvider")
    fun listProvider(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<RepositoryScmProviderVo>>

    @Operation(summary = "获取代码库配置列表")
    @GET
    @Path("/listConfig")
    fun listConfig(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "状态", required = false)
        @QueryParam("status")
        status: ScmConfigStatus?,
        @Parameter(description = "排除的状态", required = false)
        @QueryParam("status")
        excludeStatus: ScmConfigStatus?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<SQLPage<RepositoryScmConfigVo>>

    @Operation(summary = "创建代码库配置")
    @POST
    @Path("/")
    fun create(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        request: RepositoryScmConfigReq
    ): Result<Boolean>

    @Operation(summary = "编辑代码库配置")
    @PUT
    @Path("/{scmCode}")
    fun edit(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "scmCode", required = true)
        @PathParam("scmCode")
        scmCode: String,
        request: RepositoryScmConfigReq
    ): Result<Boolean>

    @Operation(summary = "启用代码库配置")
    @PUT
    @Path("/{scmCode}/enable")
    fun enable(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "scmCode", required = true)
        @PathParam("scmCode")
        scmCode: String
    ): Result<Boolean>

    @Operation(summary = "禁用代码库配置")
    @PUT
    @Path("/{scmCode}/disable")
    fun disable(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "scmCode", required = true)
        @PathParam("scmCode")
        scmCode: String
    ): Result<Boolean>

    @Operation(summary = "删除代码库配置")
    @DELETE
    @Path("/{scmCode}")
    fun delete(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "scmCode", required = true)
        @PathParam("scmCode")
        scmCode: String
    ): Result<Boolean>

    @Operation(summary = "上传logo")
    @POST
    @Path("/uploadLogo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadLogo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "contentLength", required = true)
        @HeaderParam("content-length")
        contentLength: Long,
        @Parameter(description = "logo", required = true)
        @FormDataParam("logo")
        inputStream: InputStream,
        @FormDataParam("logo")
        disposition: FormDataContentDisposition
    ): Result<RepositoryConfigLogoInfo?>

    @Operation(summary = "获取目标代码库支持的触发事件")
    @GET
    @Path("/{scmCode}/events")
    fun supportEvents(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "scmCode", required = true)
        @PathParam("scmCode")
        scmCode: String
    ): Result<List<IdValue>>

    @Operation(summary = "获取目标代码库支持的事件动作")
    @GET
    @Path("/{scmCode}/events/{eventType}/actions")
    fun supportEventActions(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "scmCode", required = true)
        @PathParam("scmCode")
        scmCode: String,
        @Parameter(description = "eventType", required = false)
        @PathParam("eventType")
        eventType: String
    ): Result<List<IdValue>>

    @Operation(summary = "获取目标代码源的组织架构")
    @GET
    @Path("/{scmCode}/dept")
    fun supportDept(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "scmCode", required = true)
        @PathParam("scmCode")
        scmCode: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<SQLPage<RepositoryConfigVisibility>>

    @Operation(summary = "批量添加目标代码源的组织架构")
    @POST
    @Path("/{scmCode}/dept")
    fun addDept(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "scmCode", required = true)
        @PathParam("scmCode")
        scmCode: String,
        @Parameter(description = "需要添加的代码源管理的组织架构", required = true)
        deptList: List<RepositoryConfigVisibility>? = null
    ): Result<Boolean>

    @Operation(summary = "批量删除目标代码源的组织架构")
    @DELETE
    @Path("/{scmCode}/dept")
    fun deleteDept(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "scmCode", required = true)
        @PathParam("scmCode")
        scmCode: String,
        @Parameter(description = "需要删除的代码源管理的组织架构", required = true)
        deptList: List<Int>? = null
    ): Result<Boolean>
}
