package com.tencent.devops.lambda.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.lambda.pojo.BG
import com.tencent.devops.lambda.pojo.BuildResultWithPage
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_LAMBDA"], description = "服务-lambda资源")
@Path("/service/lambda")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceLambdaResource {

    @ApiOperation("获取构建历史")
    @GET
    @Path("/bgs/{bg}/builds")
    fun getBuildHistory(
        @ApiParam("项目ID", required = false)
        @QueryParam("projectId")
        projectId: String?,
        @ApiParam("流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam("开始时间， 单位秒", required = true)
        @QueryParam("beginTime")
        beginTime: Long,
        @ApiParam("结束时间，单位秒", required = false)
        @QueryParam("endTime")
        endTime: Long?,
        @ApiParam("事业群英文", required = false)
        @PathParam("bg")
        bg: BG,
        @ApiParam("部门", required = false)
        @QueryParam("deptName")
        deptName: String?,
        @ApiParam("中心", required = false)
        @QueryParam("centerName")
        centerName: String?,
        @ApiParam("分页开始标识", required = true)
        @QueryParam("offset")
        offset: Int,
        @ApiParam("大小， 最多为100条记录", required = true)
        @QueryParam("limit")
        limit: Int,
        @ApiParam("项目名", required = true)
        @QueryParam("project")
        project: String
    ): Result<BuildResultWithPage>
}