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

package com.tencent.devops.sign.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_SIGN_INFO
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.sign.api.pojo.IpaUploadInfo
import com.tencent.devops.sign.api.pojo.SignDetail
import com.tencent.devops.sign.api.pojo.SignHistory
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import java.io.InputStream
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.HeaderParam
import javax.ws.rs.QueryParam
import javax.ws.rs.GET
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_IPA"], description = "服务接口-IPA包")
@Path("/service/ipa")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceIpaResource {

    @ApiOperation("获取签名任务历史")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/history")
    @Path("/sign/history")
    fun getHistorySign(
        @ApiParam("用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("起始时间戳（毫秒）", required = false)
        @QueryParam("startTime")
        startTime: Long?,
        @ApiParam("截止时间戳（毫秒）", required = false)
        @QueryParam("endTime")
        endTime: Long?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<SignHistory>>

    @ApiOperation("IPA包签名")
    @POST
    @Path("/sign")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    fun ipaSign(
        @ApiParam("ipaSignInfoHeader", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_SIGN_INFO)
        ipaSignInfoHeader: String,
        @ApiParam("IPA包文件", required = true)
        ipaInputStream: InputStream,
        @ApiParam("md5Check", required = false)
        @QueryParam("md5Check")
        md5Check: Boolean = true
    ): Result<String>

    @ApiOperation("获取IPA包重签名的上传token")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/getSignToken")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    fun getSignToken(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<IpaUploadInfo>

    @ApiOperation("IPA包签名状态")
    @GET
    @Path("/sign/{resignId}/status")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    fun getSignStatus(
        @ApiParam("签名任务ID", required = true)
        @PathParam("resignId")
        resignId: String
    ): Result<String>

    @ApiOperation("IPA包签名详情")
    @GET
    @Path("/sign/{resignId}/detail")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    fun getSignDetail(
        @ApiParam("签名任务ID", required = true)
        @PathParam("resignId")
        resignId: String
    ): Result<SignDetail>

    @ApiOperation("获取签名后IPA包的下载地址")
    @GET
    @Path("/sign/{resignId}/downloadUrl/")
    fun downloadUrl(
        @ApiParam("签名任务ID", required = true)
        @PathParam("resignId")
        resignId: String
    ): Result<String>
}
