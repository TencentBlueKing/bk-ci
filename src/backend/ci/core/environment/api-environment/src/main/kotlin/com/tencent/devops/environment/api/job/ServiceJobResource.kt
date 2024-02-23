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

package com.tencent.devops.environment.api.job

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.environment.pojo.job.req.CreateAccountReq
import com.tencent.devops.environment.pojo.job.req.DeleteAccountReq
import com.tencent.devops.environment.pojo.job.req.FileDistributeReq
import com.tencent.devops.environment.pojo.job.req.OpOperateReq
import com.tencent.devops.environment.pojo.job.req.QueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.req.ScriptExecuteReq
import com.tencent.devops.environment.pojo.job.req.TaskTerminateReq
import com.tencent.devops.environment.pojo.job.resp.CreateAccountResult
import com.tencent.devops.environment.pojo.job.resp.DeleteAccountResult
import com.tencent.devops.environment.pojo.job.resp.FileDistributeResult
import com.tencent.devops.environment.pojo.job.resp.GetAccountListResult
import com.tencent.devops.environment.pojo.job.resp.GetStepInstanceDetailResult
import com.tencent.devops.environment.pojo.job.resp.GetStepInstanceStatusResult
import com.tencent.devops.environment.pojo.job.resp.JobResult
import com.tencent.devops.environment.pojo.job.resp.OpOperateResult
import com.tencent.devops.environment.pojo.job.resp.QueryJobInstanceLogsResult
import com.tencent.devops.environment.pojo.job.resp.QueryJobInstanceStatusResult
import com.tencent.devops.environment.pojo.job.resp.ScriptExecuteResult
import com.tencent.devops.environment.pojo.job.resp.TaskTerminateResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

@Tag(name = "SERVICE_JOB", description = "服务-JOB")
@Path("/service/job")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceJobResource {
    @Operation(summary = "脚本执行的Job接口")
    @POST
    @Path("/{projectId}/script_execute")
    fun executeScript(
        @Parameter(name = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "执行脚本的信息", required = true)
        scriptExecuteReq: ScriptExecuteReq
    ): JobResult<ScriptExecuteResult>

    @Operation(summary = "文件分发的Job接口")
    @POST
    @Path("/{projectId}/file_distribute")
    fun distributeFile(
        @Parameter(name = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "文件分发的信息", required = true)
        fileDistributeReq: FileDistributeReq
    ): JobResult<FileDistributeResult>

    @Operation(summary = "终止任务的Job接口")
    @POST
    @Path("/{projectId}/task_terminate")
    fun terminateTask(
        @Parameter(name = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "终止任务的信息", required = true)
        taskTerminateReq: TaskTerminateReq
    ): JobResult<TaskTerminateResult>

    @Operation(summary = "查询任务状态的Job接口")
    @GET
    @Path("/{projectId}/query_job_instance_status")
    fun queryJobInstanceStatus(
        @Parameter(name = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "作业实例ID", required = true)
        @QueryParam("jobInstanceId")
        jobInstanceId: Long,
        @Parameter(name = "是否返回每个ip上的任务详情，默认false", required = true)
        @QueryParam("returnIpResult")
        returnIpResult: Boolean? = false
    ): JobResult<QueryJobInstanceStatusResult>

    @Operation(summary = "批量查询日志的Job接口")
    @POST
    @Path("/{projectId}/query_job_instance_logs")
    fun queryJobInstanceLogs(
        @Parameter(name = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "批量查询日志的请求信息", required = true)
        queryLogsReq: QueryJobInstanceLogsReq
    ): JobResult<QueryJobInstanceLogsResult>

    @Operation(summary = "创建帐号的Job接口")
    @POST
    @Path("/{projectId}/create_account")
    fun createAccount(
        @Parameter(name = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "执行脚本的信息", required = true)
        createAccountReq: CreateAccountReq
    ): JobResult<CreateAccountResult>

    @Operation(summary = "删除帐号的Job接口")
    @POST
    @Path("/{projectId}/delete_account")
    fun deleteAccount(
        @Parameter(name = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "执行脚本的信息", required = true)
        deleteAccountReq: DeleteAccountReq
    ): JobResult<DeleteAccountResult>

    @Operation(summary = "请求上云版job - 查询有权限账号列表的Job接口")
    @GET
    @Path("/{projectId}/get_account_list")
    fun getAccountList(
        @Parameter(name = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "账号名称")
        @QueryParam("account")
        account: String?,
        @Parameter(name = "账号别名")
        @QueryParam("alias")
        alias: String?,
        @Parameter(name = "账号用途(1：系统账号, 2：DB账号, 不传则不区分)")
        @QueryParam("category")
        category: Int?,
        @Parameter(name = "分页记录起始位置(不传默认0)")
        @QueryParam("start")
        start: Int?,
        @Parameter(name = "单次返回最大记录数(最大1000，不传默认20)")
        @QueryParam("length")
        length: Int?
    ): JobResult<GetAccountListResult>

    @Operation(summary = "请求上云版job - 获取步骤实例详情数据")
    @GET
    @Path("/{projectId}/get_step_instance_detail")
    fun getStepInstanceDetail(
        @Parameter(name = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "作业实例ID")
        @QueryParam("jobInstanceId")
        jobInstanceId: Long,
        @Parameter(name = "步骤实例ID")
        @QueryParam("stepInstanceId")
        stepInstanceId: Long
    ): JobResult<GetStepInstanceDetailResult>

    @Operation(summary = "请求上云版job - 获取步骤实例中各主机的任务执行状态数据")
    @GET
    @Path("/{projectId}/get_step_instance_status")
    fun getStepInstanceStatus(
        @Parameter(name = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "作业实例ID", required = true)
        @QueryParam("jobInstanceId")
        jobInstanceId: Long,
        @Parameter(name = "步骤实例ID", required = true)
        @QueryParam("stepInstanceId")
        stepInstanceId: Long,
        @Parameter(name = "步骤重试次数")
        @QueryParam("executeCount")
        executeCount: Int?,
        @Parameter(name = "滚动批次")
        @QueryParam("batch")
        batch: Int?,
        @Parameter(name = "每个分组里的最大主机数量，不传则返回全量数据。")
        @QueryParam("maxHostNumPerGroup")
        maxHostNumPerGroup: Int?,
        @Parameter(name = "日志搜索关键字")
        @QueryParam("keyword")
        keyword: String?,
        @Parameter(name = "主机IP/IPv6搜索关键字")
        @QueryParam("searchIp")
        searchIp: String?,
        @Parameter(name = "执行状态")
        @QueryParam("status")
        status: Int?,
        @Parameter(name = "结果标签")
        @QueryParam("tag")
        tag: String?
    ): JobResult<GetStepInstanceStatusResult>

    @Operation(summary = "操作项目灰度状态的OP接口")
    @POST
    @Path("/operate_op_project")
    fun operateOpProject(
        @Parameter(name = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(name = "op操作请求信息", required = true)
        opOperateReq: OpOperateReq
    ): OpOperateResult

    @Operation(summary = "批量写入display_name的接口")
    @POST
    @Path("/stock_data_update/write_display_name")
    fun writeDisplayName(
        @Parameter(name = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE
    )

    @Operation(summary = "蓝盾agent状态版本更新接口")
    @POST
    @Path("/stock_data_update/update_devops_agent")
    fun updateDevopsAgent(
        @Parameter(name = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE
    )

    @Operation(summary = "部署节点cc状态轮询接口")
    @POST
    @Path("/stock_data_update/check_deploy_nodes_in_cc")
    fun checkDeployNodesInCC(
        @Parameter(name = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE
    )
}
