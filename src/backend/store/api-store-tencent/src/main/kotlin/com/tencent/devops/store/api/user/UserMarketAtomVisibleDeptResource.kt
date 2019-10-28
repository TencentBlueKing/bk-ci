package com.tencent.devops.store.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.atom.AtomVisibleDeptReq
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

@Api(tags = ["USER_MARKET_ATOM_VISIBLE_DEPT"], description = "原子市场-原子-可见范围")
@Path("/user/market/desk/atom/visible/dept")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserMarketAtomVisibleDeptResource {

    @ApiOperation("设置原子可见范围")
    @POST
    @Path("/")
    fun addVisibleDept(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("原子市场-原子可见范围请求报文体", required = true)
        atomVisibleDeptRequest: AtomVisibleDeptReq
    ): Result<Boolean>

    @ApiOperation("查看原子可见范围")
    @GET
    @Path("/{atomCode}")
    fun getVisibleDept(
        @ApiParam("原子代码", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<StoreVisibleDeptResp?>

    @ApiOperation("删除原子可见范围")
    @DELETE
    @Path("/{atomCode}")
    fun deleteVisibleDept(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("原子代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("机构Id集合，用\",\"分隔进行拼接（如1,2,3）", required = true)
        @QueryParam("deptIds")
        deptIds: String
    ): Result<Boolean>
}