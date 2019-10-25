package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.GcloudConfResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_GCLOUD_CONF"], description = "用户-GCLOUD配置")
@Path("/user/gcloud")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserGcloudConfResource {

    @ApiOperation("新增GCLOUD配置")
    @POST
    @Path("/conf/create")
    fun create(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("区域", required = true)
        @QueryParam("region")
        region: String,
        @ApiParam("地址", required = true)
        @QueryParam("address")
        address: String,
        @ApiParam("文件地址", required = true)
        @QueryParam("address")
        fileAddress: String,
        @ApiParam("备注", required = false)
        @QueryParam("remark")
        remark: String?
    ): Result<Map<String, Int>>

    @ApiOperation("查询GCLOUD配置")
    @GET
    @Path("/conf/list")
    fun getList(
        @ApiParam(value = "开始页数，从1开始", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int,
        @ApiParam(value = "每页数据条数", required = false, defaultValue = "12")
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<GcloudConfResponse?>
}