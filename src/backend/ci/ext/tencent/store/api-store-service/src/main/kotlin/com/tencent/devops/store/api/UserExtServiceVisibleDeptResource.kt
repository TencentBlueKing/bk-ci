package com.tencent.devops.store.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.ExtsionServiceVisibleDeptReq
import com.tencent.devops.store.pojo.common.StoreVisibleDeptResp
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_EXT_SERVICE_VISIBLE_DEPT"], description = "扩展服务-可见范围")
@Path("/user/extension/servcie/visible/dept")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExtServiceVisibleDeptResource {
    @ApiOperation("设置扩展服务可见范围")
    @POST
    @Path("/")
    fun addVisibleDept(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("插件市场-插件可见范围请求报文体", required = true)
        serviceVisibleDeptRequest: ExtsionServiceVisibleDeptReq
    ): Result<Boolean>

    @ApiOperation("查看扩展服务可见范围")
    @GET
    @Path("/{serviceCode}")
    fun getVisibleDept(
        @ApiParam("扩展服务Code", required = true)
        @PathParam("serviceCode")
        serviceCode: String
    ): Result<StoreVisibleDeptResp?>

    @ApiOperation("删除扩展服务可见范围")
    @DELETE
    @Path("/{serviceCode}")
    fun deleteVisibleDept(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("扩展服务Code", required = true)
        @PathParam("serviceCode")
        serviceCode: String,
        @ApiParam("机构Id集合，用\",\"分隔进行拼接（如1,2,3）", required = true)
        @QueryParam("deptIds")
        deptIds: String
    ): Result<Boolean>
}