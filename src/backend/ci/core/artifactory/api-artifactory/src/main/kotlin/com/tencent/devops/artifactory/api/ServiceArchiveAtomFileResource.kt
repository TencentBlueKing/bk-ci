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

package com.tencent.devops.artifactory.api

import com.tencent.devops.artifactory.pojo.ArchiveAtomResponse
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_ARTIFACTORY", description = "仓库-插件")
@Path("/service/artifactories/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceArchiveAtomFileResource {

    @Operation(summary = "归档插件包资源")
    @POST
    @Path("/archiveAtom")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun archiveAtomFile(
        @Parameter(description = "userId", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "项目编码", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @Parameter(description = "插件代码", required = true)
        @QueryParam("atomCode")
        atomCode: String,
        @Parameter(description = "插件版本号", required = true)
        @QueryParam("version")
        version: String,
        @Parameter(description = "发布类型", required = true)
        @QueryParam("releaseType")
        releaseType: ReleaseTypeEnum,
        @Parameter(description = "文件", required = true)
        @FormDataParam("file")
        inputStream: InputStream,
        @FormDataParam("file")
        disposition: FormDataContentDisposition,
        @Parameter(description = "支持的操作系统", required = true)
        @QueryParam("os")
        os: String
    ): Result<ArchiveAtomResponse?>

    @Operation(summary = "上传插件资源文件到指定自定义仓库路径")
    @POST
    @Path("/file/uploadToPath")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadToPath(
        @Parameter(description = "userId", required = true)
        @QueryParam("userId")
        @BkField(required = true)
        userId: String,
        @Parameter(description = "项目代码", required = true)
        @QueryParam("projectId")
        @BkField(required = true)
        projectId: String,
        @Parameter(description = "文件路径", required = true)
        @QueryParam("path")
        @BkField(required = true)
        path: String,
        @Parameter(description = "文件类型", required = true)
        @QueryParam("fileType")
        @BkField(required = true)
        fileType: String,
        @Parameter(description = "文件", required = true)
        @FormDataParam("file")
        inputStream: InputStream,
        @FormDataParam("file")
        disposition: FormDataContentDisposition
    ): Result<String?>
}
