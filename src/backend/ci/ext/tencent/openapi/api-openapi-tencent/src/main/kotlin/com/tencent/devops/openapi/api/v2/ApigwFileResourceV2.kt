package com.tencent.devops.openapi.api.v2

import com.tencent.devops.artifactory.pojo.dto.PushFileDTO
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType


@Api(tags = ["OPEN_API_V2_FILE"], description = "OPEN-API-V2-文件资源")
@Path("/{apigw:apigw-user|apigw-app|apigw}/v2/file")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwFileResourceV2 {

    @ApiOperation("分发文件")
    @POST
    @Path("/pusb/byJob")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun pushFile(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("pushInfo", required = true)
        pushInfo: PushFileDTO
    ): Result<Boolean?>
}