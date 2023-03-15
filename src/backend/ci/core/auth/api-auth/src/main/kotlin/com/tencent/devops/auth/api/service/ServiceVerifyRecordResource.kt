package com.tencent.devops.auth.api.service

import com.tencent.devops.auth.pojo.dto.VerifyRecordDTO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import com.tencent.devops.common.api.pojo.Result

@Api(tags = ["AUTH_VERIFY_RECORD"], description = "校验记录")
@Path("/service/verify/record")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceVerifyRecordResource {

    @POST
    @Path("/")
    @ApiOperation("记录鉴权结果")
    fun createOrUpdate(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("用户ID")
        userId: String,
        verifyRecordDTO: VerifyRecordDTO
    ): Result<Boolean>
}
