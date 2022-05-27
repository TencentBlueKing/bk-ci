package com.tencent.devops.dispatch.kubernetes.api.builds

import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesJobReq
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesJobResp
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesJobStatusResp
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_DISPATCH_KUBERNETES"], description = "构建-dispatch-kubernetes资源操作")
@Path("/build/sdk/job")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface SdkJobResource {

    @ApiOperation("启动job")
    @POST
    @Path("")
    fun createJob(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目ID")
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam(value = "流水线ID")
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam(value = "构建ID")
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam("Job结构", required = true)
        jobReq: KubernetesJobReq
    ): Result<KubernetesJobResp>

    @ApiOperation("获取Job状态")
    @GET
    @Path("/{jobName}/status")
    fun getJobStatus(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目ID")
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam(value = "流水线ID")
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam(value = "构建ID")
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @PathParam("jobName")
        jobName: String
    ): Result<KubernetesJobStatusResp>

    @ApiOperation("获取Job日志")
    @GET
    @Path("/{jobName}/logs")
    fun getJobLogs(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目ID")
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam(value = "流水线ID")
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam(value = "构建ID")
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @PathParam("jobName")
        jobName: String,
        @QueryParam("sinceTime")
        sinceTime: Int?
    ): Result<String?>
}
