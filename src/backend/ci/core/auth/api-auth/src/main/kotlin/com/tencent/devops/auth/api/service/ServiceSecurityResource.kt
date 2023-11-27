package com.tencent.devops.auth.api.service

import com.tencent.devops.auth.pojo.vo.UserAndDeptInfoVo
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_SECURITY"], description = "安全相关")
@Path("/service/security")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceSecurityResource {
    @GET
    @Path("/getUserSecurityInfo")
    @ApiOperation("获取安全相关信息")
    fun getUserSecurityInfo(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("用户ID", required = true)
        userId: String,
        @ApiParam("项目ID", required = true)
        @QueryParam("projectCode")
        projectCode: String
    ): Result<UserAndDeptInfoVo>
}
