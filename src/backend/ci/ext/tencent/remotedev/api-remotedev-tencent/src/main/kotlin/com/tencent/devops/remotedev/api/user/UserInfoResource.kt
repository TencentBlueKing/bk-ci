package com.tencent.devops.remotedev.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.TrustDeviceInfo
import com.tencent.devops.remotedev.pojo.TrustDeviceTokenGetData
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionData
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionResult
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoAuthCheck
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckData
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "USER_INFO", description = "用户-用户信息相关")
@Path("/{apiType:user|desktop}/info")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserInfoResource {

    @Operation(summary = "校验是否实名认证")
    @GET
    @Path("/cert/checkRealName")
    fun realNameCert(
        @QueryParam("name")
        name: String
    ): Result<Boolean>

    @Operation(summary = "校验是否需要管控")
    @POST
    @Path("/cert/check")
    fun multipleCert(
        @Parameter(description = "用户ID", required = false, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String?,
        @Parameter(description = "授信设备唯一标识", required = false)
        @HeaderParam(X_DEVOPS_TRUST_DEVICE_ID)
        deviceId: String?,
        @Parameter(description = "授信设备Token", required = false)
        @HeaderParam(X_DEVOPS_TRUST_DEVICE_TOKEN)
        token: String?,
        data: UserInfoCheckData
    ): Result<UserInfoCheckResult>

    @Operation(summary = "人脸识别")
    @POST
    @Path("/cert/faceRecognition")
    fun faceRecognition(
        data: FaceRecognitionData
    ): Result<FaceRecognitionResult>

    @Operation(summary = "发起异步检查用户权限中心权限过期")
    @POST
    @Path("/auth/check")
    fun asyncAuthCheck(
        data: UserInfoAuthCheck
    ): Result<Boolean>

    @Operation(summary = "获取设备授信Token")
    @POST
    @Path("/trust/device/token")
    fun getTrustDeviceToken(
        data: TrustDeviceTokenGetData
    ): Result<String>

    @Operation(summary = "校验设备授信Token")
    @GET
    @Path("/trust/device/token/verify")
    fun verifyTrustDeviceToken(
        @Parameter(description = "用户ID", required = false, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "授信设备唯一标识", required = false)
        @HeaderParam(X_DEVOPS_TRUST_DEVICE_ID)
        deviceId: String,
        @Parameter(description = "授信设备Token", required = false)
        @HeaderParam(X_DEVOPS_TRUST_DEVICE_TOKEN)
        token: String
    ): Result<Boolean>

    @Operation(summary = "获取授信设备列表")
    @GET
    @Path("/trust/device/list")
    fun getTrustDeviceList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<TrustDeviceInfo>>

    @Operation(summary = "删除授信设备")
    @DELETE
    @Path("/trust/device/delete")
    fun deleteTrustDevice(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "授信设备唯一标识")
        @QueryParam("deviceId")
        deviceId: String
    ): Result<Boolean>

    companion object {
        const val X_DEVOPS_TRUST_DEVICE_ID = "X-DEVOPS-TRUST-DEVICE-ID"
        const val X_DEVOPS_TRUST_DEVICE_TOKEN = "X-DEVOPS-TRUST-DEVICE-TOKEN"
    }
}
