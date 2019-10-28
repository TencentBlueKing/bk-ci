package com.tencent.devops.store.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.LabelRequest
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

@Api(tags = ["OP_STORE_LABEL"], description = "OP-store-标签")
@Path("/op/store/label")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpLabelResource {

    @ApiOperation("添加标签")
    @POST
    @Path("/types/{labelType}")
    fun add(
        @ApiParam("类别", required = true)
        @PathParam("labelType")
        labelType: StoreTypeEnum,
        @ApiParam(value = "标签信息请求报文体", required = true)
        labelRequest: LabelRequest
    ): Result<Boolean>

    @ApiOperation("更新标签信息")
    @PUT
    @Path("/types/{labelType}/ids/{id}")
    fun update(
        @ApiParam("类别", required = true)
        @PathParam("labelType")
        labelType: StoreTypeEnum,
        @ApiParam("标签ID", required = true)
        @PathParam("id")
        id: String,
        @ApiParam(value = "标签信息请求报文体", required = true)
        labelRequest: LabelRequest
    ): Result<Boolean>

    @ApiOperation("获取所有标签信息")
    @GET
    @Path("/types/{labelType}")
    fun listAllLabels(
        @ApiParam("类别", required = true)
        @PathParam("labelType")
        labelType: StoreTypeEnum
    ): Result<List<Label>?>

    @ApiOperation("根据ID获取标签信息")
    @GET
    @Path("/{id}")
    fun getLabelById(
        @ApiParam("标签ID", required = true)
        @QueryParam("id")
        id: String
    ): Result<Label?>

    @ApiOperation("根据ID删除标签信息")
    @DELETE
    @Path("/{id}")
    fun deleteLabelById(
        @ApiParam("标签ID", required = true)
        @PathParam("id")
        id: String
    ): Result<Boolean>
}