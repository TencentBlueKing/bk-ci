package com.tencent.devops.store.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.MediaInfoReq
import com.tencent.devops.store.pojo.common.StoreCommentInfo
import com.tencent.devops.store.pojo.common.StoreCommentRequest
import com.tencent.devops.store.pojo.common.StoreMediaInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_EXTENSION_MEDIA"], description = "服务扩展_媒体信息")
@Path("/user/market/extension/media")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExtMediaResource {
    @ApiOperation("添加媒体信息")
    @Path("/serviceCodes/{serviceCodes}/media")
    @POST
    fun createServiceMedia(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("扩展服务代码", required = true)
        @PathParam("serviceCodes")
        serviceCodes: String,
        @ApiParam("评论信息", required = true)
        mediaInfoList: List<MediaInfoReq>
    ): Result<Boolean>

    @ApiOperation("修改媒体信息")
    @Path("/ids/{mediaId}/")
    @PUT
    fun updateSericeMedia(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("媒体ID", required = true)
        @QueryParam("mediaId")
        mediaId: String,
        @ApiParam("评论信息", required = true)
        mediaInfoReq: MediaInfoReq
    ): Result<Boolean>

    @ApiOperation("获取单条媒体信息")
    @Path("/ids/{mediaId}")
    @GET
    fun getServiceMedia(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("媒体ID", required = true)
        @PathParam("mediaId")
        mediaId: String
    ): Result<StoreMediaInfo?>

    @ApiOperation("获取扩展服务所有媒体信息")
    @Path("/services/{serviceCode}")
    @GET
    fun getServiceMediaByServiceCode(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("扩展服务编码", required = true)
        @PathParam("serviceCode")
        serviceCode: String
    ): Result<List<StoreMediaInfo>?>
}