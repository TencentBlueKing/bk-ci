package com.tencent.devops.project.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.api.pojo.ExtItemDTO
import com.tencent.devops.project.api.pojo.ItemInfoResponse
import com.tencent.devops.project.api.pojo.ServiceItem
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

@Api(tags = ["OP_EXT_ITEM"], description = "扩展点")
@Path("/op/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OPItemResource {

    @GET
    @Path("/")
    @ApiOperation("获取扩展点完整列表")
    fun getItemList(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<ServiceItem>?>

    @GET
    @Path("/list")
    @ApiOperation("列表查询")
    fun list(
        @ApiParam("扩展名称")
        @QueryParam("itemName")
        itemName: String?,
        @ApiParam("蓝盾服务Id")
        @QueryParam("pid")
        pid: String?
    ): Result<List<ServiceItem>>

    @POST
    @Path("/")
    @ApiOperation("添加扩展点")
    fun create(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("扩展点信息", required = true)
        createInfo: ItemInfoResponse
    ): Result<Boolean>

    @PUT
    @Path("/{itemId}")
    @ApiOperation("修改扩展点")
    fun get(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("扩展点Id", required = true)
        @QueryParam("itemId")
        itemId: String,
        @ApiParam("扩展点信息", required = true)
        updateInfo: ItemInfoResponse
    ): Result<Boolean>

    @GET
    @Path("/{itemId}")
    @ApiOperation("获取扩展点")
    fun get(
        @ApiParam("扩展点Id", required = true)
        @QueryParam("itemId")
        itemId: String
    ): Result<ServiceItem?>

    @DELETE
    @Path("/{itemId}")
    @ApiOperation("删除扩展点")
    fun delete(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("扩展点Id", required = true)
        @QueryParam("itemId")
        itemId: String
    ): Result<Boolean>

    @PUT
    @Path("/{itemId}/forbidden")
    @ApiOperation("禁用扩展点")
    fun disable(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("扩展点Id", required = true)
        @QueryParam("itemId")
        itemId: String
    ): Result<Boolean>

    @PUT
    @Path("/{itemId}/enable")
    @ApiOperation("启用扩展点")
    fun enable(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("扩展点Id", required = true)
        @QueryParam("itemId")
        itemId: String
    ): Result<Boolean>
}