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
package com.tencent.devops.store.api.common

import com.tencent.devops.common.api.annotation.BkInterfaceI18n
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.media.StoreMediaInfo
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

@Tag(name = "SERVICE_STORE_COMPONENT_BASE", description = "研发商店-SERVICE-组件基础/详情")
@Path("/service/store/components")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceStoreComponentBaseResource {

    @Operation(summary = "根据组件ID获取组件详情")
    @GET
    @Path("/types/{storeType}/ids/{storeId}/component/detail")
    @BkInterfaceI18n(
        keyPrefixNames = [
            "{data.storeType}", "{data.storeCode}", "{data.version}",
            "releaseInfo"
        ]
    )
    fun getComponentDetailInfoById(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: String,
        @Parameter(description = "组件ID", required = true)
        @PathParam("storeId")
        @BkField(patternStyle = BkStyleEnum.ID_STYLE, required = false)
        storeId: String
    ): Result<StoreDetailInfo?>

    @Operation(summary = "获取组件基础信息")
    @GET
    @Path("/types/{storeType}/code/{storeCode}/component/base/info")
    @BkInterfaceI18n(
        keyPrefixNames = [
            "{data.storeType}", "{data.storeCode}", "{data.version}",
            "releaseInfo"
        ]
    )
    fun getComponentBaseInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: String,
        @Parameter(description = "组件代码", required = true)
        @PathParam("storeCode")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeCode: String,
        @Parameter(description = "组件版本", required = false)
        @QueryParam("version")
        version: String? = null
    ): Result<StoreBaseInfo?>

    @Operation(summary = "根据组件code和版本号获取组件详情")
    @GET
    @Path("/types/{storeType}/codes/{storeCode}/{version}/component/base/info")
    @BkInterfaceI18n(
        keyPrefixNames = [
            "{data.storeType}", "{data.storeCode}", "{data.version}",
            "releaseInfo"
        ]
    )
    fun getComponentDataInfoByCode(
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: StoreTypeEnum,
        @Parameter(description = "组件CODE", required = true)
        @PathParam("storeCode")
        @BkField(patternStyle = BkStyleEnum.ID_STYLE, required = false)
        storeCode: String,
        @Parameter(description = "组件版本", required = true)
        @PathParam("version")
        version: String,
        @Parameter(description = "版本状态", required = false)
        @QueryParam("status")
        status: StoreStatusEnum? = null
    ): Result<StoreDetailInfo?>

    @Operation(summary = "根据组件code和版本号获取组件详情")
    @POST
    @Path("/types/{storeType}/codes/base/info")
    @BkInterfaceI18n(
        keyPrefixNames = ["{data[*].storeType}", "{data[*].storeCode}", "{data[*].version}", "releaseInfo"]
    )
    fun getComponentBaseInfoByCodes(
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: StoreTypeEnum,
        @Parameter(description = "组件CODE集合", required = false)
        @QueryParam("storeCodes")
        storeCodes: String? = null
    ): Result<List<StoreBaseInfo>>

    @Operation(summary = "获取组件媒体信息")
    @Path("/types/{storeType}/codes/{storeCode}/component/media/info/get")
    @GET
    fun getStoreMediaInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: StoreTypeEnum,
        @Parameter(description = "组件代码", required = true)
        @PathParam("storeCode")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeCode: String
    ): Result<List<StoreMediaInfo>?>
}
