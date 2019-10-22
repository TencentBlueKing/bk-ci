package com.tencent.devops.environment.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.DevCloudImageParam
import com.tencent.devops.environment.pojo.DevCloudModel
import com.tencent.devops.environment.pojo.DevCloudVmParam
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_DEVCLOUD"], description = "用户-DEVCLOUD信息")
@Path("/user/devcloud")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserDevCloudResource {

    @ApiOperation("获取DevCloud机型列表")
    @GET
    @Path("/projects/{projectId}/getModelList")
    fun getDevCloudModelList(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<DevCloudModel>>

    @ApiOperation("添加DevCloud虚拟机")
    @POST
    @Path("/projects/{projectId}/addDevCloudVm")
    fun addDevCloudVm(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点配置", required = true)
        devCloudVmParam: DevCloudVmParam
    ): Result<Boolean>

    @ApiOperation("开机DevCloud虚拟机")
    @POST
    @Path("/projects/{projectId}/nodes/{nodeHashId}/startDevCloudVm")
    fun startDevCloudVm(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String
    ): Result<Boolean>

    @ApiOperation("关机DevCloud虚拟机")
    @POST
    @Path("/projects/{projectId}/nodes/{nodeHashId}/stopDevCloudVm")
    fun stopDevCloudVm(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String
    ): Result<Boolean>

    @ApiOperation("销毁DevCloud虚拟机")
    @DELETE
    @Path("/projects/{projectId}/nodes/{nodeHashId}/deleteDevCloudVm")
    fun deleteDevCloudVm(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String
    ): Result<Boolean>

    @ApiOperation("制作镜像")
    @POST
    @Path("/projects/{projectId}/nodes/{nodeHashId}/createImage")
    fun createImage(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @ApiParam("镜像参数", required = true)
        devCloudImage: DevCloudImageParam
    ): Result<Boolean>

    @ApiOperation("制作镜像结果确认")
    @POST
    @Path("/projects/{projectId}/nodes/{nodeHashId}/confirm")
    fun createImageResultConfirm(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String
    ): Result<Boolean>

    @ApiOperation("查询DevCloud虚拟机状态")
    @GET
    @Path("/projects/{projectId}/nodes/{nodeHashId}/getDevCloudVm")
    fun getDevCloudVm(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String
    ): Result<Map<String, Any>>
}