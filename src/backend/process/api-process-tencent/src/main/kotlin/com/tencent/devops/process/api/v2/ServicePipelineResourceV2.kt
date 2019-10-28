package com.tencent.devops.process.api.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.pojo.Pipeline
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_PIPELINE_V2"], description = "服务-流水线资源-V2")
@Path("/service/v2/pipelines/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePipelineResourceV2 {

    @ApiOperation("根据构建资源获取流水线编排列表")
    @GET
    @Path("/buildResource")
    fun listPipelinesByBuildResource(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("构建类型", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_TYPE)
        buildResourceType: String,
        @ApiParam("构建资源值（镜像名称/节点ID/环境ID）", required = false)
        @QueryParam("resourceValue")
        buildResourceValue: String?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int? = 1,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int? = 20,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode? = ChannelCode.BS
    ): Result<Page<Pipeline>>

    @ApiOperation("根据projectId迁移数据")
    @PUT
    @Path("/dispatchTypeExtract")
    fun extractDispatchTypeByProjectId(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = false)
        @QueryParam("projectId")
        projectId: String
    ): Result<String>
}