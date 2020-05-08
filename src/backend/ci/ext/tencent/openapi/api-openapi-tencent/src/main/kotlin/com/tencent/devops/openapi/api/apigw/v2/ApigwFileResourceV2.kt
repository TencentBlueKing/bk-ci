package com.tencent.devops.openapi.api.apigw.v2

import com.tencent.devops.artifactory.pojo.dto.PushFileDTO
import com.tencent.devops.artifactory.pojo.vo.PushResultVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType


@Api(tags = ["OPEN_API_V2_FILE"], description = "OPEN-API-V2-文件资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v2/file")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwFileResourceV2 {

    @ApiOperation("分发文件")
    @POST
    @Path("/push/byJob")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun pushFile(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("pushInfo", required = true)
        pushInfo: PushFileDTO
    ): Result<Long?>

    @ApiOperation("检验分发结果")
    @GET
    @Path("/check/status")
    fun checkPushStatus(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("jobInstanceId", required = true)
        @QueryParam("jobInstanceId")
        jobInstanceId: Long
    ): Result<PushResultVO?>
}