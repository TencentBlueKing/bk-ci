package com.tencent.devops.store.api.image.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.request.InstallImageReq
import com.tencent.devops.store.pojo.image.response.JobImageItem
import com.tencent.devops.store.pojo.image.response.JobMarketImageItem
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_MARKET_IMAGE_PROJECT"], description = "研发商店-镜像项目间关系")
@Path("/user/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserImageProjectResource {

    @ApiOperation("安装镜像到项目")
    @POST
    @Path("/image/install")
    fun installImage(
        @ApiParam("token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("安装镜像到项目请求报文体", required = true)
        installImageReq: InstallImageReq
    ): Result<Boolean>

    @ApiOperation("根据镜像标识获取已安装的项目列表")
    @GET
    @Path("/image/installedProjects/{imageCode}")
    fun getInstalledProjects(
        @ApiParam("token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("模版代码", required = true)
        @PathParam("imageCode")
        imageCode: String
    ): Result<List<InstalledProjRespItem>>

    @ApiOperation("根据项目标识获取可用镜像列表（公共+已安装）")
    @GET
    @Path("/image/availableImages")
    fun getAvailableImagesByProjectCode(
        @ApiParam("token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目标识", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @ApiParam("机器类型", required = false)
        @QueryParam("agentType")
        agentType: ImageAgentTypeEnum?,
        @ApiParam("是否推荐", required = false)
        @QueryParam("recommendFlag")
        recommendFlag: Boolean?,
        @ApiParam("分类ID", required = false)
        @QueryParam("classifyId")
        classifyId: String?,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<JobImageItem>?>

    @ApiOperation("根据项目标识获取商店镜像列表")
    @GET
    @Path("/image/jobMarketImages")
    fun getJobMarketImagesByProjectCode(
        @ApiParam("token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目标识", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @ApiParam("机器类型", required = false)
        @QueryParam("agentType")
        agentType: ImageAgentTypeEnum,
        @ApiParam("是否推荐", required = false)
        @QueryParam("recommendFlag")
        recommendFlag: Boolean?,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<JobMarketImageItem?>?>

    @ApiOperation("根据项目标识与镜像名称模糊搜索商店镜像列表（已安装+未安装）")
    @POST
    @Path("/image/jobMarketImages/search")
    fun searchJobMarketImages(
        @ApiParam("token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目标识", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @ApiParam("机器类型", required = false)
        @QueryParam("agentType")
        agentType: ImageAgentTypeEnum,
        @ApiParam("是否推荐", required = false)
        @QueryParam("recommendFlag")
        recommendFlag: Boolean?,
        @ApiParam("部分镜像名称", required = false)
        @QueryParam("imageNamePart")
        imageNamePart: String?,
        @ApiParam("镜像分类Id", required = false)
        @QueryParam("classifyId")
        classifyId: String?,
        @ApiParam("应用范畴", required = false)
        @QueryParam("categoryCode")
        categoryCode: String?,
        @ApiParam("研发来源", required = false)
        @QueryParam("rdType")
        rdType: ImageRDTypeEnum?,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<JobMarketImageItem?>?>
}