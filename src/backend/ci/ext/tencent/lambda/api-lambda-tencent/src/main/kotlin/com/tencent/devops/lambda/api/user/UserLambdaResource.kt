package com.tencent.devops.lambda.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.lambda.pojo.MakeUpBuildVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_LAMBDA"], description = "用户-大数据接口")
@Path("/user/lambda")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserLambdaResource {

    @ApiOperation("手动补录流水线流水线构建历史")
    @POST
    @Path("/makeup-build-his")
    fun manualMakeUpBuildHistory(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("补录列表", required = true)
        makeUpBuildVOs: List<MakeUpBuildVO>
    ): Result<Boolean>

    @ApiOperation("手动补录流水线流水线构建历史")
    @POST
    @Path("/makeup-build-task")
    fun manualMakeUpBuildTasks(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("补录列表", required = true)
        makeUpBuildVOs: List<MakeUpBuildVO>
    ): Result<Boolean>
}
