package com.tencent.devops.remotedev.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.job.JobSchema
import com.tencent.devops.remotedev.pojo.job.JobSchemaConstValResp
import com.tencent.devops.remotedev.pojo.job.OpJobSchemaCreateData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_REMOTE_DEV_JOB", description = "OP-REMOTE-DEV-JOB")
@Path("/op/remotedevjob")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpRemoteDevJobResource {
    @Operation(summary = "获取jobSchmema的一些配置项")
    @GET
    @Path("/schema/constval")
    fun getJobSchemaConstval(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<JobSchemaConstValResp>

    @Operation(summary = "创建或更新jobSchema")
    @POST
    @Path("/schema/createOrUpdate")
    fun createJobSchema(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        data: OpJobSchemaCreateData
    ): Result<Boolean>

    @Operation(summary = "获取schema列表")
    @GET
    @Path("/schema/list")
    fun getSchemaList(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
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

    @Operation(summary = "回调更新流水线任务状态")
    @POST
    @Path("/callback/pipeline/end")
    fun callBackUpdateJobStatus(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("jobId")
        jobId: Long
    ): Result<Boolean>

    @Operation(summary = "删除schema")
    @DELETE
    @Path("/schema")
    fun deleteSchema(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("schemaId")
        schemaId: String
    ): Result<Boolean>
}
