package com.tencent.devops.environment.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.NodeBaseInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_AUTH_NODE"], description = "服务-节点-权限中心")
@Path("/service/node/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface RemoteNodeResource {

    @ApiOperation("分页获取节点列表")
    @GET
    @Path("/projects/{projectId}/list/page")
    fun listNodeByPage(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int? = null,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int? = null
    ): Result<Page<NodeBaseInfo>>

    @ApiOperation("获取节点信息")
    @GET
    @Path("/projects/{projectId}/infos")
    fun getNodeInfos(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点Id串", required = true)
        @QueryParam("nodeIds")
        nodeIds: List<String>
    ): Result<List<NodeBaseInfo>>
}