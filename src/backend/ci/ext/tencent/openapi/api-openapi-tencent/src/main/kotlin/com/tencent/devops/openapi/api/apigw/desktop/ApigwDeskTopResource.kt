package com.tencent.devops.openapi.api.apigw.desktop

import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_OS_ARCH
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_OS_NAME
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_SHA_CONTENT
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_STORE_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_STORE_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_STORE_VERSION
import com.tencent.devops.common.api.auth.DEVX_HEADER_GW_TOKEN
import com.tencent.devops.common.api.auth.DEVX_HEADER_NGGW_CLIENT_ADDRESS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.remotedev.pojo.DesktopTokenSignBody
import com.tencent.devops.remotedev.pojo.op.WorkspaceDesktopNotifyData
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OPEN_API_DESKTOP", description = "云桌面SDK API")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/desktop")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwDeskTopResource {

    @Operation(summary = "云桌面SDK获取应用token", tags = ["v4_app_desktop_sdk_token"])
    @POST
    @Path("/token")
    fun getToken(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "IP", required = true)
        @HeaderParam(DEVX_HEADER_NGGW_CLIENT_ADDRESS)
        @BkField(patternStyle = BkStyleEnum.IP_STYLE, required = true, message = "need ipv4")
        desktopIP: String,
        @Parameter(description = "devx token", required = true)
        @HeaderParam(DEVX_HEADER_GW_TOKEN)
        @BkField(minLength = 32, maxLength = 32, required = true, message = "need token")
        devxGwToken: String,
        @Parameter(description = "文件sha1", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_SHA_CONTENT)
        sha1: String,
        @Parameter(description = "操作系统类型", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_OS_NAME)
        osName: String,
        @Parameter(description = "架构", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_OS_ARCH)
        osArch: String,
        @Parameter(description = "应用id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_STORE_CODE)
        storeCode: String,
        @Parameter(description = "应用类型", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_STORE_TYPE)
        storeType: String,
        @Parameter(description = "应用版本", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_STORE_VERSION)
        storeVersion: String,
        sign: DesktopTokenSignBody
    ): Result<String>

    @Operation(
        summary = "云桌面SDK根据X-BK-NGGW-CLIENT-ADDRESS获取云桌面信息",
        tags = ["v4_app_desktop_workspace_detail"]
    )
    @GET
    @Path("/project/workspace/detail")
    fun getProjectWorkspace(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "IP", required = false)
        @HeaderParam(DEVX_HEADER_NGGW_CLIENT_ADDRESS)
        @BkField(patternStyle = BkStyleEnum.IP_STYLE, required = true, message = "need ipv4")
        desktopIP: String,
        @Parameter(description = "devx token", required = false)
        @HeaderParam(DEVX_HEADER_GW_TOKEN)
        @BkField(minLength = 32, maxLength = 32, required = true, message = "need token")
        devxGwToken: String
    ): Result<WeSecProjectWorkspace?>

    @Operation(summary = "给云桌面发送消息")
    @POST
    @Path("/sdk/notify")
    fun messageRegister(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "IP", required = false)
        @HeaderParam(DEVX_HEADER_NGGW_CLIENT_ADDRESS)
        @BkField(patternStyle = BkStyleEnum.IP_STYLE, required = true, message = "need ipv4")
        desktopIP: String,
        @Parameter(description = "devx token", required = false)
        @HeaderParam(DEVX_HEADER_GW_TOKEN)
        @BkField(minLength = 32, maxLength = 32, required = true, message = "need token")
        devxGwToken: String,
        data: WorkspaceDesktopNotifyData
    ): Result<Boolean>

    @Operation(summary = "云桌面SDK获取AccessToken", tags = ["v4_app_desktop_sdk_accesstoken"])
    @POST
    @Path("/sdk/accesstoken")
    fun sdkGetAccessToken(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "IP", required = true)
        @HeaderParam(DEVX_HEADER_NGGW_CLIENT_ADDRESS)
        @BkField(patternStyle = BkStyleEnum.IP_STYLE, required = true, message = "need ipv4")
        desktopIP: String,
        @Parameter(description = "devx token", required = true)
        @HeaderParam(DEVX_HEADER_GW_TOKEN)
        @BkField(minLength = 32, maxLength = 32, required = true, message = "need token")
        devxGwToken: String,
        @Parameter(description = "文件sha1", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_SHA_CONTENT)
        sha1: String,
        @Parameter(description = "操作系统类型", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_OS_NAME)
        osName: String,
        @Parameter(description = "架构", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_OS_ARCH)
        osArch: String,
        @Parameter(description = "应用id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_STORE_CODE)
        storeCode: String,
        @Parameter(description = "应用类型", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_STORE_TYPE)
        storeType: String,
        @Parameter(description = "应用版本", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_STORE_VERSION)
        storeVersion: String,
        @Parameter(description = "new", required = true)
        @QueryParam("new")
        new: Boolean?,
        sign: DesktopTokenSignBody
    ): Result<Oauth2AccessTokenVo>
}
