package com.tencent.devops.store.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.dto.CreateExtensionServiceDTO
import com.tencent.devops.store.pojo.dto.UpdateExtensionServiceDTO
import com.tencent.devops.store.pojo.vo.ExtensionServiceVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["EXTENSION_SERVICE"], description = "服务扩展")
@Path("/user/extension/services")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExtServiceResource {

    @POST
    @ApiOperation(value = "添加扩展服务")
    @Path("/{serviceCode}")
    fun createExtensionService(
        @ApiParam("扩展服务编码")
        @PathParam("serviceCode")
        serviceCode: String,
        @ApiParam("扩展服务信息")
        extensionInfo: CreateExtensionServiceDTO
    ): Result<String>

    @PUT
    @ApiOperation(value = "修改扩展服务")
    @Path("/{serviceId}")
    fun updateExtensionService(
        @ApiParam("扩展服务Id")
        @PathParam("serviceId")
        serviceId: String,
        @ApiParam("扩展服务信息")
        extensionInfo: UpdateExtensionServiceDTO
    ): Result<String>

    @GET
    @ApiOperation(value = "获取单条扩展服务信息")
    @Path("{serviceId}")
    fun getExtensionServiceInfo(
        @ApiParam("扩展服务Id")
        @PathParam("serviceId")
        serviceId: String
    ): Result<ExtensionServiceVO>

    @GET
    @ApiOperation(value = "获取扩展服务列表")
    @Path("{serviceId}/list")
    fun getExtensionServiceInfoList(
        @ApiParam("扩展服务Id")
        @PathParam("serviceId")
        serviceId: String?,
        @ApiParam("扩展分类", required = false)
        @QueryParam("category")
        category: String?,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<ExtensionServiceVO>
}