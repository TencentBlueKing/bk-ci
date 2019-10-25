package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.zhiyun.ZhiyunProduct
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_ZHIYUN_PRODUCT"], description = "OP-织云业务")
@Path("/op/zhiyun")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpZhiyunResource {

    @ApiOperation("新增织云业务")
    @POST
    @Path("/product/create")
    fun create(
        @ApiParam("织云业务", required = true)
        zhiyunProduct: ZhiyunProduct
    ): Result<Boolean>

    @ApiOperation("删除织云业务")
    @DELETE
    @Path("/product/{productId}/delete")
    fun delete(
        @ApiParam("productId", required = true)
        @PathParam("productId")
        productId: String
    ): Result<Boolean>

    @ApiOperation("查询织云业务")
    @GET
    @Path("/product/list")
    fun getList(): Result<List<ZhiyunProduct>>
}