package com.tencent.devops.remotedev.api.service

import com.tencent.devops.auth.pojo.dto.ClientDetailsDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.CdsToken
import com.tencent.devops.remotedev.pojo.DesktopTokenSign
import com.tencent.devops.remotedev.pojo.sdk.SdkReportData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_REMOTEDEV", description = "remotedev service接口")
@Path("/service/remotedev_sdk")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceSDKResource {
    @Operation(summary = "云桌面SDK获取应用token")
    @POST
    @Path("/token")
    fun getToken(
        @Parameter(description = "IP", required = false)
        @QueryParam("desktopIP")
        desktopIP: String,
        sign: DesktopTokenSign
    ): Result<String>

    @Operation(summary = "云桌面SDK获取AccessToken")
    @POST
    @Path("/accesstoken")
    fun sdkGetAccessToken(
        @Parameter(description = "IP", required = false)
        @QueryParam("desktopIP")
        desktopIP: String,
        @Parameter(description = "new", required = false)
        @QueryParam("new")
        new: Boolean?,
        sign: DesktopTokenSign
    ): Result<Oauth2AccessTokenVo>

    @Operation(summary = "提供给openapi服务拿取appId对应的oauth原材料")
    @GET
    @Path("/appid_oauth_client_detail")
    fun getAppIdOauthClientDetail(
        @Parameter(description = "desktopIP", required = false)
        @QueryParam("desktopIP")
        desktopIP: String,
        @Parameter(description = "appId", required = false)
        @QueryParam("appId")
        appId: String
    ): Result<ClientDetailsDTO?>

    @Operation(summary = "提供给openapi服务拿取cdsToken解析内容")
    @GET
    @Path("/check_cds_token")
    fun checkCdsToken(
        @Parameter(description = "cdsToken", required = false)
        @QueryParam("cdsToken")
        cdsToken: String
    ): Result<CdsToken?>

    @Operation(summary = "云桌面SDK上报数据")
    @POST
    @Path("/reportdata")
    fun sdkReportData(
        data: SdkReportData
    ): Result<Boolean>

    @Operation(summary = "校验cdi接口token是否有授权，返回当前实例Id和应用Id。若未授权返回null。")
    @GET
    @Path("/check_cdi_oauth")
    fun checkCDIOauth(
        @Parameter(description = "cdsToken", required = false)
        @QueryParam("cdiToken")
        cdiToken: String
    ): Result<Pair<String/*当前实例id*/, String/*appId*/>?>

    @Operation(summary = "根据实例Id，返回实例当前登陆人。若没人登陆返回null。")
    @GET
    @Path("/login_user_id")
    fun getLoginUserId(
        @Parameter(description = "workspaceName", required = false)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<String/*当前登陆人id*/?>
}
