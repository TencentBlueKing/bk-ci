package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.luna.LunaUploadParam
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_MIG_CDN"], description = "服务-LUNA相关接口")
@Path("/service/luna/upload")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceLunaResource {

    @ApiOperation("上传LUNA")
    @POST
    @Path("/pushFile")
    fun pushFile(
        @ApiParam("上传米LUNA请求参数", required = true)
        uploadParam: LunaUploadParam
    ): Result<String>
}