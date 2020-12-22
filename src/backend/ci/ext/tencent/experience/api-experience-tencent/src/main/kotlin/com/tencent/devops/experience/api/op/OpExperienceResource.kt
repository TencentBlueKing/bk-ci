package com.tencent.devops.experience.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.enums.Platform
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["EXPERIENCE_OP"], description = "版本体验-OP")
@Path("/op/experience")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpExperienceResource {
    @ApiOperation("转换数据")
    @Path("/transform")
    @POST
    fun transform(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<String>

    @ApiOperation("修改鹅厂必备")
    @Path("/public/switchNecessary")
    @POST
    fun switchNecessary(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "公开体验记录ID", required = true)
        @QueryParam("id")
        id: Long
    ): Result<String>

    @ApiOperation("修改公开体验banner")
    @Path("/public/setBannerUrl")
    @POST
    fun setBannerUrl(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "公开体验记录ID", required = true)
        @QueryParam("id")
        id: Long,
        @ApiParam(value = "banner地址", required = true)
        @QueryParam("bannerUrl")
        bannerUrl: String
    ): Result<String>

    @ApiOperation("公开体验上下线")
    @Path("/public/switchOnline")
    @POST
    fun switchOnline(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "公开体验记录ID", required = true)
        @QueryParam("id")
        id: Long
    ): Result<String>

    @ApiOperation("新增搜索推荐")
    @Path("/search/addRecommend")
    @POST
    fun addRecommend(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "推荐内容", required = true)
        @QueryParam("content")
        content: String,
        @ApiParam(value = "平台", required = true)
        @QueryParam("platform")
        platform: Platform
    ): Result<String>

    @ApiOperation("删除搜索推荐")
    @Path("/search/removeRecommend")
    @DELETE
    fun removeRecommend(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "搜索推荐ID", required = true)
        @QueryParam("id")
        id: Long
    ): Result<String>
}
