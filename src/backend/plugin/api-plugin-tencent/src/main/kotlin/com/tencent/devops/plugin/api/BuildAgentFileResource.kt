package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_AGENT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_AGENT_SECRET_KEY
import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * Created by Aaron Sheng on 2018/4/26.
 */
@Api(tags = ["BUILD_AGENT_FILE"], description = "服务-创建异步任务")
@Path("/buildAgent/file")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildAgentFileResource {

    @ApiOperation("上传待加固文件")
    @POST
    @Path("/security/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun securityUpload(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("原子ID", required = true)
        @HeaderParam("elementId")
        elementId: String,
        @ApiParam("Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_AGENT_ID)
        agentId: String,
        @ApiParam("秘钥", required = true)
        @HeaderParam(AUTH_HEADER_AGENT_SECRET_KEY)
        secretKey: String,
        @ApiParam("构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam(value = "文件流", required = true)
        @FormDataParam("file")
        fileStream: InputStream,
        @ApiParam(value = "环境id", required = true)
        @HeaderParam("envId")
        envId: String,
        @ApiParam(value = "文件名", required = true)
        @HeaderParam("fileName")
        fileName: String
    ): Result<String>
}