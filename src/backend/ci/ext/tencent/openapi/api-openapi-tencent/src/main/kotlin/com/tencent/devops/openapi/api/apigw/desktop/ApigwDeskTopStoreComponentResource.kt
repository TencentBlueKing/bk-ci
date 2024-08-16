package com.tencent.devops.openapi.api.apigw.desktop

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.openapi.api.apigw.pojo.StoreDailyStatisticInfo
import com.tencent.devops.store.pojo.common.InstallStoreReq
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.MarketMainItem
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.enums.RdTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OPEN_API_DESKTOP_STORE_COMPONENT", description = "云桌面研发商店组件 API")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/desktop/store/components")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("LongParameterList")
interface ApigwDeskTopStoreComponentResource {

    @Operation(summary = "获取研发商店首页组件的数据", tags = ["v4_app_desktop_store_component_main_page"])
    @Path("/types/{storeType}/component/main/page/list")
    @GET
    fun getMainPageComponents(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
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

    @Operation(summary = "根据条件查询组件列表", tags = ["v4_app_desktop_store_component_list"])
    @Path("/types/{storeType}/component/list")
    @GET
    @Suppress("LongParameterList")
    fun queryComponents(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
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
        @Parameter(description = "范畴ID", required = false)
        @QueryParam("categoryId")
        @BkField(patternStyle = BkStyleEnum.ID_STYLE, required = false)
        categoryId: String?,
        @Parameter(description = "标签ID", required = false)
        @QueryParam("labelId")
        @BkField(patternStyle = BkStyleEnum.ID_STYLE, required = false)
        labelId: String?,
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
        instanceId: String? = null,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        @BkField(patternStyle = BkStyleEnum.NUMBER_STYLE)
        page: Int = 1,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE)
        pageSize: Int = 10
    ): Result<Page<MarketItem>>

    @Operation(summary = "根据组件ID获取组件详情", tags = ["v4_app_desktop_store_component_detail"])
    @GET
    @Path("/types/{storeType}/ids/{storeId}/component/detail")
    fun getComponentDetailInfoById(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
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

    @Operation(summary = "安装组件到项目", tags = ["v4_app_desktop_store_component_install"])
    @POST
    @Path("/component/install")
    fun installComponent(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "安装组件到项目请求报文体", required = true)
        installStoreReq: InstallStoreReq
    ): Result<Boolean>

    @Operation(summary = "卸载组件", tags = ["v4_app_desktop_store_component_uninstall"])
    @Path("/projects/{projectCode}/types/{storeType}/codes/{storeCode}/component/uninstall")
    @DELETE
    fun uninstallComponent(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: String,
        @Parameter(description = "组件代码", required = true)
        @PathParam("storeCode")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeCode: String,
        @Parameter(description = "卸载组件请求包体", required = true)
        unInstallReq: UnInstallReq
    ): Result<Boolean>

    @Operation(summary = "获取组件包文件下载链接", tags = ["v4_app_store_component_downloadUrl_get"])
    @GET
    @Path("/types/{storeType}/codes/{storeCode}/versions/{version}/pkg/download/url/get")
    fun getComponentPkgDownloadUrl(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户Id", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目代码", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectCode: String,
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

    @Operation(summary = "更新store组件的每日统计信息")
    @PUT
    @Path("/types/{storeType}/codes/{storeCode}/daily/info/update")
    fun updateDailyStatisticInfo(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: StoreTypeEnum,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "store组件的每日统计信息", required = true)
        storeDailyStatisticInfo: StoreDailyStatisticInfo
    ): Result<Boolean>
}
