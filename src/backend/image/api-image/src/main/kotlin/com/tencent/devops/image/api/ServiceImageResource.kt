package com.tencent.devops.image.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.image.pojo.DockerRepo
import com.tencent.devops.image.pojo.DockerTag
import com.tencent.devops.image.pojo.ImagePageData
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

@Api(tags = ["SERVICE_IMAGE"], description = "镜像-镜像服务")
@Path("/service/image")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.MULTIPART_FORM_DATA)
interface ServiceImageResource {

    @ApiOperation("获取项目Docker构建镜像列表")
    @Path("/projects/{projectId}/listDockerBuildImages")
    @GET
    fun listDockerBuildImages(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<DockerTag>>

    @ApiOperation("镜像仓库支持升级为构建镜像")
    @Path("/projects/{projectId}/setBuildImage")
    @POST
    fun setBuildImage(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "镜像repo", required = true)
        @QueryParam("imageRepo")
        imageRepo: String,
        @ApiParam(value = "镜像tag", required = true)
        @QueryParam("imageTag")
        imageTag: String
    ): Result<Boolean>

    @ApiOperation("获取公共镜像列表")
    @Path("/listPublicImages")
    @GET
    fun listPublicImages(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "查询关键字", required = false)
        @QueryParam("searchKey")
        searchKey: String?,
        @ApiParam(value = "分页start", required = false)
        @QueryParam("start")
        start: Int?,
        @ApiParam(value = "分页大小", required = false)
        @QueryParam("limit")
        limit: Int?
    ): Result<ImagePageData>

    @ApiOperation("获取项目镜像列表")
    @Path("/{projectId}/listImages")
    @GET
    fun listProjectImages(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "查询关键字", required = false)
        @QueryParam("searchKey")
        searchKey: String?,
        @ApiParam(value = "分页start", required = false)
        @QueryParam("start")
        start: Int?,
        @ApiParam(value = "分页大小", required = false)
        @QueryParam("limit")
        limit: Int?
    ): Result<ImagePageData>

    @ApiOperation("获取镜像信息")
    @Path("/getImageInfo")
    @GET
    fun getImageInfo(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "镜像repo", required = true)
        @QueryParam("imageRepo")
        imageRepo: String,
        @ApiParam(value = "开始索引", required = false)
        @QueryParam("tagStart")
        tagStart: Int?,
        @ApiParam(value = "页大小", required = false)
        @QueryParam("tagLimit")
        tagLimit: Int?
    ): Result<DockerRepo?>

    @ApiOperation("获取构建镜像信息")
    @Path("/getTagInfo")
    @GET
    fun getTagInfo(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "镜像repo", required = true)
        @QueryParam("imageRepo")
        imageRepo: String,
        @ApiParam(value = "镜像tag", required = true)
        @QueryParam("imageTag")
        imageTag: String
    ): Result<DockerTag?>

    @ApiOperation("获取项目DevCloud构建镜像列表")
    @Path("/{projectId}/listDevCloudImages/{public}")
    @GET
    fun listDevCloudImages(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("是否公共镜像", required = true)
        @PathParam("public")
        public: Boolean
    ): Result<List<DockerTag>>
}