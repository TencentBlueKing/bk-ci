package com.tencent.devops.remotedev.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.job.CronJob
import com.tencent.devops.remotedev.pojo.job.CronJobSearchParam
import com.tencent.devops.remotedev.pojo.job.JobCreateData
import com.tencent.devops.remotedev.pojo.job.JobDetail
import com.tencent.devops.remotedev.pojo.job.JobRecord
import com.tencent.devops.remotedev.pojo.job.JobRecordSearchParam
import com.tencent.devops.remotedev.pojo.job.JobSchema
import com.tencent.devops.remotedev.pojo.job.JobSchemaShort
import com.tencent.devops.remotedev.pojo.job.JobType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "USER_REMOTEDEV_JOB", description = "用户-自动化任务")
@Path("/user/remotedevjob")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserRemoteDevJobResource {
    @Operation(summary = "获取job schema 列表")
    @GET
    @Path("/schema/list")
    fun fetchJobSchemaList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "类型一次性，周期等", required = true)
        @QueryParam("type")
        type: JobType
    ): Result<List<JobSchemaShort>>

    @Operation(summary = "获取所有正在运行的机型")
    @GET
    @Path("/machineType/list")
    fun fetchMachineTypeList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String
    ): Result<Set<String>>

    @Operation(summary = "获取所有正在运行的拥有者")
    @GET
    @Path("/owners")
    fun fetchOwners(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String
    ): Result<Set<String>>

    @Operation(summary = "获取job schema")
    @GET
    @Path("/schema")
    fun getJobSchema(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "schemaId", required = true)
        @QueryParam("schemaId")
        schemaId: String
    ): Result<JobSchema?>

    @Operation(summary = "创建job")
    @POST
    @Path("/create")
    fun createJob(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "创建数据", required = true)
        data: JobCreateData
    ): Result<Boolean>

    @Operation(summary = "获取执行记录")
    @POST
    @Path("/record/list")
    fun fetchJobRecord(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "搜索参数", required = true)
        search: JobRecordSearchParam
    ): Result<Page<JobRecord>>

    @Operation(summary = "获取周期任务列表")
    @POST
    @Path("/cron/list")
    fun fetchCronList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "搜索参数", required = true)
        search: CronJobSearchParam
    ): Result<Page<CronJob>>

    @Operation(summary = "重新执行任务记录中的任务")
    @PUT
    @Path("/record/{id}/rerun")
    fun recordRerun(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "任务ID")
        @PathParam("id")
        id: Long
    ): Result<Boolean>

    @Operation(summary = "获取JOB执行详情")
    @GET
    @Path("/record/{id}/detail")
    fun fetchJobDetail(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "任务ID")
        @PathParam("id")
        id: Long
    ): Result<JobDetail?>
}
