package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.job.AddCmdbNodesRes
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_V4_ENVIRONMENT"], description = "OPENAPI-环境管理")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/environment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwTXEnvironmentResourceV4 {

    @ApiOperation("导入CMDB节点", tags = ["v4_app_node_cmdb_import", "v4_user_node_cmdb_import"])
    @POST
    @Path("/projects/{projectId}/add_cmdb_nodes")
    fun addCmdbNodes(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "CMDB节点 IP", required = true)
        nodeIps: List<String>
    ): Result<AddCmdbNodesRes>

    @ApiOperation("部署节点cmdb状态轮询接口", tags = ["v4_app_job_check_deploy_nodes_in_cmdb"])
    @POST
    @Path("/stock_data_update/v4_app_job_check_deploy_nodes_in_cmdb")
    fun checkDeployNodesInCmdb(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    )

    @ApiOperation("添加不在cc中的部署节点到cc中的接口", tags = ["v4_app_job_add_nodes_to_cc"])
    @POST
    @Path("/stock_data_update/add_nodes_to_cc")
    fun addNodesToCC(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    )

    @ApiOperation("agent状态版本更新接口", tags = ["v4_app_job_agent_update"])
    @POST
    @Path("/stock_data_update/agent_update")
    fun agentUpdate(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    )
}
