/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_FILE"], description = "服务-创建异步任务")
@Path("/build/file")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildFileResource {

    @ApiOperation("上传待加固文件")
    @POST
    @Path("/security/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun securityUpload(
        @ApiParam(value = "文件流", required = true)
        @FormDataParam("file")
        fileStream: InputStream,
        @ApiParam(value = "环境id", required = true)
        @HeaderParam("envId")
        envId: String,
        @ApiParam(value = "文件名", required = true)
        @HeaderParam("fileName")
        fileName: String,
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam("原子ID", required = true)
        @HeaderParam("elementId")
        elementId: String
    ): Result<String>

    @ApiOperation("上传企业签名包")
    @POST
    @Path("/enterprise/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun enterpriseSignUpload(
        @ApiParam(value = "文件流", required = true)
        @FormDataParam("file")
        fileStream: InputStream,
        @ApiParam(value = "文件md5", required = true)
        @QueryParam("md5")
        md5: String,
        @ApiParam(value = "文件大小", required = true)
        @QueryParam("size")
        size: String,
        @ApiParam(value = "文件名字", required = true)
        @QueryParam("fileName")
        fileName: String,
        @ApiParam(value = "文件属性", required = true)
        @QueryParam("props")
        props: String,
        @ApiParam(value = "原子id", required = true)
        @QueryParam("elementId")
        elementId: String,
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String
    ): Result<String>
}