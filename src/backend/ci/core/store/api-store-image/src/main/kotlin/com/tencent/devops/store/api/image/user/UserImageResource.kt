package com.tencent.devops.store.api.image.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.store.pojo.common.VersionInfo
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.request.ImageBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.image.response.ImageDetail
import com.tencent.devops.store.pojo.image.response.MarketImageMain
import com.tencent.devops.store.pojo.image.response.MarketImageResp
import com.tencent.devops.store.pojo.image.response.MyImage
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_MARKET_IMAGE"], description = "研发商店-镜像")
@Path("/user/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserImageResource {
    @ApiOperation("镜像市场首页")
    @GET
    @Path("/image/list/main")
    fun mainPageList(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页大小", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<MarketImageMain>>

    @ApiOperation("镜像市场搜索镜像")
    @GET
    @Path("/image/list")
    fun searchImage(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像名称", required = false)
        @QueryParam("imageName")
        imageName: String?,
        @ApiParam("镜像来源", required = false)
        @QueryParam("imageSourceType")
        imageSourceType: ImageType?,
        @ApiParam("镜像分类编码", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @ApiParam("应用范畴", required = false)
        @QueryParam("categoryCode")
        categoryCode: String?,
        @ApiParam("镜像研发来源", required = false)
        @QueryParam("rdType")
        rdType: ImageRDTypeEnum?,
        @ApiParam("镜像标签代码", required = false)
        @QueryParam("labelCode")
        labelCode: String?,
        @ApiParam("镜像评分", required = false)
        @QueryParam("score")
        score: Int?,
        @ApiParam("镜像排序字段", required = false)
        @QueryParam("sortType")
        sortType: String?,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页大小", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<MarketImageResp>

    @ApiOperation("根据ID查询镜像详情")
    @GET
    @Path("/image/imageIds/{imageId}")
    fun getImageDetailById(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像Id", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<ImageDetail>

    @ApiOperation("根据code查询镜像详情")
    @GET
    @Path("/image/imageCodes/{imageCode}")
    fun getImageDetailByCode(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像标识", required = true)
        @PathParam("imageCode")
        imageCode: String
    ): Result<ImageDetail>

    @ApiOperation("查询镜像版本列表")
    @GET
    @Path("/image/imageCodes/{imageCode}/version/list")
    fun getImageVersionListByCode(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像标识", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页大小", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ImageDetail>>

    @ApiOperation("我的镜像列表")
    @GET
    @Path("/desk/image/list")
    fun getMyImageList(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像名称", required = false)
        @QueryParam("imageName")
        imageName: String?,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页大小", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<MyImage>>

    @ApiOperation("删除镜像")
    @DELETE
    @Path("/image/imageCodes/{imageCode}")
    fun delete(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像Code", required = true)
        @PathParam("imageCode")
        imageCode: String
    ): Result<Boolean>

    @ApiOperation("更新流水线镜像信息")
    @PUT
    @Path("/baseInfo/images/{imageCode}")
    fun updateImageBaseInfo(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像代码 ", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @ApiParam(value = "镜像基本信息修改请求报文体", required = true)
        imageBaseInfoUpdateRequest: ImageBaseInfoUpdateRequest
    ): Result<Boolean>

    @ApiOperation("根据镜像代码获取对应的版本列表信息")
    @GET
    @Path("/projectCodes/{projectCode}/imageCodes/{imageCode}/version/list")
    fun getPipelineImageVersions(
        @ApiParam("项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("镜像代码", required = true)
        @PathParam("imageCode")
        imageCode: String
    ): Result<List<VersionInfo>>
}