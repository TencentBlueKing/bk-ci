package com.tencent.devops.remotedev.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.InstallStoreReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.VersionInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_REMOTEDEV_STORE", description = "用户-RemoteDevStore")
@Path("/user/remotedev/store")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserRemoteDevStoreResource {

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

    @Operation(summary = "安装组件到项目")
    @POST
    @Path("/component/install")
    fun installComponent(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "安装组件到项目请求报文体", required = true)
        installStoreReq: InstallStoreReq
    ): Result<Boolean>

    @Operation(summary = "获取组件包文件下载链接")
    @GET
    @Path("/types/{storeType}/codes/{storeCode}/versions/{version}/pkg/download/url/get")
    @Suppress("LongParameterList")
    fun getComponentPkgDownloadUrl(
        @Parameter(description = "用户Id", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        @DefaultValue("")
        projectId: String,
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
        @Parameter(description = "实例ID", required = false)
        @QueryParam("instanceId")
        instanceId: String? = null,
        @Parameter(description = "操作系统名称", required = false)
        @QueryParam("osName")
        osName: String? = null,
        @Parameter(description = "操作系统架构", required = false)
        @QueryParam("osArch")
        osArch: String? = null
    ): Result<String>
}
