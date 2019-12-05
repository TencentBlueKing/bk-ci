package com.tencent.devops.store.api.image

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_DATATRANSFER_IMAGE"], description = "OP-数据迁移-镜像")
@Path("/op/datatransfer/image")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpImageDataTransferResource {

    @ApiOperation("初始化系统分类与范畴")
    @PUT
    @Path("/initClassifyAndCategory")
    fun initClassifyAndCategory(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "分类代码", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @ApiParam(value = "分类名称", required = false)
        @QueryParam("classifyName")
        classifyName: String?,
        @ApiParam(value = "范畴代码", required = false)
        @QueryParam("categoryCode")
        categoryCode: String?,
        @ApiParam(value = "范畴名称", required = false)
        @QueryParam("categoryName")
        categoryName: String?
    ): Result<Int>

    @ApiOperation("按项目迁移已拷贝为构建镜像的数据")
    @PUT
    @Path("/transferImage")
    fun transferImage(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目编码", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @ApiParam(value = "分类代码", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @ApiParam(value = "范畴代码", required = false)
        @QueryParam("categoryCode")
        categoryCode: String?
    ): Result<Int>

    @ApiOperation("清除已迁移数据特征记录")
    @PUT
    @Path("/clearFinishedSet")
    fun clearFinishedSet(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<Int>
}