package com.tencent.devops.store.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.Classify
import com.tencent.devops.store.pojo.common.ClassifyRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_STORE_CLASSIFY"], description = "OP-store-分类")
@Path("/op/store/classify")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpClassifyResource {

    @ApiOperation("添加分类")
    @POST
    @Path("/types/{classifyType}")
    fun add(
        @ApiParam("类别", required = true)
        @PathParam("classifyType")
        classifyType: StoreTypeEnum,
        @ApiParam(value = "分类信息请求报文体", required = true)
        classifyRequest: ClassifyRequest
    ): Result<Boolean>

    @ApiOperation("更新分类信息")
    @PUT
    @Path("/types/{classifyType}/ids/{id}")
    fun update(
        @ApiParam("类别", required = true)
        @PathParam("classifyType")
        classifyType: StoreTypeEnum,
        @ApiParam("分类ID", required = true)
        @PathParam("id")
        id: String,
        @ApiParam(value = "分类信息请求报文体", required = true)
        classifyRequest: ClassifyRequest
    ): Result<Boolean>

    @ApiOperation("获取所有分类信息")
    @GET
    @Path("/types/{classifyType}")
    fun listAllClassifys(
        @ApiParam("类别", required = true)
        @PathParam("classifyType")
        classifyType: StoreTypeEnum
    ): Result<List<Classify>>

    @ApiOperation("根据ID获取分类信息")
    @GET
    @Path("/{id}")
    fun getClassifyById(
        @ApiParam("分类ID", required = true)
        @QueryParam("id")
        id: String
    ): Result<Classify?>

    @ApiOperation("根据ID删除分类信息")
    @DELETE
    @Path("/{id}")
    fun deleteClassifyById(
        @ApiParam("分类ID", required = true)
        @PathParam("id")
        id: String
    ): Result<Boolean>
}