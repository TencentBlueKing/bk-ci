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
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import com.tencent.devops.store.pojo.common.classify.Classify
import com.tencent.devops.store.pojo.common.enums.ErrorCodeTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreBuildResultRequest
import com.tencent.devops.store.pojo.common.sensitive.SensitiveConfResp
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_STORE", description = "service-store")
@Path("/service/store")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceStoreResource {

    @Operation(summary = "卸载")
    @DELETE
    @Path("/codes/{storeCode}/uninstall")
    fun uninstall(
        @Parameter(description = "标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "项目", required = true)
        @QueryParam("projectCode")
        projectCode: String
    ): Result<Boolean>

    @Operation(summary = "获取敏感数据")
    @GET
    @Path("/getSensitiveConf")
    fun getSensitiveConf(
        @Parameter(description = "组件类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "组件标识", required = true)
        @QueryParam("storeCode")
        storeCode: String
    ): Result<List<SensitiveConfResp>?>

    @Operation(summary = "store组件内置流水线构建结果处理")
    @PUT
    @Path("/pipelineIds/{pipelineId}/buildIds/{buildId}/build/handle")
    fun handleStoreBuildResult(
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "store组件内置流水线构建结果请求报文体", required = true)
        storeBuildResultRequest: StoreBuildResultRequest
    ): Result<Boolean>

    @Operation(summary = "判断用户是否是该组件的成员")
    @GET
    @Path("/codes/{storeCode}/user/validate")
    fun isStoreMember(
        @Parameter(description = "标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "用户ID", required = true)
        @QueryParam("userId")
        userId: String
    ): Result<Boolean>

    @Operation(summary = "判断项目是否是研发商店公共项目")
    @GET
    @Path("projects/{projectCode}/validate")
    fun isPublicProject(
        @Parameter(description = "标识", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<Boolean>

    @Operation(summary = "校验流水线用户访问组件信息权限")
    @GET
    @Path("/codes/{storeCode}/pipeline/visit/validate")
    fun validatePipelineUserStorePermission(
        @Parameter(description = "标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "用户ID", required = true)
        @QueryParam("userId")
        userId: String
    ): Result<Boolean>

    @Operation(summary = "判断错误码是否合规")
    @POST
    @Path("/codes/{storeCode}/errorCode/compliance")
    fun isComplianceErrorCode(
        @Parameter(description = "标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "错误码", required = true)
        @QueryParam("errorCode")
        errorCode: Int,
        @Parameter(description = "错误码类型", required = true)
        @QueryParam("errorCodeType")
        errorCodeType: ErrorCodeTypeEnum
    ): Result<Boolean>

    @GET
    @Path("/types/{storeType}/codes/{storeCode}/versions/{version}/permission/validate")
    @Operation(summary = "校验是否有使用该组件的权限")
    fun validateComponentDownloadPermission(
        @Parameter(description = "标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "版本号", required = true)
        @PathParam("version")
        version: String,
        @Parameter(description = "项目", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @Parameter(description = "用户ID", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "实例ID", required = false)
        @QueryParam("instanceId")
        instanceId: String? = null
    ): Result<StoreBaseInfo?>

    @Operation(summary = "获取组件分类信息列表")
    @GET
    @Path("/classifies/types/{storeType}/list")
    fun getClassifyList(
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum
    ): Result<List<Classify>>
}
