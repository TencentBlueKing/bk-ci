package com.tencent.devops.remotedev.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.job.JobSchema
import com.tencent.devops.remotedev.pojo.job.JobSchemaConstValResp
import com.tencent.devops.remotedev.pojo.job.OpJobSchemaCreateData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
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
