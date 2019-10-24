package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.codecc.CodeccCallback
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["EXTERNAL_CODECC"], description = "服务-创建异步任务")
@Path("/external/codecc")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalCodeccResource {

    @ApiOperation("提供根据流水线构建id查询构建号（页面上展示用的）、构建时间、构建人等信息件")
    @POST
    @Path("/callback")
    fun callback(
        callback: CodeccCallback
    ): Result<String>
}