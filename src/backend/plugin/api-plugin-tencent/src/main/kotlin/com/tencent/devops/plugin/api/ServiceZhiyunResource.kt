package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.zhiyun.ZhiyunUploadParam
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_TCM"], description = "服务-织云相关接口")
@Path("/service/tcm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceZhiyunResource {

    @ApiOperation("上传织云任务")
    @POST
    @Path("/pushFile")
    fun pushFile(
        @ApiParam("织云请求参数", required = true)
        uploadParam: ZhiyunUploadParam
    ): Result<List<String>>
}