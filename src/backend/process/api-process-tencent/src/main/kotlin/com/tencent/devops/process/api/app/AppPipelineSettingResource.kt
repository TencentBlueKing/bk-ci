package com.tencent.devops.process.api.app

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.setting.PipelineSetting
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
import javax.ws.rs.core.MediaType

@Api(tags = ["APP_PIPELINE_SETTING"], description = "app流水线配置相关接口")
@Path("/app/pipelineSetting")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AppPipelineSettingResource {

    @ApiOperation("app获取流水线基础配置")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/setting")
    fun getPipelineSetting(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线id")
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<PipelineSetting>

    @ApiOperation("app保存流水线基础配置")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/save")
    fun saveSetting(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目Id")
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线Id")
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("setting内容")
        setting: PipelineSetting
    ): Result<String>
}