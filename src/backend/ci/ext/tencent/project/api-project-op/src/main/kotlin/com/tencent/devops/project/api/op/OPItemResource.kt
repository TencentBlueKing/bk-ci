package com.tencent.devops.project.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.project.api.pojo.ItemInfoResponse
import com.tencent.devops.project.api.pojo.ServiceItem
import com.tencent.devops.project.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_EXT_ITEM"], description = "扩展点")
@Path("/op/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OPItemResource {
    @GET
    @Path("/parentServices")
    @ApiOperation("查询根服务")
    fun parentList(
    ): Result<List<ServiceItem>>

    @POST
    @Path("/")
    @ApiOperation("添加扩展点")
    fun create(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("扩展点信息", required = true )
        createInfo: ItemInfoResponse
    ) : Result<String>

    @PUT
    @Path("/{itemId}")
    @ApiOperation("修改扩展点")
    fun update(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("扩展点Id", required = true)
        @QueryParam("itemId")
        itemId: String,
        @ApiParam("扩展点信息", required = true )
        createInfo: ItemInfoResponse
    ) : Result<String>

    @GET
    @Path("/{itemId}")
    @ApiOperation("获取扩展点")
    fun update(
        @ApiParam("扩展点Id", required = true)
        @QueryParam("itemId")
        itemId: String
    ) : Result<ServiceItem>
}