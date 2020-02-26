package com.tencent.devops.project.api.service.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.api.pojo.ExtItemDTO
import com.tencent.devops.project.api.pojo.ServiceItem
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_EXT_ITEM"], description = "服务扩展-项目")
@Path("/service/ext/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceItemResource {
    @GET
    @Path("/{itemId}")
    @ApiOperation("获取扩展项目列表")
    fun getItemList(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("项目Id", required = true)
        @QueryParam("itemId")
        itemId: String
    ): Result<ExtItemDTO?>

    @GET
    @Path("itemCodes/{itemCode}")
    @ApiOperation("获取扩展项目列表")
    fun getItemByCode(
        @ApiParam("项目Code", required = true)
        @QueryParam("itemCode")
        itemCode: String
    ): Result<ServiceItem?>

    @GET
    @Path("itemIds/{itemIds}/itemInfo")
    @ApiOperation("获取扩展项目列表")
    fun getItemInfoByIds(
        @ApiParam("项目id串", required = true)
        @QueryParam("itemIds")
        itemIds: List<String>
    ): Result<List<ServiceItem>?>

    @GET
    @Path("/{itemIds}/byIds")
    @ApiOperation("获取扩展项目列表")
    fun getItemListsByIds(
        @ApiParam("项目Id", required = true)
        @QueryParam("itemIds")
        itemIds: List<String>
    ): Result<List<ExtItemDTO>?>

    @PUT
    @Path("/add/serviceNum")
    @ApiOperation("批量添加扩展点使用数量")
    fun addServiceNum(
        @ApiParam("项目Id", required = true)
        @QueryParam("itemIds")
        itemIds: List<String>
    ): Result<Boolean>
}