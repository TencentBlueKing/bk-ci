// package com.tencent.devops.plugin.api
//
// import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
// import com.tencent.devops.common.api.auth.AUTH_HEADER_PIPELINE_ID
// import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
// import com.tencent.devops.common.api.auth.AUTH_HEADER_VM_SEQ_ID
// import com.tencent.devops.common.api.pojo.Result
// import io.swagger.annotations.Api
// import io.swagger.annotations.ApiOperation
// import io.swagger.annotations.ApiParam
// import javax.ws.rs.Consumes
// import javax.ws.rs.HeaderParam
// import javax.ws.rs.POST
// import javax.ws.rs.Path
// import javax.ws.rs.Produces
// import javax.ws.rs.core.MediaType
//
// @Api(tags = ["BUILD_PLUGIN_AGENT"], description = "构建-插件原子构建机")
// @Path("/build/pluginAgent/")
// @Produces(MediaType.APPLICATION_JSON)
// @Consumes(MediaType.APPLICATION_JSON)
// interface BuildPluginAgentResource {
//
//    @ApiOperation("执行开始")
//    @POST
//    @Path("/startup")
//    fun startup(
//        @ApiParam("项目ID", required = true)
//        @HeaderParam(AUTH_HEADER_PROJECT_ID)
//        projectId: String,
//        @ApiParam("构建ID", required = true)
//        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
//        pipelineId: String,
//        @ApiParam("构建ID", required = true)
//        @HeaderParam(AUTH_HEADER_BUILD_ID)
//        buildId: String,
//        @ApiParam("构建ID", required = true)
//        @HeaderParam(AUTH_HEADER_VM_SEQ_ID)
//        vmSeqId: String
//    ): Result<Boolean>
//
//    @ApiOperation("执行结束")
//    @POST
//    @Path("/end")
//    fun end(
//        @ApiParam("项目ID", required = true)
//        @HeaderParam(AUTH_HEADER_PROJECT_ID)
//        projectId: String,
//        @ApiParam("构建ID", required = true)
//        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
//        pipelineId: String,
//        @ApiParam("构建ID", required = true)
//        @HeaderParam(AUTH_HEADER_BUILD_ID)
//        buildId: String,
//        @ApiParam("构建ID", required = true)
//        @HeaderParam(AUTH_HEADER_VM_SEQ_ID)
//        vmSeqId: String
//    ): Result<Boolean>
// }