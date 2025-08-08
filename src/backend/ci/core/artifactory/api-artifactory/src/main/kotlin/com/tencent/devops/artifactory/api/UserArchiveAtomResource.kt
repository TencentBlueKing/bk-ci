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
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_ARTIFACTORY", description = "仓库-插件")
@Path("/user/artifactories/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)@Suppress("ALL")
interface UserArchiveAtomResource {

    @Operation(summary = "归档插件包")
    @POST
    @Path("/projects/{projectCode}/ids/{atomId}/codes/{atomCode}/versions/{version}/types/{releaseType}/archive")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun archiveAtom(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "插件ID", required = true)
        @PathParam("atomId")
        atomId: String,
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "插件版本号", required = true)
        @PathParam("version")
        version: String,
        @Parameter(description = "发布类型", required = true)
        @PathParam("releaseType")
        releaseType: ReleaseTypeEnum,
        @Parameter(description = "文件", required = true)
        @FormDataParam("file")
        inputStream: InputStream,
        @FormDataParam("file")
        disposition: FormDataContentDisposition,
        @Parameter(description = "支持的操作系统", required = true)
        @FormDataParam("os")
        os: String
    ): Result<ArchiveAtomResponse?>

    @Operation(summary = "重新归档插件包")
    @POST
    @Path("/projects/{projectCode}/ids/{atomId}/codes/{atomCode}/versions/{version}/re/archive")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun reArchiveAtom(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "插件ID", required = true)
        @PathParam("atomId")
        atomId: String,
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "插件版本号", required = true)
        @PathParam("version")
        version: String,
        @Parameter(description = "文件", required = true)
        @FormDataParam("file")
        inputStream: InputStream,
        @FormDataParam("file")
        disposition: FormDataContentDisposition,
        @Parameter(description = "插件字段校验确认标识", required = false)
        @QueryParam("fieldCheckConfirmFlag")
        fieldCheckConfirmFlag: Boolean?
    ): Result<ArchiveAtomResponse?>
}
