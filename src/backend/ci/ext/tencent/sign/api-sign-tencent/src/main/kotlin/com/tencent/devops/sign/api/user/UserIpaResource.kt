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

package com.tencent.devops.sign.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_SIGN_INFO
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.sign.api.pojo.SignDetail
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import java.io.InputStream
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "USER_IPA", description = "用户接口-IPA包")
@Path("/user/ipa/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserIpaResource {

    @Operation(summary = "IPA包签名")
    @POST
    @Path("/sign")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    fun ipaSign(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "ipaSignInfoHeader", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_SIGN_INFO)
        ipaSignInfoHeader: String,
        @Parameter(description = "IPA包文件", required = true)
        ipaInputStream: InputStream
    ): Result<String?>

    @Operation(summary = "IPA包签名状态")
    @GET
    @Path("/sign/{resignId}/status")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    fun getSignStatus(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "签名任务ID", required = true)
        @PathParam("resignId")
        resignId: String
    ): Result<String>

    @Operation(summary = "IPA包签名详情")
    @GET
    @Path("/sign/{resignId}/detail")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    fun getSignDetail(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "签名任务ID", required = true)
        @PathParam("resignId")
        resignId: String
    ): Result<SignDetail>

    @Operation(summary = "获取签名后IPA的下载地址")
    @GET
    @Path("/sign/{resignId}/downloadUrl")
    fun downloadUrl(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "签名任务ID", required = true)
        @PathParam("resignId")
        resignId: String
    ): Result<String>
}
