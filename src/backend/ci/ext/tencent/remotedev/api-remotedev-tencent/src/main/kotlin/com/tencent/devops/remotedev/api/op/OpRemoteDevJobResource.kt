package com.tencent.devops.remotedev.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.job.JobSchema
import com.tencent.devops.remotedev.pojo.job.JobSchemaCreateData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OP_REMOTE_DEV_JOB", description = "OP-REMOTE-DEV-JOB")
@Path("/op/remotedevjob")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpRemoteDevJobResource {
    @Operation(summary = "创建或更新jobSchema")
    @POST
    @Path("/schema/createOrUpdate")
    fun createJobSchema(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        data: JobSchemaCreateData
    ): Result<Boolean>

    @Operation(summary = "获取schema列表")
    @GET
    @Path("/schema/list")
    fun getSchemaList(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
    ): Result<List<JobSchema>>

    @Operation(summary = "获取schema详情")
    @GET
    @Path("/schema")
    fun getSchema(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("schemaId")
        schemaId: String
    ): Result<JobSchema?>
}
