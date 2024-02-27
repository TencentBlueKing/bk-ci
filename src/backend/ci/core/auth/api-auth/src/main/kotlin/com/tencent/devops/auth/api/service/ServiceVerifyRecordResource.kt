package com.tencent.devops.auth.api.service

import com.tencent.devops.auth.pojo.dto.VerifyRecordDTO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "AUTH_VERIFY_RECORD", description = "鉴权记录")
@Path("/service/verify/record")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceVerifyRecordResource {

    @POST
    @Path("/")
    @Operation(summary = "记录鉴权结果")
    fun createOrUpdate(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @Parameter(description = "用户ID")
        userId: String,
        @Parameter(description = "鉴权记录实体")
        verifyRecordDTO: VerifyRecordDTO
    ): Result<Boolean>

    @DELETE
    @Path("/")
    @Operation(summary = "删除鉴权结果")
    fun delete(
        @Parameter(description = "项目ID")
        @QueryParam("projectCode")
        projectCode: String,
        @Parameter(description = "资源类型")
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "资源Code")
        @QueryParam("resourceCode")
        resourceCode: String
    ): Result<Boolean>
}
