package com.tencent.devops.plugin.api.cos

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.cos.CdnUploadFileInfo
import com.tencent.devops.plugin.pojo.cos.SpmFile
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_PLUGIN_COS"], description = "COS系统")
@Path("/build/plugin/cos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePluginCosResource {

//    @ApiOperation("上传文件")
//    @POST
//    @Path("/projects/{projectId}/cos/{pipelineId}/{buildId}/uploadcos")
//    fun uploadCos(
//            @ApiParam("项目ID", required = true)
//            @PathParam("projectId")
//            projectId: String,
//            @ApiParam("流水线ID", required = true)
//            @PathParam("pipelineId")
//            pipelineId: String,
//            @ApiParam("构建ID", required = true)
//            @PathParam("buildId")
//            buildId: String,
//            @ApiParam("文件信息", required = true)
//            fileInfo: CosUploadFileInfo
//    ): MutableMap<String, String>

    @ApiOperation("上传文件到CDN")
    @POST
    @Path("/projects/{projectId}/cos/{pipelineId}/{buildId}/{elementId}/uploadcdn")
    fun uploadCdn(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("原子ID", required = true)
        @PathParam("elementId")
        elementId: String,
        @ApiParam("容器job ID", required = true)
        @PathParam("containerId")
        containerId: String,
        @ApiParam("执行次数", required = true)
        @QueryParam("executeCount")
        executeCount: Int,
        @ApiParam("文件信息", required = true)
        cdnUploadFileInfo: CdnUploadFileInfo
    ): Result<SpmFile>
}