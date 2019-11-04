package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.security.UploadParams
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_FILE"], description = "服务-创建异步任务")
@Path("/service/file")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceFileResource {

    @ApiOperation("上传待加固文件")
    @POST
    @Path("/security/upload")
    fun securityUpload(
        uploadParams: UploadParams
    ): Result<String>

    @ApiOperation("获取最后加固结果")
    @GET
    @Path("/security/result")
    fun getSecurityResult(
        @ApiParam(value = "环境id", required = true)
        @QueryParam("envId")
        envId: String,
        @ApiParam("项目ID", required = true)
        @QueryParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("构建ID", required = true)
        @QueryParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam("原子ID", required = true)
        @QueryParam("elementId")
        elementId: String,
        @ApiParam(value = "执行用户", required = true)
        @QueryParam("taskId")
        taskId: String
    ): Result<String>
}