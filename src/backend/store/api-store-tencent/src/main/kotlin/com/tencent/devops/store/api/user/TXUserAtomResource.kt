package com.tencent.devops.store.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.AtomBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.InstalledAtom
import com.tencent.devops.store.pojo.unInstallReq
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

@Api(tags = ["USER_PIPELINE_ATOM"], description = "流水线-插件")
@Path("/user/pipeline/atom")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface TXUserAtomResource {

    @ApiOperation("获取项目下已安装的插件列表")
    @GET
    @Path("/projectCodes/{projectCode}/list")
    fun getInstalledAtoms(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("插件分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<InstalledAtom>>

    @ApiOperation("更新流水线插件信息")
    @PUT
    @Path("/baseInfo/atoms/{atomCode}")
    fun updateAtomBaseInfo(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("插件代码 ", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam(value = "插件基本信息修改请求报文体", required = true)
        atomBaseInfoUpdateRequest: AtomBaseInfoUpdateRequest
    ): Result<Boolean>

    @ApiOperation("卸载插件")
    @Path("/projectCodes/{projectCode}/atoms/{atomCode}")
    @DELETE
    fun uninstallAtom(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("插件代码 ", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("卸载插件请求包体", required = true)
        unInstallReq: unInstallReq
    ): Result<Boolean>
}