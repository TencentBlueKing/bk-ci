package com.tencent.bk.codecc.apiquery.api

import com.tencent.bk.codecc.apiquery.defect.model.CheckerDetailModel
import com.tencent.bk.codecc.apiquery.defect.model.CodeRepoFromAnalyzeLogModel
import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetModel
import com.tencent.bk.codecc.apiquery.defect.model.CommonModel
import com.tencent.bk.codecc.apiquery.defect.model.StatisticModel
import com.tencent.bk.codecc.apiquery.task.TaskQueryReq
import com.tencent.bk.codecc.apiquery.task.model.CheckerSetQueryReq
import com.tencent.bk.codecc.apiquery.vo.DefectQueryReqVO
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO
import com.tencent.bk.codecc.apiquery.vo.ToolDefectRspVO
import com.tencent.bk.codecc.apiquery.vo.openapi.CheckerDefectStatVO
import com.tencent.bk.codecc.apiquery.vo.openapi.TaskOverviewDetailRspVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["DEFECT"], description = "告警信息查询")
@Path("/{apigw:apigw-user|apigw-app|apigw}/v2/defect")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwDefectResource {

    @ApiOperation("获取告警详细信息")
    @Path("/detail")
    @POST
    fun getDefectDetailList(
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
    ): Result<Page<CommonModel>>

    @ApiOperation("获取告警统计信息")
    @Path("/statistic")
    @POST
    fun getDefectStatisticList(
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
    ): Result<Page<StatisticModel>>

    @ApiOperation("批量统计任务告警概览情况")
    @Path("/statistics/overview")
    @POST
    fun queryTaskOverview(
        @ApiParam(value = "查询参数详情", required = true)
        taskToolInfoReqVO: TaskToolInfoReqVO,
        @ApiParam(value = "页数") @QueryParam(value = "pageNum")
        pageNum: Int?,
        @ApiParam(value = "每页数量") @QueryParam(value = "pageSize")
        pageSize: Int?,
        @ApiParam(value = "排序类型") @QueryParam(value = "sortType")
        sortType: String?
    ): Result<TaskOverviewDetailRspVO>

    @ApiOperation("按规则维度批量统计告警数")
    @Path("/statistics/by/checker")
    @POST
    fun getDefectStatByChecker(
        @ApiParam(value = "查询参数详情", required = true)
        taskToolInfoReqVO: TaskToolInfoReqVO,
        @ApiParam(value = "应用code", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String,
        @ApiParam(value = "页数") @QueryParam(value = "pageNum")
        pageNum: Int?,
        @ApiParam(value = "每页数量") @QueryParam(value = "pageSize")
        pageSize: Int?
    ): Result<Page<CheckerDefectStatVO>>

    @ApiOperation("所有语言全量/简化规则集的规则列表")
    @Path("/checkers/checkerSetType/{checkerSetType}")
    @GET
    fun queryChecker(
        @ApiParam(value = "规则集类型", required = true)
        @PathParam(value = "checkerSetType")
        checkerSetType: String
    ): Result<List<CheckerDetailModel>>

    @ApiOperation("规则列表")
    @Path("/checkers/list")
    @POST
    fun listCheckerDetail(
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String,
        @QueryParam("pageNum")
        pageNum: Int?,
        @QueryParam("pageSize")
        pageSize: Int?,
        @QueryParam("toolName")
        toolName: String?,
        checkerKey: Set<String>?
    ): Result<List<CheckerDetailModel>>

    @ApiOperation("根据规则集id和版本查询规则清单")
    @Path("/checkers/checkerSetId/{checkerSetId}")
    @GET
    fun queryCheckerByCheckerId(
        @ApiParam(value = "规则集id", required = true)
        @PathParam(value = "checkerSetId")
        checkerSetId: String,
        @ApiParam(value = "规则集版本", required = false)
        @QueryParam("version")
        version: Int?
    ): Result<List<CheckerDetailModel>>

    @ApiOperation("查询工具告警清单")
    @Path("/list/taskId/{taskId}")
    @POST
    fun queryToolDefectList(
        @ApiParam(value = "任务ID", required = true) @PathParam(value = "taskId") taskId: Long,
        @ApiParam(value = "应用code", required = false) @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE) appCode: String,
        @ApiParam(value = "查询参数详情", required = true) @Valid defectQueryReqVO: DefectQueryReqVO,
        @ApiParam(value = "页数") @QueryParam(value = "pageNum") pageNum: Int?,
        @ApiParam(value = "页面大小") @QueryParam(value = "pageSize") pageSize: Int?,
        @ApiParam(value = "排序字段") @QueryParam(value = "sortField") sortField: String?,
        @ApiParam(value = "排序方式") @QueryParam(value = "sortType") sortType: String?
    ): Result<ToolDefectRspVO>

    @ApiOperation("通过规则集id查询最新版本规则集信息")
    @Path("/checkerSet/latest")
    @POST
    fun getLastestCheckerSetByIds(
        @ApiParam(value = "规则集id清单", required = true)
        checkerSetQueryReq: CheckerSetQueryReq
    ) : Result<List<CheckerSetModel>>

    @ApiOperation("根据任务id清单查询代码库信息清单")
    @Path("/codeRepo/list")
    @POST
    fun getCodeRepoListByTaskIdList(
        @ApiParam(value = "任务id清单入参", required = true)
        taskQueryReq: TaskQueryReq) : Result<List<CodeRepoFromAnalyzeLogModel>>

}
