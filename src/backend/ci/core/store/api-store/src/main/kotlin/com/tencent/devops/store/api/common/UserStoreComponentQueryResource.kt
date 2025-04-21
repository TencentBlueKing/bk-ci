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

import com.tencent.devops.common.api.annotation.BkInterfaceI18n
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.MarketMainItem
import com.tencent.devops.store.pojo.common.MyStoreComponent
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.enums.RdTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.media.StoreMediaInfo
import com.tencent.devops.store.pojo.common.test.StoreTestItem
import com.tencent.devops.store.pojo.common.version.StoreDeskVersionItem
import com.tencent.devops.store.pojo.common.version.StoreShowVersionInfo
import com.tencent.devops.store.pojo.common.version.VersionInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_STORE_COMPONENT", description = "研发商店-组件查询")
@Path("/user/store/components")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserStoreComponentQueryResource {

    @Operation(summary = "根据用户获取工作台组件列表")
    @GET
    @Path("/desk/types/{storeType}/component/list")
    @BkInterfaceI18n(
        keyPrefixNames = ["{data.records[*].storeType}", "{data.records[*].storeCode}", "{data.records[*].version}",
            "releaseInfo"]
    )
    fun getMyComponents(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: String,
        @Parameter(description = "组件名称", required = false)
        @QueryParam("name")
        name: String?,
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
    @Path("/desk/types/{storeType}/codes/{storeCode}/component/version/list")
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

    @Operation(summary = "根据组件代码获取组件详情")
    @GET
    @Path("/types/{storeType}/codes/{storeCode}/component/detail")
    @BkInterfaceI18n(
        keyPrefixNames = ["{data.storeType}", "{data.storeCode}", "{data.version}", "releaseInfo"]
    )
    fun getComponentDetailInfoByCode(
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
    ): Result<StoreDetailInfo?>

    @Operation(summary = "获取研发商店首页组件的数据")
    @Path("/types/{storeType}/component/main/page/list")
    @GET
    @BkInterfaceI18n(
        keyPrefixNames = ["{data[*].records[*].type}", "{data[*].records[*].code}", "{data[*].records[*].version}",
            "releaseInfo"
        ]
    )
    @Suppress("LongParameterList")
    fun getMainPageComponents(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: String,
        @Parameter(description = "项目代码", required = false)
        @QueryParam("projectCode")
        projectCode: String? = null,
        @Parameter(description = "实例ID", required = false)
        @QueryParam("instanceId")
        instanceId: String? = null,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        @BkField(patternStyle = BkStyleEnum.NUMBER_STYLE)
        page: Int = 1,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE)
        pageSize: Int = 8
    ): Result<List<MarketMainItem>>

    @Operation(summary = "根据条件查询组件列表")
    @Path("/types/{storeType}/component/list")
    @GET
    @BkInterfaceI18n(
        keyPrefixNames = [
            "{data.records[*].type}", "{data.records[*].code}", "{data.records[*].version}",
            "releaseInfo"
        ]
    )
    @Suppress("LongParameterList")
    fun queryComponents(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: String,
        @Parameter(description = "项目代码", required = false)
        @QueryParam("projectCode")
        projectCode: String? = null,
        @Parameter(description = "搜索关键字", required = false)
        @QueryParam("keyword")
        @BkField(patternStyle = BkStyleEnum.COMMON_STYLE, required = false)
        keyword: String?,
        @Parameter(description = "分类ID", required = false)
        @QueryParam("classifyId")
        @BkField(patternStyle = BkStyleEnum.ID_STYLE, required = false)
        classifyId: String?,
        @Parameter(description = "标签ID", required = false)
        @QueryParam("labelId")
        @BkField(patternStyle = BkStyleEnum.ID_STYLE, required = false)
        labelId: String?,
        @Parameter(description = "范畴ID", required = false)
        @QueryParam("categoryId")
        @BkField(patternStyle = BkStyleEnum.ID_STYLE, required = false)
        categoryId: String?,
        @Parameter(description = "评分", required = false)
        @QueryParam("score")
        @BkField(patternStyle = BkStyleEnum.NUMBER_STYLE, required = false)
        score: Int?,
        @Parameter(description = "研发来源类型", required = false)
        @QueryParam("rdType")
        rdType: RdTypeEnum?,
        @Parameter(description = "是否推荐标识 true：推荐，false：不推荐", required = false)
        @QueryParam("recommendFlag")
        @BkField(patternStyle = BkStyleEnum.BOOLEAN_STYLE, required = false)
        recommendFlag: Boolean?,
        @Parameter(description = "是否已在该项目安装 true：是，false：否", required = false)
        @QueryParam("installed")
        @BkField(patternStyle = BkStyleEnum.BOOLEAN_STYLE, required = false)
        installed: Boolean? = null,
        @Parameter(description = "是否需要更新标识 true：需要，false：不需要", required = false)
        @QueryParam("updateFlag")
        @BkField(patternStyle = BkStyleEnum.BOOLEAN_STYLE, required = false)
        updateFlag: Boolean?,
        @Parameter(description = "是否查询项目下组件标识", required = true)
        @QueryParam("queryProjectComponentFlag")
        queryProjectComponentFlag: Boolean = false,
        @Parameter(description = "排序", required = false)
        @QueryParam("sortType")
        sortType: StoreSortTypeEnum? = StoreSortTypeEnum.CREATE_TIME,
        @Parameter(description = "实例ID", required = false)
        @QueryParam("instanceId")
        instanceId: String?,
        @Parameter(description = "是否查测试中版本 true：是，false：否", required = false)
        @QueryParam("queryTestFlag")
        queryTestFlag: Boolean?,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        @BkField(patternStyle = BkStyleEnum.NUMBER_STYLE)
        page: Int = 1,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE)
        pageSize: Int = 10
    ): Result<Page<MarketItem>>

    @Operation(summary = "根据组件标识获取组件回显版本信息")
    @GET
    @Path("/types/{storeType}/codes/{storeCode}/component/showVersionInfo")
    fun getComponentShowVersionInfo(
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
    ): Result<StoreShowVersionInfo>

    @Operation(summary = "获取组件测试信息")
    @GET
    @Path("/types/{storeType}/codes/{storeCode}/component/test/info/get")
    fun getStoreTestInfo(
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
    ): Result<Set<StoreTestItem>>

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

    @Operation(summary = "获取组件升级版本信息")
    @Path("/types/{storeType}/codes/{storeCode}/component/upgrade/version/info/get")
    @GET
    fun getStoreUpgradeVersionInfo(
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
        @Parameter(description = "项目代码", required = true)
        @QueryParam("projectCode")
        @DefaultValue("")
        projectCode: String = "",
        @Parameter(description = "实例ID", required = false)
        @QueryParam("instanceId")
        instanceId: String? = null,
        @Parameter(description = "操作系统名称", required = false)
        @QueryParam("osName")
        osName: String? = null,
        @Parameter(description = "操作系统架构", required = false)
        @QueryParam("osArch")
        osArch: String? = null
    ): Result<VersionInfo?>
}
