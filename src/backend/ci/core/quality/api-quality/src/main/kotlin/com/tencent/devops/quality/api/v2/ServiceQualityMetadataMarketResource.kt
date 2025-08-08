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

package com.tencent.devops.quality.api.v2

import com.tencent.devops.common.api.constant.IN_READY_TEST
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.api.v2.pojo.op.QualityMetaData
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_METADATA_MARKET", description = "服务-质量红线-插件市场")
@Path("/service/metadata/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceQualityMetadataMarketResource {

    @Operation(summary = "注册插件指标的测试元数据")
    @Path("/setMetadata")
    @POST
    fun setTestMetadata(
        @QueryParam("userId")
        userId: String,
        @QueryParam("atomCode")
        atomCode: String,
        @QueryParam("extra")
        extra: String = IN_READY_TEST,
        metadataList: List<QualityMetaData>
    ): Result<Map<String/* dataId */, Long/* metadataId */>>

    @Operation(summary = "刷新插件指标的元数据")
    @Path("/refreshMetadata")
    @PUT
    fun refreshMetadata(
        @QueryParam("elementType")
        elementType: String
    ): Result<Map<String, String>>

    @Operation(summary = "删除插件指标的测试元数据")
    @Path("/deleteTestMetadata")
    @DELETE
    fun deleteTestMetadata(
        @QueryParam("elementType")
        elementType: String,
        @QueryParam("extra")
        extra: String = IN_READY_TEST
    ): Result<Int>
}
