package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.codecc.CodeccCallback
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_CODECC"], description = "用户-codecc相关")
@Path("/user/codecc")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserCodeccResource {

    @ApiOperation("获取codecc报告")
    @GET
    @Path("/report/{buildId}")
    fun getCodeccReport(
        @ApiParam("构建ID", required = true)
        @PathParam(value = "buildId")
        buildId: String
    ): Result<CodeccCallback?>

    @ApiOperation("获取codecc报告")
    @GET
    @Path("/ruleSet/project/{projectId}/codeccRuleSet")
    fun getCodeccRuleSet(
        @ApiParam("项目ID", required = true)
        @PathParam(value = "projectId")
        projectId: String,
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("工具名称", required = true)
        @QueryParam(value = "toolName")
        toolName: String
    ): Result<Map<String, Any>>
}