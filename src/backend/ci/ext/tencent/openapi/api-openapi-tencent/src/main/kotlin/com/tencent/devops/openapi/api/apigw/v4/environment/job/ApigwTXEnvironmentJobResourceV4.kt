package com.tencent.devops.openapi.api.apigw.v4.environment.job

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.environment.pojo.job.agentreq.ApiGwInstallAgentReq
import com.tencent.devops.environment.pojo.job.agentres.AgentResult
import com.tencent.devops.environment.pojo.job.agentres.InstallAgentResult
import com.tencent.devops.environment.pojo.job.agentres.ObtainManualCommandResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentTaskStatusResult
import com.tencent.devops.environment.pojo.job.agentres.OperateStepInstanceResult
import com.tencent.devops.environment.pojo.job.jobreq.CreateAccountReq
import com.tencent.devops.environment.pojo.job.jobreq.DeleteAccountReq
import com.tencent.devops.environment.pojo.job.jobreq.FileDistributeReq
import com.tencent.devops.environment.pojo.job.jobreq.OpOperateReq
import com.tencent.devops.environment.pojo.job.jobreq.OperateStepInstanceReq
import com.tencent.devops.environment.pojo.job.jobreq.QueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.jobreq.ScriptExecuteReq
import com.tencent.devops.environment.pojo.job.jobreq.TaskTerminateReq
import com.tencent.devops.environment.pojo.job.jobresp.CreateAccountResult
import com.tencent.devops.environment.pojo.job.jobresp.DeleteAccountResult
import com.tencent.devops.environment.pojo.job.jobresp.GetAccountListResult
import com.tencent.devops.environment.pojo.job.jobresp.FileDistributeResult
import com.tencent.devops.environment.pojo.job.jobresp.GetStepInstanceDetailResult
import com.tencent.devops.environment.pojo.job.jobresp.GetStepInstanceStatusResult
import com.tencent.devops.environment.pojo.job.jobresp.JobResult
import com.tencent.devops.environment.pojo.job.jobresp.OpOperateResult
import com.tencent.devops.environment.pojo.job.jobresp.QueryJobInstanceLogsResult
import com.tencent.devops.environment.pojo.job.jobresp.QueryJobInstanceStatusResult
import com.tencent.devops.environment.pojo.job.jobresp.ScriptExecuteResult
import com.tencent.devops.environment.pojo.job.jobresp.TaskTerminateResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OPENAPI_JOB_V4", description = "OPENAPI-JOB")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/job")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwTXEnvironmentJobResourceV4 {

    @Operation(summary = "脚本执行的Job接口", tags = ["v4_app_job_script_execute"])
    @POST
    @Path("/{projectId}/script_execute")
    fun executeScript(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String? = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "执行脚本的信息", required = true)
        scriptExecuteReq: ScriptExecuteReq
    ): JobResult<ScriptExecuteResult>

    @Operation(summary = "文件分发的Job接口", tags = ["v4_app_job_file_distribute"])
    @POST
    @Path("/{projectId}/file_distribute")
    fun distributeFile(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String? = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "文件分发的信息", required = true)
        fileDistributeReq: FileDistributeReq
    ): JobResult<FileDistributeResult>

    @Operation(summary = "终止任务的Job接口", tags = ["v4_app_job_task_terminate"])
    @POST
    @Path("/{projectId}/task_terminate")
    fun terminateTask(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String? = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "终止任务的信息", required = true)
        taskTerminateReq: TaskTerminateReq
    ): JobResult<TaskTerminateResult>

    @Operation(
        summary =
        "查询任务状态的Job接口",
        tags = ["v4_app_job_query_job_instance_status", "v4_user_job_query_job_instance_status"]
    )
    @GET
    @Path("/{projectId}/query_job_instance_status")
    fun queryJobInstanceStatus(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String? = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "作业实例ID", required = true)
        @QueryParam("jobInstanceId")
        jobInstanceId: Long,
        @Parameter(description = "是否返回每个ip上的任务详情，默认false")
        @QueryParam("returnIpResult")
        returnIpResult: Boolean?
    ): JobResult<QueryJobInstanceStatusResult>

    @Operation(
        summary = "批量查询日志的Job接口",
        tags = ["v4_app_job_query_job_instance_logs", "v4_user_job_query_job_instance_logs"]
    )
    @POST
    @Path("/{projectId}/query_job_instance_logs")
    fun queryJobInstanceLogs(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String? = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量查询日志的请求信息", required = true)
        queryJobInstanceLogsReq: QueryJobInstanceLogsReq
    ): JobResult<QueryJobInstanceLogsResult>

    @Operation(summary = "创建帐号的Job接口", tags = ["v4_app_job_create_account"])
    @POST
    @Path("/{projectId}/create_account")
    fun createAccount(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String? = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "创建帐号的信息", required = true)
        createAccountReq: CreateAccountReq
    ): JobResult<CreateAccountResult>

    @Operation(summary = "删除帐号的Job接口", tags = ["v4_app_job_delete_account"])
    @POST
    @Path("/{projectId}/delete_account")
    fun deleteAccount(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String? = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "创建帐号的信息", required = true)
        deleteAccountReq: DeleteAccountReq
    ): JobResult<DeleteAccountResult>

    @Operation(summary = "查询有权限账号列表的Job接口", tags = ["v4_app_job_get_account_list"])
    @GET
    @Path("/{projectId}/get_account_list")
    fun getAccountList(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String? = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "账号名称")
        @QueryParam("account")
        account: String?,
        @Parameter(description = "账号别名")
        @QueryParam("alias")
        alias: String?,
        @Parameter(description = "账号用途(1：系统账号, 2：DB账号, 不传则不区分)")
        @QueryParam("category")
        category: Int?,
        @Parameter(description = "分页记录起始位置(不传默认0)")
        @QueryParam("start")
        start: Int?,
        @Parameter(description = "单次返回最大记录数(最大1000，不传默认20)")
        @QueryParam("length")
        length: Int?
    ): JobResult<GetAccountListResult>

    @Operation(summary = "获取步骤实例详情数据", tags = ["v4_app_job_get_step_instance_detail"])
    @GET
    @Path("/{projectId}/get_step_instance_detail")
    fun getStepInstanceDetail(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String? = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "作业实例ID")
        @QueryParam("jobInstanceId")
        jobInstanceId: Long,
        @Parameter(description = "步骤实例ID")
        @QueryParam("stepInstanceId")
        stepInstanceId: Long
    ): JobResult<GetStepInstanceDetailResult>

    @Operation(summary = "获取步骤实例中各主机的任务执行状态数据", tags = ["v4_app_job_get_step_instance_status"])
    @GET
    @Path("/{projectId}/get_step_instance_status")
    fun getStepInstanceStatus(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String? = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "作业实例ID", required = true)
        @QueryParam("jobInstanceId")
        jobInstanceId: Long,
        @Parameter(description = "步骤实例ID", required = true)
        @QueryParam("stepInstanceId")
        stepInstanceId: Long,
        @Parameter(description = "步骤重试次数")
        @QueryParam("executeCount")
        executeCount: Int?,
        @Parameter(description = "滚动批次")
        @QueryParam("batch")
        batch: Int?,
        @Parameter(description = "每个分组里的最大主机数量，不传则返回全量数据。")
        @QueryParam("maxHostNumPerGroup")
        maxHostNumPerGroup: Int?,
        @Parameter(description = "日志搜索关键字")
        @QueryParam("keyword")
        keyword: String?,
        @Parameter(description = "主机IP/IPv6搜索关键字")
        @QueryParam("searchIp")
        searchIp: String?,
        @Parameter(description = "执行状态")
        @QueryParam("status")
        status: Int?,
        @Parameter(description = "结果标签")
        @QueryParam("tag")
        tag: String?
    ): JobResult<GetStepInstanceStatusResult>

    @Operation(summary = "对执行的实例的步骤进行操作的接口", tags = ["v4_app_job_operate_step_instance"])
    @POST
    @Path("/{projectId}/operate_step_instance")
    fun operateStepInstance(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String? = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "操作执行实例步骤的信息", required = true)
        operateStepInstanceReq: OperateStepInstanceReq
    ): JobResult<OperateStepInstanceResult>

    @Operation(summary = "操作项目灰度状态的OP接口", tags = ["v4_app_job_operate_op_project"])
    @POST
    @Path("/operate_op_project")
    fun operateOpProject(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "op操作请求信息", required = true)
        opOperateReq: OpOperateReq
    ): OpOperateResult

    @Operation(summary = "部署节点cc状态轮询接口", tags = ["v4_app_job_check_deploy_nodes_in_cmdb"])
    @POST
    @Path("/stock_data_update/check_deploy_nodes_in_cmdb")
    fun checkDeployNodesInCmdb(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE
    )

    @Operation(summary = "gse agent状态版本更新接口", tags = ["v4_app_job_update_gse_agent"])
    @POST
    @Path("/stock_data_update/update_gse_agent")
    fun updateGseAgent(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE
    )

    @Operation(summary = "将存量不在CC中的机器导入CC中的接口", tags = ["v4_app_job_add_stock_node_to_cc"])
    @POST
    @Path("/stock_data_update/add_stock_node_to_cc")
    fun addStockNodeToCC(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE
    )

    @Operation(summary = "给存量部署节点写入机器server_id", tags = ["v4_app_job_write_server_id"])
    @POST
    @Path("/stock_data_update/write_server_id")
    fun writeServerId(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE
    )

    @Operation(
        summary = "在指定节点上安装GSE Agent",
        tags = ["v4_app_job_install_agent"]
    )
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Successful Operation")])
    @POST
    @Path("/{projectId}/install_agent")
    fun installAgent(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "安装agent的请求信息", required = true)
        apiGwInstallAgentReq: ApiGwInstallAgentReq
    ): AgentResult<InstallAgentResult>

    @Operation(
        summary = "查询节点的agent安装任务的状态",
        tags = ["v4_app_job_query_agent_task_status"]
    )
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Successful Operation")])
    @GET
    @Path("/{projectId}/query_agent_task_status")
    fun queryAgentTaskStatus(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @QueryParam("jobId")
        jobId: Int,
        @Parameter(description = "查询agent任务状态的页码", required = true)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "查询agent任务状态的页码", required = true)
        @QueryParam("pageSize")
        pageSize: Int = 10
    ): AgentResult<QueryAgentTaskStatusResult>

    @Operation(
        summary = "获取手动安装agent的命令",
        tags = ["v4_app_job_obtain_manual_installation_command"]
    )
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Successful Operation")])
    @GET
    @Path("/{projectId}/obtain_manual_installation_command")
    fun obtainManualInstallationCommand(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @QueryParam("jobId")
        jobId: Int,
        @Parameter(description = "主机内网ip", required = true)
        @QueryParam("innerIp")
        innerIp: String,
        @Parameter(description = "主机云区域ID，默认为公共区域，云区域ID为0")
        @QueryParam("bkCloudId")
        bkCloudId: Int = 0
    ): AgentResult<ObtainManualCommandResult>
}
