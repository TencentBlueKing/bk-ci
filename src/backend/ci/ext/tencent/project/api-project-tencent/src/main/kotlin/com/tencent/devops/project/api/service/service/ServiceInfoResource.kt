package com.tencent.devops.project.api.service.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.pojo.service.ServiceVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_SERVICE_INFO"], description = "服务-服务信息")
@Path("/service/infos/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceInfoResource {
    @GET
    @Path("/")
    @ApiOperation("获取所有服务信息")
    fun getServiceList(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<ServiceVO>?>
}