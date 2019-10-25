package com.tencent.devops.store.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.Category
import com.tencent.devops.store.pojo.common.CategoryRequest
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

@Api(tags = ["OP_STORE_CATEGORY"], description = "OP-store-范畴")
@Path("/op/store/category")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpCategoryResource {

    @ApiOperation("添加范畴")
    @POST
    @Path("/types/{categoryType}")
    fun add(
        @ApiParam("类别", required = true)
        @PathParam("categoryType")
        categoryType: StoreTypeEnum,
        @ApiParam(value = "范畴信息请求报文体", required = true)
        categoryRequest: CategoryRequest
    ): Result<Boolean>

    @ApiOperation("更新范畴信息")
    @PUT
    @Path("/types/{categoryType}/ids/{id}")
    fun update(
        @ApiParam("类别", required = true)
        @PathParam("categoryType")
        categoryType: StoreTypeEnum,
        @ApiParam("范畴ID", required = true)
        @PathParam("id")
        id: String,
        @ApiParam(value = "范畴信息请求报文体", required = true)
        categoryRequest: CategoryRequest
    ): Result<Boolean>

    @ApiOperation("获取所有范畴信息")
    @GET
    @Path("/types/{categoryType}")
    fun listAllCategorys(
        @ApiParam("类别", required = true)
        @PathParam("categoryType")
        categoryType: StoreTypeEnum
    ): Result<List<Category>?>

    @ApiOperation("根据ID获取范畴信息")
    @GET
    @Path("/{id}")
    fun getCategoryById(
        @ApiParam("范畴ID", required = true)
        @QueryParam("id")
        id: String
    ): Result<Category?>

    @ApiOperation("根据ID删除范畴信息")
    @DELETE
    @Path("/{id}")
    fun deleteCategoryById(
        @ApiParam("范畴ID", required = true)
        @PathParam("id")
        id: String
    ): Result<Boolean>
}