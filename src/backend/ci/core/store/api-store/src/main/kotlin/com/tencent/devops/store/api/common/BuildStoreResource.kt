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
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_VM_SEQ_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.SensitiveConfResp
import com.tencent.devops.store.pojo.common.StorePkgRunEnvInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_STORE"], description = "build-store")
@Path("/build/store/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildStoreResource {

    @ApiOperation("获取敏感数据")
    @GET
    @Path("/sensitiveConf/types/{storeType}/codes/{storeCode}")
    fun getSensitiveConf(
        @ApiParam(value = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @ApiParam(value = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @ApiParam("组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @ApiParam("组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String
    ): Result<List<SensitiveConfResp>?>

    @ApiOperation("获取组件安装包运行时环境信息")
    @GET
    @Path("/pkg/envs/types/{storeType}/languages/{language}/versions/{runtimeVersion}/get")
    fun getStorePkgRunEnvInfo(
        @ApiParam("组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @ApiParam("开发语言", required = true)
        @PathParam("language")
        language: String,
        @ApiParam("运行时版本", required = true)
        @PathParam("runtimeVersion")
        runtimeVersion: String,
        @ApiParam("支持的操作系统名称", required = true)
        @QueryParam("osName")
        osName: String,
        @ApiParam("支持的操作系统架构", required = true)
        @QueryParam("osArch")
        osArch: String
    ): Result<StorePkgRunEnvInfo?>
}
