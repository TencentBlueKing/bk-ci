/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.api.image

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.image.pojo.DockerRepo
import com.tencent.devops.store.pojo.common.StoreVisibleDeptResp
import com.tencent.devops.store.pojo.image.request.ApproveImageReq
import com.tencent.devops.store.pojo.image.request.ImageCreateRequest
import com.tencent.devops.store.pojo.image.request.ImageUpdateRequest
import com.tencent.devops.store.pojo.image.request.OpImageSortTypeEnum
import com.tencent.devops.store.pojo.image.response.ImageDetail
import com.tencent.devops.store.pojo.image.response.OpImageResp
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_MARKET_IMAGE"], description = "OP-流水线-镜像")
@Path("/op/market/image")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpImageResource {

    @ApiOperation("新增镜像")
    @POST
    @Path("/")
    fun addImage(
        @ApiParam("PAAS_CC Token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "新增镜像请求报文体", required = true)
        imageCreateRequest: ImageCreateRequest
    ): Result<String>

    @ApiOperation("确认镜像通过测试")
    @PUT
    @Path("/release/passTest/imageIds/{imageId}")
    fun passTest(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像Id", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<Boolean>

    @ApiOperation("重新验证镜像")
    @PUT
    @Path("/release/recheck/imageIds/{imageId}")
    fun recheck(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像Id", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<Boolean>

    @ApiOperation("更新镜像信息")
    @PUT
    @Path("/{imageId}")
    fun updateImage(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像ID", required = true)
        @PathParam("imageId")
        imageId: String,
        @ApiParam(value = "更新镜像请求报文体", required = true)
        imageUpdateRequest: ImageUpdateRequest
    ): Result<Boolean>

    @ApiOperation("根据ID删除镜像信息")
    @DELETE
    @Path("/imageIds/{imageId}")
    fun deleteImageById(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像ID", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<Boolean>

    @ApiOperation("根据ID获取镜像信息")
    @GET
    @Path("/imageIds/{imageId}")
    fun getImageById(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像ID", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<ImageDetail>

    @ApiOperation("根据镜像代码获取镜像信息")
    @GET
    @Path("/imageCodes/{imageCode}")
    fun getImagesByCodeAndVersion(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像代码", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @ApiParam("版本号", required = false)
        @QueryParam("version")
        version: String?
    ): Result<ImageDetail>

    @ApiOperation("根据镜像代码获取镜像版本列表")
    @GET
    @Path("/imageCodes/{imageCode}/versions/list")
    fun getImageVersionsByCode(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像代码", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ImageDetail>>

    @ApiOperation("下架镜像")
    @PUT
    @Path("/offline/imageCodes/{imageCode}/versions")
    fun offlineImage(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像代码", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @ApiParam("版本号", required = false)
        @QueryParam("version")
        version: String?,
        @ApiParam("原因", required = false)
        @QueryParam("reason")
        reason: String?
    ): Result<Boolean>

    @ApiOperation("获取市场镜像")
    @GET
    @Path("/")
    fun listImages(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像名称", required = false)
        @QueryParam("imageName")
        imageName: String?,
        @ApiParam("镜像来源类型", required = false)
        @QueryParam("imageSourceType")
        imageSourceType: ImageType?,
        @ApiParam("是否处于流程中", required = false)
        @QueryParam("processFlag")
        processFlag: Boolean?,
        @ApiParam("镜像分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @ApiParam("应用范畴", required = false)
        @QueryParam("categoryCodes")
        categoryCodes: String?,
        @ApiParam("功能标签", required = false)
        @QueryParam("labelCodes")
        labelCodes: String?,
        @ApiParam("排序", required = false)
        @QueryParam("sortType")
        sortType: OpImageSortTypeEnum?,
        @ApiParam("是否降序", required = false)
        @QueryParam("desc")
        desc: Boolean?,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<OpImageResp>

    @ApiOperation("审核镜像")
    @Path("/{imageId}/approve")
    @PUT
    fun approveImage(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("ID", required = true)
        @PathParam("imageId")
        imageId: String,
        @ApiParam("审核镜像请求报文")
        approveImageReq: ApproveImageReq
    ): Result<Boolean>

    @ApiOperation("查看可见范围")
    @GET
    @Path("/{imageCode}/visible")
    fun getVisibleDept(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("代码", required = true)
        @PathParam("imageCode")
        imageCode: String
    ): Result<StoreVisibleDeptResp?>

    @ApiOperation("查找蓝盾仓库关联镜像信息")
    @GET
    @Path("/repo/bk/names/{imageRepoName}")
    fun getBkRelImageInfo(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像在仓库中的名称", required = true)
        @PathParam("imageRepoName")
        imageRepoName: String,
        @ApiParam("需要回显镜像tag的镜像ID", required = false)
        @QueryParam("imageId")
        imageId: String?
    ): Result<DockerRepo?>
}
