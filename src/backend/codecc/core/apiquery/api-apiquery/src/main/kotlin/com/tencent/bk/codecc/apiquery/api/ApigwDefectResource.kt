package com.tencent.bk.codecc.apiquery.api

import com.tencent.bk.codecc.apiquery.defect.model.CheckerDetailModel
import com.tencent.bk.codecc.apiquery.defect.model.CommonModel
import com.tencent.bk.codecc.apiquery.defect.model.StatisticModel
import com.tencent.bk.codecc.apiquery.task.TaskQueryReq
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO
import com.tencent.bk.codecc.apiquery.vo.openapi.CheckerDefectStatVO
import com.tencent.bk.codecc.apiquery.vo.openapi.TaskOverviewDetailRspVO
import com.tencent.devops.common.api.auth.CODECC_AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.CodeCCResult
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
        @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_APP_CODE)
        appCode : String,
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
    ): CodeCCResult<Page<CommonModel>>


    @ApiOperation("获取告警统计信息")
    @Path("/statistic")
    @POST
    fun getDefectStatisticList(
        @ApiParam(value = "任务查询请求体", required = true)
        taskQueryReq: TaskQueryReq,
        @ApiParam(value = "应用code", required = false)
        @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_APP_CODE)
        appCode : String,
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
    ): CodeCCResult<Page<StatisticModel>>


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
    ): CodeCCResult<TaskOverviewDetailRspVO>


    @ApiOperation("按规则维度批量统计告警数")
    @Path("/statistics/by/checker")
    @POST
    fun getDefectStatByChecker(
            @ApiParam(value = "查询参数详情", required = true)
            taskToolInfoReqVO: TaskToolInfoReqVO,
            @ApiParam(value = "应用code", required = false)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_APP_CODE)
            appCode : String,
            @ApiParam(value = "页数") @QueryParam(value = "pageNum")
            pageNum: Int?,
            @ApiParam(value = "每页数量") @QueryParam(value = "pageSize")
            pageSize: Int?
    ): CodeCCResult<Page<CheckerDefectStatVO>>

    @ApiOperation("所有语言全量/简化规则集的规则列表")
    @Path("/checkers/checkerSetType/{checkerSetType}")
    @GET
    fun queryChecker(
        @ApiParam(value = "规则集类型", required = true)
        @PathParam(value = "checkerSetType")
        checkerSetType: String
    ) : CodeCCResult<List<CheckerDetailModel>>

}