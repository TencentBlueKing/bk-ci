package com.tencent.bk.codecc.apiquery.api

import com.tencent.bk.codecc.apiquery.task.TaskQueryReq
import com.tencent.bk.codecc.apiquery.task.model.BuildIdRelationshipModel
import com.tencent.bk.codecc.apiquery.task.model.CustomProjModel
import com.tencent.bk.codecc.apiquery.task.model.TaskFailRecordModel
import com.tencent.bk.codecc.apiquery.task.model.TaskInfoModel
import com.tencent.bk.codecc.apiquery.task.model.ToolConfigInfoModel
import com.tencent.bk.codecc.apiquery.vo.pipeline.PipelineTaskVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["TASK"], description = "告警信息查询")
@Path("/{apigw:apigw-user|apigw-app|apigw}/v2/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwTaskResource {

    @ApiOperation("获取任务详情信息")
    @Path("/detail")
    @POST
    fun getTaskDetailList(
        @ApiParam(value = "任务查询请求体", required = true)
        taskQueryReq: TaskQueryReq,
        @ApiParam(value = "应用code", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String,
        @ApiParam(value = "页面数", required = false)
        @QueryParam("pageNum")
        pageNum: Int?,
        @ApiParam(value = "页面大小", required = false)
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam(value = "排序字段", required = false)
        @QueryParam("sortField")
        sortField: String?,
        @ApiParam(value = "排序类型", required = false)
        @QueryParam("sortType")
        sortType: String?
    ): Result<Page<TaskInfoModel>>

    @ApiOperation("获取任务详情信息")
    @Path("/detail/projectId/{projectId}")
    @GET
    fun getTaskDetailListByProjectId(
        @ApiParam(value = "项目id", required = false)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "应用code", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String,
        @ApiParam(value = "页面数", required = false)
        @QueryParam("pageNum")
        pageNum: Int?,
        @ApiParam(value = "页面大小", required = false)
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam(value = "排序字段", required = false)
        @QueryParam("sortField")
        sortField: String?,
        @ApiParam(value = "排序类型", required = false)
        @QueryParam("sortType")
        sortType: String?
    ): Result<Page<TaskInfoModel>>

    @ApiOperation("获取个性化触发详情信息")
    @Path("/custom")
    @POST
    fun findCustomProjByTaskIds(
        @ApiParam(value = "任务查询请求体", required = true)
        taskQueryReq: TaskQueryReq,
        @ApiParam(value = "应用code", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String,
        @ApiParam(value = "页面数", required = false)
        @QueryParam("pageNum")
        pageNum: Int?,
        @ApiParam(value = "页面大小", required = false)
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam(value = "排序字段", required = false)
        @QueryParam("sortField")
        sortField: String?,
        @ApiParam(value = "排序类型", required = false)
        @QueryParam("sortType")
        sortType: String?
    ): Result<Page<CustomProjModel>>

    @ApiOperation("获取工具信息")
    @Path("/tool")
    @POST
    fun findToolNameByTaskIds(
        @ApiParam(value = "任务查询请求体", required = true)
        taskQueryReq: TaskQueryReq,
        @ApiParam(value = "应用code", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String,
        @ApiParam(value = "页面数", required = false)
        @QueryParam("pageNum")
        pageNum: Int?,
        @ApiParam(value = "页面大小", required = false)
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam(value = "排序字段", required = false)
        @QueryParam("sortField")
        sortField: String?,
        @ApiParam(value = "排序类型", required = false)
        @QueryParam("sortType")
        sortType: String?
    ): Result<Page<ToolConfigInfoModel>>

    @ApiOperation("根据流水线ID获取任务有效信息")
    @Path("/by/pipelines")
    @POST
    fun getPipelineTask(
        @ApiParam(value = "任务查询请求体", required = true)
        taskQueryReq: TaskQueryReq,
        @ApiParam(value = "应用code", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String,
        @ApiParam(value = "页面数")
        @QueryParam("pageNum")
        pageNum: Int?,
        @ApiParam(value = "页面大小")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam(value = "排序字段")
        @QueryParam("sortField")
        sortField: String?,
        @ApiParam(value = "排序类型")
        @QueryParam("sortType")
        sortType: String?
    ): Result<Page<PipelineTaskVO>>

    @ApiOperation("查询任务失败信息")
    @Path("/fail/list")
    @POST
    fun findTaskFailRecord(
        @ApiParam(value = "任务查询请求体", required = true)
        taskQueryReq: TaskQueryReq,
        @ApiParam(value = "应用code", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String,
        @ApiParam(value = "页面数")
        @QueryParam("pageNum")
        pageNum: Int?,
        @ApiParam(value = "页面大小")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam(value = "排序字段")
        @QueryParam("sortField")
        sortField: String?,
        @ApiParam(value = "排序类型")
        @QueryParam("sortType")
        sortType: String?
    ): Result<List<TaskFailRecordModel>>

    @ApiOperation("")
    @Path("/buildId")
    @POST
    fun getBuildIdRelationship(
        @ApiParam(value = "任务查询请求体", required = true)
        taskQueryReq: TaskQueryReq,
        @ApiParam(value = "应用code", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String
    ): Result<BuildIdRelationshipModel?>

    @ApiOperation("通过工具名查询任务清单")
    @Path("/tasks/toolName/{toolName}")
    @GET
    fun getTaskListByToolName(
        @ApiParam(value = "工具名")
        @PathParam("toolName")
        toolName: String,
        @ApiParam(value = "应用code", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String,
        @ApiParam(value = "页面数")
        @QueryParam("pageNum")
        pageNum: Int?,
        @ApiParam(value = "页面大小")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam(value = "排序字段")
        @QueryParam("sortField")
        sortField: String?,
        @ApiParam(value = "排序类型")
        @QueryParam("sortType")
        sortType: String?
    ): Result<Page<Long>>
}
