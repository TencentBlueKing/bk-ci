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
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.InstalledPkgFileShaContentRequest
import com.tencent.devops.store.pojo.common.MyStoreComponent
import com.tencent.devops.store.pojo.common.StoreBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreApproveReleaseRequest
import com.tencent.devops.store.pojo.common.publication.StoreOfflineRequest
import com.tencent.devops.store.pojo.common.version.StoreDeskVersionItem
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_STORE_COMPONENT", description = "研发商店-OP-组件管理")
@Path("/op/store/components")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpStoreComponentResource {

    @Operation(summary = "审核组件发布")
    @PUT
    @Path("/{storeId}/release/approve")
    fun approveComponentRelease(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件ID", required = true)
        @PathParam("storeId")
        storeId: String,
        @Parameter(description = "审核组件发布请求请求报文")
        storeApproveReleaseRequest: StoreApproveReleaseRequest
    ): Result<Boolean>

    @Operation(summary = "获取组件列表")
    @GET
    @Path("/types/{storeType}/component/list")
    @BkInterfaceI18n(
        keyPrefixNames = ["{data.records[*].storeType}", "{data.records[*].storeCode}", "{data.records[*].version}",
            "releaseInfo"]
    )
    @Suppress("LongParameterList")
    fun listComponents(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: String,
        @Parameter(description = "名称", required = false)
        @QueryParam("name")
        name: String?,
        @Parameter(description = "类型", required = false)
        @QueryParam("type")
        type: String?,
        @Parameter(description = "是否处于流程中", required = false)
        @QueryParam("processFlag")
        processFlag: Boolean?,
        @Parameter(description = "分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @Parameter(description = "应用范畴，多个用逗号分隔", required = false)
        @QueryParam("categoryCodes")
        categoryCodes: String?,
        @Parameter(description = "功能标签，多个用逗号分隔", required = false)
        @QueryParam("labelCodes")
        labelCodes: String?,
        @Parameter(description = "排序", required = false)
        @QueryParam("sortType")
        sortType: StoreSortTypeEnum? = StoreSortTypeEnum.CREATE_TIME,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        @BkField(patternStyle = BkStyleEnum.NUMBER_STYLE)
        page: Int = 1,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE)
        pageSize: Int = 10
    ): Result<Page<MyStoreComponent>?>

    @Operation(summary = "根据组件标识获取组件版本列表")
    @GET
    @Path("/types/{storeType}/codes/{storeCode}/component/version/list")
    @BkInterfaceI18n(
        keyPrefixNames = ["{data.records[*].storeType}", "{data.records[*].storeCode}", "{data.records[*].version}",
            "releaseInfo"]
    )
    fun getComponentVersionsByCode(
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
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        @BkField(patternStyle = BkStyleEnum.NUMBER_STYLE)
        page: Int = 1,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE)
        pageSize: Int = 10
    ): Result<Page<StoreDeskVersionItem>>

    @Operation(summary = "根据组件ID获取组件详情")
    @GET
    @Path("/types/{storeType}/ids/{storeId}/component/detail")
    @BkInterfaceI18n(
        keyPrefixNames = ["{data.storeType}", "{data.storeCode}", "{data.version}", "releaseInfo"]
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

    @Operation(summary = "更新组件基本信息")
    @PUT
    @Path("/types/{storeType}/codes/{storeCode}/component/base/info/update")
    fun updateComponentBaseInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
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
        @Parameter(description = "组件基本信息修改请求报文体", required = true)
        storeBaseInfoUpdateRequest: StoreBaseInfoUpdateRequest
    ): Result<Boolean>

    @Operation(summary = "删除工作台组件")
    @DELETE
    @Path("/desk/types/{storeType}/codes/{storeCode}/component/delete")
    fun deleteComponent(
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
        storeCode: String
    ): Result<Boolean>

    @Operation(summary = "下架组件")
    @PUT
    @Path("/component/offline")
    fun offlineComponent(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作台-下架组件请求报文体", required = true)
        @Valid
        storeOfflineRequest: StoreOfflineRequest
    ): Result<Boolean>

    @Operation(summary = "更新组件已安装包文件sha1摘要值")
    @PUT
    @Path("/types/{storeType}/codes/{storeCode}/versions/{version}/component/installed/pkg/file/sha/update")
    fun updateComponentInstalledPkgFileShaContent(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: StoreTypeEnum,
        @Parameter(description = "组件代码", required = true)
        @PathParam("storeCode")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeCode: String,
        @Parameter(description = "组件版本", required = true)
        @PathParam("version")
        version: String,
        @Parameter(description = "更新组件已安装包文件sha1摘要值请求报文", required = true)
        installedPkgFileShaContentRequest: InstalledPkgFileShaContentRequest
    ): Result<Boolean>
}
