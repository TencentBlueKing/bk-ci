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

package com.tencent.devops.common.web.service

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_OS_ARCH
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_OS_NAME
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_SHA_CONTENT
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_SIGN_FILE_NAME
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

/**
 * 验证组件是否有调用敏感接口的权限,在store中实现
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/service/sdk/sensitiveApi/")
@ServiceInterface("store")
interface ServiceSensitiveApiPermissionResource {

    /**
     * 验证组件是否有该api接口的权限
     *
     * @param signFileName 签名文件名称
     * @param fileShaContent 文件sha1摘要值
     * @param osName 操作系统名称
     * @param osArch 操作系统CPU架构
     * @param storeCode 组件标识
     * @param apiName api接口名称
     * @param storeType 组件类型
     * @param version 组件版本
     */
    @Path("verify/{storeCode}/{apiName}")
    @GET
    @Suppress("LongParameterList")
    fun verifyApi(
        @Parameter(description = "签名文件名称", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_SIGN_FILE_NAME)
        signFileName: String? = null,
        @Parameter(description = "文件sha1摘要值", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_SHA_CONTENT)
        fileShaContent: String? = null,
        @Parameter(description = "操作系统名称", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_OS_NAME)
        osName: String? = null,
        @Parameter(description = "操作系统CPU架构", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_OS_ARCH)
        osArch: String? = null,
        @PathParam("storeCode")
        @Parameter(description = "组件标识", required = true)
        storeCode: String,
        @PathParam("apiName")
        @Parameter(description = "api接口名称", required = true)
        apiName: String,
        @QueryParam("storeType")
        @Parameter(description = "组件类型", required = true)
        @DefaultValue("ATOM")
        storeType: String = "ATOM",
        @QueryParam("version")
        @Parameter(description = "组件版本", required = false)
        version: String? = null
    ): Result<Boolean>
}
