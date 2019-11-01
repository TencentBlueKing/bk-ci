package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.pojo.AtomMarketInitPipelineReq
import com.tencent.devops.common.pipeline.pojo.CheckImageInitPipelineReq
import com.tencent.devops.process.pojo.AtomMarketInitPipelineResp
import com.tencent.devops.process.pojo.CheckImageInitPipelineResp
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_PIPELINE"], description = "服务-流水线资源")
@Path("/service/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePipelineInitResource {

    @ApiOperation("插件打包初始化流水线")
    @POST
    @Path("/market/pipeline/init/{projectCode}")
    fun initAtomMarketPipeline(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("插件市场初始化流水线请求报文体", required = true)
        atomMarketInitPipelineReq: AtomMarketInitPipelineReq
    ): Result<AtomMarketInitPipelineResp>

    @ApiOperation("验证镜像合法性初始化流水线")
    @POST
    @Path("/market/pipeline/projectCodes/{projectCode}/image/check/init")
    fun initCheckImagePipeline(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("检查镜像合法性初始化流水线请求报文体", required = true)
        checkImageInitPipelineReq: CheckImageInitPipelineReq
    ): Result<CheckImageInitPipelineResp>
}