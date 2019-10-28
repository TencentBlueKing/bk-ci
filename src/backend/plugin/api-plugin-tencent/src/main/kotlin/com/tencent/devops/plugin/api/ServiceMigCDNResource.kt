package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.migcdn.MigCDNUploadParam
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_MIG_CDN"], description = "服务-米格云控相关接口")
@Path("/service/mig/cdn")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceMigCDNResource {

    @ApiOperation("上传米格云控CDN")
    @POST
    @Path("/pushFile")
    fun pushFile(
        @ApiParam("上传米格云控CDN请求参数", required = true)
        uploadParam: MigCDNUploadParam
    ): Result<String>
}