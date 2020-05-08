package com.tencent.devops.dispatch.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.pojo.DockerHostLoadConfig
import com.tencent.devops.dispatch.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.pojo.DockerIpListPage
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.DELETE
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_DISPATCH_IDC"], description = "OP-IDC构建机管理接口")
@Path("/op/dispatchDocker")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OPDispatchDockerResource {

    @GET
    @Path("/getDockerIpList")
    @ApiOperation("获取Docker构建机列表")
    fun listDispatchDocker(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "10")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<DockerIpListPage<DockerIpInfoVO>>

    @POST
    @Path("/add")
    @ApiOperation("批量新增Docker构建机")
    fun createDispatchDocker(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("IDC构建机信息", required = true)
        dockerIpInfoVOs: List<DockerIpInfoVO>
    ): Result<Boolean>

    @PUT
    @Path("/update/{dockerIpInfoId}")
    @ApiOperation("更新Docker构建机状态")
    fun updateDispatchDocker(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("IDC构建机ID", required = true)
        @PathParam("dockerIpInfoId")
        dockerIpInfoId: Long,
        @ApiParam("IDC构建机信息", required = true)
        dockerIpInfoVO: DockerIpInfoVO
    ): Result<Boolean>

    @DELETE
    @Path("/delete/{dockerIpInfoId}")
    @ApiOperation("删除Docker构建机")
    fun deleteDispatchDocker(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("服务ID", required = true)
        @PathParam("dockerIpInfoId")
        dockerIpInfoId: Long
    ): Result<Boolean>

    @POST
    @Path("/load-config/add")
    @ApiOperation("新增Docker构建机负载配置")
    fun createDockerHostLoadConfig(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("创建IDC构建机所需信息", required = true)
        dockerHostLoadConfigMap: Map<String, DockerHostLoadConfig>
    ): Result<Boolean>
}