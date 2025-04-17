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

package com.tencent.devops.store.api.common

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ENV
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_VM_SEQ_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.env.StorePkgRunEnvInfo
import com.tencent.devops.store.pojo.common.publication.StoreProcessInfo
import com.tencent.devops.store.pojo.common.sensitive.SensitiveConfResp
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import java.io.InputStream
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam

@Tag(name = "BUILD_STORE", description = "build-store")
@Path("/build/store/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildStoreResource {

    @Operation(summary = "获取敏感数据")
    @GET
    @Path("/sensitiveConf/types/{storeType}/codes/{storeCode}")
    fun getSensitiveConf(
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String
    ): Result<List<SensitiveConfResp>?>

    @Operation(summary = "获取组件安装包运行时环境信息")
    @GET
    @Path("/pkg/envs/types/{storeType}/languages/{language}/versions/{runtimeVersion}/get")
    fun getStorePkgRunEnvInfo(
        @Parameter(description = "环境信息", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_ENV)
        devopsEnv: String? = null,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "开发语言", required = true)
        @PathParam("language")
        language: String,
        @Parameter(description = "运行时版本", required = true)
        @PathParam("runtimeVersion")
        runtimeVersion: String,
        @Parameter(description = "支持的操作系统名称", required = true)
        @QueryParam("osName")
        osName: String,
        @Parameter(description = "支持的操作系统架构", required = true)
        @QueryParam("osArch")
        osArch: String
    ): Result<StorePkgRunEnvInfo?>

    @Operation(summary = "根据组件包一键部署组件")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/deploy")
    fun oneClickDeployComponent(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "storeCode", required = true)
        @FormDataParam("storeCode")
        storeCode: String,
        @Parameter(description = "storeType", required = true)
        @FormDataParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "文件", required = true)
        @FormDataParam("file")
        inputStream: InputStream,
        @FormDataParam("file")
        disposition: FormDataContentDisposition
    ): Result<String?>

    @Operation(summary = "根据组件版本ID获取组件发布版本进度")
    @GET
    @Path("/release/ids/{storeId}")
    fun getProcessInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "storeId", required = true)
        @PathParam("storeId")
        storeId: String
    ): Result<StoreProcessInfo>
}
