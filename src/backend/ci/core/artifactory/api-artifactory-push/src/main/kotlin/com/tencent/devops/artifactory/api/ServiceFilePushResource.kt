package com.tencent.devops.artifactory.api

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

@Api(tags = ["SERVICE_FILE_PUSH"], description = "仓库-文件分发")
@Path("/service/file/push")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceFilePushResource {

    @ApiOperation("分发文件")
    @POST
    @Path("/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun pushFile(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("pushInfo", required = true)
        pushInfo: PushFileDTO
    ): Result<String?>
}