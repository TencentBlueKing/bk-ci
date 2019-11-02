package com.tencent.devops.store.api.image.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.StoreVisibleDeptResp
import com.tencent.devops.store.pojo.image.request.ImageVisibleDeptReq
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

@Api(tags = ["USER_MARKET_IMAGE_VISIBLE_DEPT"], description = "镜像市场-镜像-可见范围")
@Path("/user/market/desk/image/visible/dept")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserMarketImageVisibleDeptResource {

    @ApiOperation("设置镜像可见范围")
    @POST
    @Path("/")
    fun addVisibleDept(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像市场-镜像可见范围请求报文体", required = true)
        imageVisibleDeptRequest: ImageVisibleDeptReq
    ): Result<Boolean>

    @ApiOperation("查看镜像可见范围")
    @GET
    @Path("/{imageCode}")
    fun getVisibleDept(
        @ApiParam("镜像代码", required = true)
        @PathParam("imageCode")
        imageCode: String
    ): Result<StoreVisibleDeptResp?>

    @ApiOperation("删除镜像可见范围")
    @DELETE
    @Path("/{imageCode}")
    fun deleteVisibleDept(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像代码", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @ApiParam("机构Id集合，用\",\"分隔进行拼接（如1,2,3）", required = true)
        @QueryParam("deptIds")
        deptIds: String
    ): Result<Boolean>
}