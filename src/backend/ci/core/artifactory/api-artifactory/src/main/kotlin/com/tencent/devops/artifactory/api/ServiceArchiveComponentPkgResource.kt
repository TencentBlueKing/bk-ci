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

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_ARTIFACTORY_STORE", description = "仓库-研发商店")
@Path("/service/artifactories/store/component")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceArchiveComponentPkgResource {

    @Operation(summary = "获取组件包文件下载链接")
    @GET
    @Path("/types/{storeType}/codes/{storeCode}/versions/{version}/pkg/download/url/get")
    @Suppress("LongParameterList")
    fun getComponentPkgDownloadUrl(
        @Parameter(description = "用户Id", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目Id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: StoreTypeEnum,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeCode: String,
        @Parameter(description = "组件版本号", required = true)
        @PathParam("version")
        version: String,
        @Parameter(description = "操作系统名称", required = false)
        @QueryParam("osName")
        osName: String? = null,
        @Parameter(description = "操作系统架构", required = false)
        @QueryParam("osArch")
        osArch: String? = null
    ): Result<String>

    @Operation(summary = "删除组件包文件")
    @DELETE
    @Path("/types/{storeType}/file/delete")
    fun deleteStorePkg(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件代码", required = true)
        @QueryParam("storeCode")
        storeCode: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: StoreTypeEnum
    ): Result<Boolean>
}
