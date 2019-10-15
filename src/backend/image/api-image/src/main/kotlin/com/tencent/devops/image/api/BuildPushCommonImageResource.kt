package com.tencent.devops.image.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.image.pojo.PushImageParam
import com.tencent.devops.image.pojo.PushImageTask
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

@Api(tags = ["BUILD_IMAGE"], description = "构建-推送镜像相关接口")
@Path("/build/image/common")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildPushCommonImageResource {

    @ApiOperation("推送镜像到指定镜像仓库")
    @POST
    @Path("/push")
    fun pushImage(
        @ApiParam("推送镜像到指定仓库请求参数", required = true)
        pushParam: PushImageParam
    ): Result<PushImageTask?>

    @ApiOperation("查询推送镜像到指定镜像仓库任务详情")
    @Path("/query")
    @GET
    fun queryImageTask(
        @ApiParam("用户ID", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("任务ID", required = true)
        @QueryParam("taskId")
        taskId: String
    ): Result<PushImageTask?>
}