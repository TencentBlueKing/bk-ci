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
import com.tencent.devops.common.auth.api.AuthPermission
import javax.ws.rs.QueryParam

@Api(tags = ["AUTH_VERIFY_RECORD"], description = "鉴权记录")
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
        @ApiParam("鉴权记录实体")
        verifyRecordDTO: VerifyRecordDTO
    ): Result<Boolean>

    @POST
    @Path("/batch")
    @ApiOperation("批量记录鉴权结果")
    fun bathCreateOrUpdate(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("用户ID")
        userId: String,
        @ApiParam("项目ID")
        @QueryParam("projectCode")
        projectCode: String,
        @ApiParam("资源类型")
        @QueryParam("resourceType")
        resourceType: String,
        @ApiParam("资源权限结果")
        permissionsResourcesMap: Map<AuthPermission, List<String>>
    ): Result<Boolean>
}
