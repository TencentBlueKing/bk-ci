package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.wetest.WetestEmailGroup
import com.tencent.devops.plugin.pojo.wetest.WetestEmailGroupParam
import com.tencent.devops.plugin.pojo.wetest.WetestEmailGroupResponse
import com.tencent.devops.plugin.pojo.wetest.WetestReportResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_WETEST_EAMIL_GROUP"], description = "用户-WETEST邮件组")
@Path("/user/wetest/emailgroup")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserWetestEmailGroupResource {

    @ApiOperation("新增WETEST邮件组")
    @POST
    @Path("/{projectId}/create")
    fun create(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("邮件组", required = true)
        weTestEmailGroup: WetestEmailGroupParam
    ): Result<Map<String, Int>>

    @ApiOperation("更新WETEST邮件组")
    @POST
    @Path("/{projectId}/update")
    fun update(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("id", required = true)
        @QueryParam("id")
        id: Int,
        @ApiParam("邮件组", required = true)
        weTestEmailGroup: WetestEmailGroupParam
    ): Result<Boolean>

    @ApiOperation("获取单个WETEST邮件组")
    @POST
    @Path("/{projectId}/get")
    fun get(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("ID", required = true)
        @QueryParam("id")
        id: Int
    ): Result<WetestEmailGroup?>

    @ApiOperation("删除WETEST邮件组")
    @POST
    @Path("/{projectId}/delete")
    fun delete(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("id", required = true)
        @QueryParam("id")
        id: Int
    ): Result<Boolean>

    @ApiOperation("查询WETEST邮件组列表")
    @GET
    @Path("/{projectId}/list")
    fun getList(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "开始页数，从1开始", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int,
        @ApiParam(value = "每页数据条数", required = false, defaultValue = "12")
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<WetestEmailGroupResponse?>

    @ApiOperation("根据用户查询WETEST报告共享")
    @GET
    @Path("/{projectId}/wetestReport")
    fun getWetestReport(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<WetestReportResponse?>
}