package com.tencent.bk.codecc.openapi.v2

import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO
import com.tencent.bk.codecc.defect.vo.ToolClocRspVO
import com.tencent.bk.codecc.defect.vo.ToolDefectRspVO
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO
import com.tencent.bk.codecc.defect.vo.openapi.TaskOverviewDetailRspVO
import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldReq
import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldRsp
import com.tencent.bk.codecc.task.pojo.TriggerPipelineReq
import com.tencent.bk.codecc.task.pojo.TriggerPipelineRsp
import com.tencent.bk.codecc.task.vo.pipeline.PipelineTaskVO
import com.tencent.bk.codecc.task.vo.tianyi.QueryMyTasksReqVO
import com.tencent.bk.codecc.task.vo.tianyi.TaskInfoVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.data.domain.Sort
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_V2_DEFECT"], description = "OPEN-API-V2-告警查询")
@Path("/{apigw:apigw-user|apigw-app|apigw}/v2/defect")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwDefectResourceV2 {

    @ApiOperation("根据作者获取对应任务信息列表")
    @Path("/myTasks")
    @POST
    fun getTasksByAuthor(
        @ApiParam(value = "请求对象模型", required = true)
        reqVO: QueryMyTasksReqVO
    ): Result<Page<TaskInfoVO>>


    @ApiOperation("统计工具规则包各规则的告警情况")
    @Path("/list/taskId/{taskId}")
    @POST
    fun queryToolDefectList(
        @ApiParam(value = "任务ID", required = true)
        @PathParam(value="taskId")
        taskId: Long,
        @ApiParam(value = "文件查询请求视图", required = true)
        reqVO: DefectQueryReqVO,
        @ApiParam(value = "页数")
        @QueryParam(value = "pageNum")
        pageNum: Int? = null,
        @ApiParam(value = "每页多少条")
        @QueryParam(value = "pageSize")
        pageSize: Int? = null,
        @ApiParam(value = "排序字段")
        @QueryParam(value = "sortField")
        sortField: String? = null,
        @ApiParam(value = "排序类型")
        @QueryParam(value = "sortType")
        sortType: Sort.Direction? = null
    ) : Result<ToolDefectRspVO>


    @ApiOperation("查询代码行数情况")
    @Path("/codeLine/taskId/{taskId}")
    @GET
    fun queryCodeLineInfo(
        @ApiParam(value = "任务ID", required = true)
        @PathParam(value="taskId")
        taskId: Long,
        @ApiParam(value = "工具名称", required = false)
        @QueryParam(value="toolName")
        @DefaultValue("CLOC")
        toolName: String
    ): Result<ToolClocRspVO>


    @ApiOperation("按事业群ID获取部门ID集合")
    @Path("/org/bgId/{bgId}")
    @GET
    fun queryDeptIdByBgId(
        @ApiParam(value = "事业群ID", required = true)
        @PathParam(value = "bgId")
        bgId: Int
    ) : Result<Set<Int>>


    @ApiOperation("通过流水线ID获取任务信息")
    @Path("/pipelines/{pipelineId}")
    @GET
    fun getPipelineTask(
        @ApiParam(value = "流水线ID", required = true)
        @PathParam(value = "pipelineId")
        pipelineId: String,
        @ApiParam(value = "当前用户")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        user: String? = null
    ) : Result<PipelineTaskVO>


    @ApiOperation("通过流水线ID获取任务信息")
    @Path("/custom/pipeline")
    @POST
    fun triggerCustomPipeline(
        @ApiParam(value = "触发参数", required = true)
        triggerPipelineReq: TriggerPipelineOldReq,
        @ApiParam(value = "用户", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId : String
    ) : Result<TriggerPipelineOldRsp>


    @ApiOperation("通过流水线ID获取任务信息")
    @Path("/custom/pipeline/new")
    @POST
    fun triggerCustomPipelineNew(
        @ApiParam(value = "触发参数", required = true)
        triggerPipelineReq: TriggerPipelineReq,
        @ApiParam(value = "应用code", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String,
        @ApiParam(value = "用户", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId : String
    ) : Result<TriggerPipelineRsp>


    @ApiOperation("停止api触发流水线")
    @Path("/custom/pipeline/codeccBuildId/{codeccBuildId}")
    @DELETE
    fun stopRunningApiTask(
        @ApiParam(value = "codecc构建id", required = true)
        @PathParam(value = "codeccBuildId")
        codeccBuildId: String,
        @ApiParam(value = "应用code", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String,
        @ApiParam(value = "用户", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId : String
    ) : Result<Boolean>



    @ApiOperation("批量统计任务告警概览情况")
    @Path("/statistics/overview")
    @POST
    fun queryTaskOverview(
            @ApiParam(value = "按组织架构查询视图", required = true)
            reqVO: DeptTaskDefectReqVO,
            @ApiParam(value = "页数")
            @QueryParam(value = "pageNum")
            pageNum: Int? = null,
            @ApiParam(value = "每页多少条")
            @QueryParam(value = "pageSize")
            pageSize: Int? = null,
            @ApiParam(value = "排序类型")
            @QueryParam(value = "sortType")
            sortType: Sort.Direction? = null
    ) : Result<TaskOverviewDetailRspVO>


    @ApiOperation("批量获取个性化任务告警概览情况")
    @Path("/statistics/custom")
    @GET
    fun getCustomTaskList(
            @ApiParam(value = "流水线ID", required = true)
            @QueryParam(value = "customProjSource")
            customProjSource: String,
            @ApiParam(value = "页数")
            @QueryParam(value = "pageNum")
            pageNum: Int? = null,
            @ApiParam(value = "每页多少条")
            @QueryParam(value = "pageSize")
            pageSize: Int? = null,
            @ApiParam(value = "排序类型")
            @QueryParam(value = "sortType")
            sortType: Sort.Direction? = null
    ) : Result<TaskOverviewDetailRspVO>




    @ApiOperation("作者转换")
    @Path("/author/taskId/{taskId}/projectId/{projectId}")
    @PUT
    fun authorTransfer(
        @ApiParam(value = "api类型", required = true)
        @PathParam(value = "apigw")
        apigw : String,
        @ApiParam(value = "任务id", required = true)
        @PathParam(value = "taskId")
        taskId : Long,
        @ApiParam(value = "项目id", required = true)
        @PathParam(value = "projectId")
        projectId : String,
        @ApiParam(value = "appCode", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode : String,
        @ApiParam(value = "transferAuthorPairs", required = false)
        batchDefectProcessReqVO : BatchDefectProcessReqVO,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId : String
    ) : Result<Boolean>

}
