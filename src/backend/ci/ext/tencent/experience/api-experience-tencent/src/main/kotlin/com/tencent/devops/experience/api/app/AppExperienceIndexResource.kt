package com.tencent.devops.experience.api.app

import com.tencent.devops.common.api.auth.AUTH_HEADER_PLATFORM
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.index.IndexAppInfoVO
import com.tencent.devops.experience.pojo.index.IndexBannerVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["APP_EXPERIENCE_INDEX"], description = "版本体验-首页")
@Path("/app/experiences/index")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AppExperienceIndexResource {

    @ApiOperation("banner列表")
    @Path("/banners")
    @GET
    fun banners(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("平台", required = true)
        @HeaderParam(AUTH_HEADER_PLATFORM)
        platform: Int?,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Pagination<IndexBannerVO>>

    @ApiOperation("热门推荐")
    @Path("/hots")
    @GET
    fun hots(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("平台", required = true)
        @HeaderParam(AUTH_HEADER_PLATFORM)
        platform: Int?,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Pagination<IndexAppInfoVO>>

    @ApiOperation("鹅厂必备")
    @Path("/necessary")
    @GET
    fun necessary(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("平台", required = true)
        @HeaderParam(AUTH_HEADER_PLATFORM)
        platform: Int?,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Pagination<IndexAppInfoVO>>

    @ApiOperation("本周最新")
    @Path("/newest")
    @GET
    fun newest(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("平台", required = true)
        @HeaderParam(AUTH_HEADER_PLATFORM)
        platform: Int?,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Pagination<IndexAppInfoVO>>

    @ApiOperation("分类列表--热门")
    @Path("/category/{categoryId}/hot")
    @GET
    fun hotCategory(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("平台", required = true)
        @HeaderParam(AUTH_HEADER_PLATFORM)
        platform: Int?,
        @ApiParam("类别ID,1--游戏,2--工具,3--生活,4--社交", required = true)
        @PathParam("categoryId")
        categoryId: Int,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Pagination<IndexAppInfoVO>>

    @ApiOperation("分类列表--最新")
    @Path("/category/{categoryId}/new")
    @GET
    fun newCategory(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("平台", required = true)
        @HeaderParam(AUTH_HEADER_PLATFORM)
        platform: Int?,
        @ApiParam("类别ID,1--游戏,2--工具,3--生活,4--社交", required = true)
        @PathParam("categoryId")
        categoryId: Int,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Pagination<IndexAppInfoVO>>
}