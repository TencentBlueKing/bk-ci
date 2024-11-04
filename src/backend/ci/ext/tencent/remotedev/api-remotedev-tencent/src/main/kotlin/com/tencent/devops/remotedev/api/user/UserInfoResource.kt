package com.tencent.devops.remotedev.api.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionData
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionResult
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckData
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.GET
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
        data: UserInfoCheckData
    ): Result<UserInfoCheckResult>

    @Operation(summary = "人脸识别")
    @POST
    @Path("/cert/faceRecognition")
    fun faceRecognition(
        data: FaceRecognitionData
    ): Result<FaceRecognitionResult>
}
