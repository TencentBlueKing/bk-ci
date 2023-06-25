package com.tencent.devops.auth.api.migrate

import com.tencent.devops.auth.pojo.dto.VerifyRecordDTO
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["AUTH_MIGRATE"], description = "权限-迁移结果处理")
@Path("/op/auth/migrate/result")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAuthMigrateResultResource {
    @POST
    @Path("/fixMigrateCompareResult/")
    @ApiOperation("修正迁移鉴权结果")
    fun fixMigrateCompareResult(
        @ApiParam("鉴权记录实体", required = true)
        verifyRecordDTO: VerifyRecordDTO
    ): Result<Boolean>
}
