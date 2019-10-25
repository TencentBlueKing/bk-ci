package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.GcloudConf
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_GCLOUD_CONF"], description = "用户-GCLOUD配置")
@Path("/service/gcloud")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGcloudConfResource {

    @ApiOperation("根据configId查询GCLOUD配置")
    @GET
    @Path("/conf/list/{configId}")
    fun getByConfigId(
        @ApiParam(value = "conf的主键id", required = true)
        @PathParam("configId")
        configId: Int
    ): Result<GcloudConf?>
}