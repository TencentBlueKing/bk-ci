package com.tencent.devops.image.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.image.pojo.PushImageTask
import com.tencent.devops.image.pojo.tke.TkePushImageParam
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_TKE"], description = "服务-TKE镜像相关接口")
@Path("/service/tke/image")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceTkePushImageResource {

    @ApiOperation("推送镜像到TKE镜像仓库")
    @POST
    @Path("/pushImage")
    fun pushImage(
        @ApiParam("推送镜像到TKE镜像仓库请求参数", required = true)
        pushParam: TkePushImageParam
    ): Result<PushImageTask?>

    @ApiOperation("推送镜像到TKE镜像仓库任务")
    @Path("/queryPushImageTask")
    @GET
    fun queryUploadTask(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("任务ID", required = true)
        @QueryParam("taskId")
        taskId: String
    ): Result<PushImageTask?>
}