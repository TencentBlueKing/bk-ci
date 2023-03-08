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

package com.tencent.devops.artifactory.api

import com.tencent.devops.artifactory.pojo.ArchiveAtomResponse
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_ARTIFACTORY"], description = "仓库-插件")
@Path("/service/artifactories/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceArchiveAtomFileResource {

    @ApiOperation("归档插件包资源")
    @POST
    @Path("/archiveAtom")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun archiveAtomFile(
        @ApiParam("userId", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("项目编码", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @ApiParam("插件ID", required = true)
        @QueryParam("atomId")
        atomId: String,
        @ApiParam("插件代码", required = true)
        @QueryParam("atomCode")
        atomCode: String,
        @ApiParam("插件版本号", required = true)
        @QueryParam("version")
        version: String,
        @ApiParam("发布类型", required = true)
        @QueryParam("releaseType")
        releaseType: ReleaseTypeEnum,
        @ApiParam("文件", required = true)
        @FormDataParam("file")
        inputStream: InputStream,
        @FormDataParam("file")
        disposition: FormDataContentDisposition,
        @ApiParam("支持的操作系统", required = true)
        @QueryParam("os")
        os: String
    ): Result<ArchiveAtomResponse?>

    @ApiOperation("上传插件资源文件到指定自定义仓库路径")
    @POST
    @Path("/file/uploadToPath")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadToPath(
        @ApiParam("userId", required = true)
        @QueryParam("userId")
        @BkField(required = true)
        userId: String,
        @ApiParam("项目代码", required = true)
        @QueryParam("projectId")
        @BkField(required = true)
        projectId: String,
        @ApiParam("文件路径", required = true)
        @QueryParam("path")
        @BkField(required = true)
        path: String,
        @ApiParam("文件类型", required = true)
        @QueryParam("fileType")
        @BkField(required = true)
        fileType: String,
        @ApiParam("文件", required = true)
        @FormDataParam("file")
        inputStream: InputStream,
        @FormDataParam("file")
        disposition: FormDataContentDisposition
    ): Result<String?>
}
