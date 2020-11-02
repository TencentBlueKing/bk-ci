package com.tencent.devops.experience.api.app

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.experience.pojo.index.IndexAppInfoVO
import com.tencent.devops.experience.pojo.index.IndexBannerVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import java.util.Date
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["APP_EXPERIENCE"], description = "版本体验-发布体验V2")
@Path("/app/experiences/v2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AppExperienceV2Resource {

    @ApiOperation("banner列表")
    @Path("/banners")
    @GET
    fun banners(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<IndexBannerVO>> {
        //TODO 真正的实现
        val banners = mutableListOf<IndexBannerVO>()
        for (i in 1..3) {
            banners.add(
                IndexBannerVO(
                    experienceHashId = HashUtil.encodeIntId(i),
                    bannerUrl = "https://www.tencent.com/img/brief/pic.jpg"
                )
            )
        }

        return Result(banners)
    }

    @ApiOperation("热门推荐")
    @Path("/hots")
    @GET
    fun hots(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<IndexAppInfoVO>> {
        //TODO 真正的实现
        val banners = mutableListOf<IndexAppInfoVO>()
        for (i in 1..20) {
            banners.add(
                IndexAppInfoVO(
                    experienceHashId = HashUtil.encodeIntId(i),
                    experienceName = "test_$i",
                    createTime = Date().time,
                    size = i * 1031467 + 1013L,
                    url = "https://v2.bkdevops.qq.com/app/download/devops_app.apk",
                    logoUrl = "http://radosgw.open.oa.com/paas_backend/ieod/prod/file/png/random_15663728753195467594717312328557.png"
                )
            )
        }

        return Result(banners)
    }

    @ApiOperation("鹅厂必备")
    @Path("/necessary")
    @GET
    fun necessary(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<IndexAppInfoVO>> {
        //TODO 真正的实现
        val banners = mutableListOf<IndexAppInfoVO>()
        for (i in 1..10) {
            banners.add(
                IndexAppInfoVO(
                    experienceHashId = HashUtil.encodeIntId(i),
                    experienceName = "test_$i",
                    createTime = Date().time,
                    size = i * 1031461 + 1013L,
                    url = "https://v2.bkdevops.qq.com/app/download/devops_app.apk",
                    logoUrl = "http://radosgw.open.oa.com/paas_backend/ieod/prod/file/png/random_15663728753195467594717312328557.png"
                )
            )
        }

        return Result(banners)
    }

    @ApiOperation("本周最新")
    @Path("/newest")
    @GET
    fun newest(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<IndexAppInfoVO>> {
        //TODO 真正的实现
        val banners = mutableListOf<IndexAppInfoVO>()
        for (i in 1..19) {
            banners.add(
                IndexAppInfoVO(
                    experienceHashId = HashUtil.encodeIntId(i),
                    experienceName = "test_$i",
                    createTime = Date().time,
                    size = i * 1031463 + 1013L,
                    url = "https://v2.bkdevops.qq.com/app/download/devops_app.apk",
                    logoUrl = "http://radosgw.open.oa.com/paas_backend/ieod/prod/file/png/random_15663728753195467594717312328557.png"
                )
            )
        }

        return Result(banners)
    }
}