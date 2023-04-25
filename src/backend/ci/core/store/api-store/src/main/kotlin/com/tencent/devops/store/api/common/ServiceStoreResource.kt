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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.SensitiveConfResp
import com.tencent.devops.store.pojo.common.StoreBuildResultRequest
import com.tencent.devops.store.pojo.common.enums.ErrorCodeTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_STORE"], description = "service-store")
@Path("/service/store")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceStoreResource {

    @ApiOperation("卸载")
    @DELETE
    @Path("/codes/{storeCode}/uninstall")
    fun uninstall(
        @ApiParam("标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @ApiParam("类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum,
        @ApiParam("项目", required = true)
        @QueryParam("projectCode")
        projectCode: String
    ): Result<Boolean>

    @ApiOperation("获取敏感数据")
    @GET
    @Path("/getSensitiveConf")
    fun getSensitiveConf(
        @ApiParam("组件类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum,
        @ApiParam("组件标识", required = true)
        @QueryParam("storeCode")
        storeCode: String
    ): Result<List<SensitiveConfResp>?>

    @ApiOperation("store组件内置流水线构建结果处理")
    @PUT
    @Path("/pipelineIds/{pipelineId}/buildIds/{buildId}/build/handle")
    fun handleStoreBuildResult(
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam(value = "store组件内置流水线构建结果请求报文体", required = true)
        storeBuildResultRequest: StoreBuildResultRequest
    ): Result<Boolean>

    @ApiOperation("判断用户是否是该组件的成员")
    @GET
    @Path("/codes/{storeCode}/user/validate")
    fun isStoreMember(
        @ApiParam("标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @ApiParam("类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum,
        @ApiParam("用户ID", required = true)
        @QueryParam("userId")
        userId: String
    ): Result<Boolean>

    @ApiOperation("判断错误码是否合规")
    @POST
    @Path("/codes/{storeCode}/errorCode/compliance")
    fun isComplianceErrorCode(
        @ApiParam("标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @ApiParam("类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum,
        @ApiParam("错误码", required = true)
        @QueryParam("errorCode")
        errorCode: Int,
        @ApiParam("错误码类型", required = true)
        @QueryParam("errorCodeType")
        errorCodeType: ErrorCodeTypeEnum
    ): Result<Boolean>
}
