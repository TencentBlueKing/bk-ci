package com.tencent.devops.store.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.Label
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_EXT_SERVICE_LABEL"], description = "流水线-插件标签")
@Path("/user/market/service/label")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExtServiceLableResource {

    @ApiOperation("获取所有插件标签信息")
    @GET
    @Path("/labels")
    fun getAllServiceLabels(): Result<List<Label>?>

    @ApiOperation("根据扩展服务ID获取服务标签信息")
    @GET
    @Path("/serviceIds/{serviceId}/labels")
    fun getServiceLabelsByServiceId(
        @ApiParam("扩展服务Id", required = true)
        @PathParam("serviceId")
        serviceId: String
    ): Result<List<Label>?>
}