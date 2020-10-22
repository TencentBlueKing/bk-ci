package com.tencent.bk.codecc.openapi.v2

import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO
import com.tencent.bk.codecc.defect.vo.openapi.TaskOverviewDetailRspVO
import com.tencent.bk.codecc.task.pojo.TriggerPipelineReq
import com.tencent.bk.codecc.task.pojo.TriggerPipelineRsp
import com.tencent.devops.common.api.auth.CODECC_AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.CODECC_AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.CodeCCResult
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.data.domain.Sort
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_V2_DEFECT"], description = "OPEN-API-V2-告警查询")
@Path("/{apigw:apigw-user|apigw-app|apigw}/v2/defect")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwOpenSoureceDefectResourceV2 {

    @ApiOperation("通过流水线ID获取任务信息")
    @Path("/custom/pipeline/new")
    @POST
    fun triggerCustomPipelineNew(
        @ApiParam(value = "触发参数", required = true)
        triggerPipelineReq: TriggerPipelineReq,
        @ApiParam(value = "应用code", required = true)
        @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String,
        @ApiParam(value = "用户", required = true)
        @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_USER_ID)
        userId : String
    ) : CodeCCResult<TriggerPipelineRsp>


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
    ) : CodeCCResult<TaskOverviewDetailRspVO>

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
    ) : CodeCCResult<TaskOverviewDetailRspVO>
}