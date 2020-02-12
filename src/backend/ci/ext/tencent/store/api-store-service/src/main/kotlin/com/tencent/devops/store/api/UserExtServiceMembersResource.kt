package com.tencent.devops.store.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.StoreMemberItem
import com.tencent.devops.store.pojo.common.StoreMemberReq
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_EXT_SERVICE_MEMBER"], description = "服务扩展-用户")
@Path("/user/market/service/members")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExtServiceMembersResource {

    @ApiOperation("获取服务扩展成员列表")
    @GET
    @Path("/list")
    fun list(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("serviceCode", required = true)
        @QueryParam("serviceCode")
        serviceCode: String
    ): Result<List<StoreMemberItem?>>

    @ApiOperation("添加服务扩展成员")
    @POST
    @Path("/add")
    fun add(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("添加成员请求报文")
        storeMemberReq: StoreMemberReq
    ): Result<Boolean>

    @ApiOperation("删除服务扩展成员")
    @DELETE
    @Path("/delete")
    fun delete(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("成员ID", required = true)
        @QueryParam("id")
        id: String,
        @ApiParam("serviceCode", required = true)
        @QueryParam("serviceCode")
        serviceCode: String
    ): Result<Boolean>

    @ApiOperation("查看服务扩展成员信息")
    @GET
    @Path("/view")
    fun view(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("扩展服务编码", required = true)
        @QueryParam("serviceCode")
        serviceCode: String
    ): Result<StoreMemberItem?>

    @ApiOperation("修改服务扩展成员的调试项目")
    @PUT
    @Path("/test/project/change")
    fun changeMemberTestProjectCode(
        @ApiParam("token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目代码", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @ApiParam("扩展服务编码", required = true)
        @QueryParam("serviceCode")
        serviceCode: String
    ): Result<Boolean>
}