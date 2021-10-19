package com.tencent.devops.quality.api.v3

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_RULE_V3"], description = "质量红线v3")
@Path("/user/rules/v3")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserQualityRuleResource {

    @ApiOperation("更新红线状态")
    @Path("/update/{ruleHashId}")
    @PUT
    fun update(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("规则ID", required = true)
        @PathParam("ruleHashId")
        ruleHashId: String,
        @ApiParam("审核操作", required = true)
        @QueryParam("pass")
        pass: Boolean
    ): Result<Boolean>
}
