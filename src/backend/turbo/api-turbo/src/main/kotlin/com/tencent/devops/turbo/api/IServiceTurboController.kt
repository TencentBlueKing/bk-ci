package com.tencent.devops.turbo.api

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.turbo.pojo.TurboRecordModel
import com.tencent.devops.turbo.validate.TurboRecordGroup
import com.tencent.devops.turbo.vo.TurboPlanDetailVO
import com.tencent.devops.turbo.vo.TurboPlanStatRowVO
import com.tencent.devops.turbo.vo.TurboRecordHistoryVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@Api(tags = ["SERVICE_TURBO"], description = "编译加速service接口")
@RequestMapping("/service/turbo")
@FeignClient(name = "turbo", contextId = "IServiceTurboController")
@ServiceInterface("turbo-new")
interface IServiceTurboController {

    @ApiOperation("获取方案列表")
    @GetMapping(
            "/projectId/{projectId}/{userId}/turboPlan/list",
            produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getTurboPlanByProjectIdAndCreatedDate(
        @ApiParam(value = "项目id", required = true)
        @PathVariable("projectId")
        projectId: String,
        @ApiParam(value = "开始时间", required = false)
        @RequestParam("startTime")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        startTime: LocalDate?,
        @ApiParam(value = "结束时间", required = false)
        @RequestParam("endTime")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        endTime: LocalDate?,
        @ApiParam(value = "页数", required = false)
        @RequestParam(value = "pageNum")
        pageNum: Int?,
        @ApiParam(value = "每页多少条", required = false)
        @RequestParam("pageSize")
        pageSize: Int?,
        @ApiParam(value = "用户信息", required = true)
        @PathVariable("userId")
        userId: String
    ): Response<Page<TurboPlanStatRowVO>>

    @ApiOperation("获取加速历史列表")
    @PostMapping(
            "/projectId/{projectId}/{userId}/history/list",
            consumes = [MediaType.APPLICATION_JSON_VALUE],
            produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getTurboRecordHistoryList(
        @ApiParam(value = "页数", required = false)
        @RequestParam(value = "pageNum")
        pageNum: Int?,
        @ApiParam(value = "每页多少条", required = false)
        @RequestParam("pageSize")
        pageSize: Int?,
        @ApiParam(value = "排序字段", required = false)
        @RequestParam("sortField")
        sortField: String?,
        @ApiParam(value = "排序类型", required = false)
        @RequestParam("sortType")
        sortType: String?,
        @ApiParam(value = "编译加速历史请求数据信息", required = true)
        @RequestBody
        @Validated(TurboRecordGroup.OpenApi::class)
        turboRecordModel: TurboRecordModel,
        @ApiParam(value = "蓝盾项目id", required = true)
        @PathVariable("projectId")
        projectId: String,
        @ApiParam(value = "用户信息", required = true)
        @PathVariable("userId")
        userId: String

    ): Response<Page<TurboRecordHistoryVO>>

    @ApiOperation("获取加速方案详情")
    @GetMapping(
            "/projectId/{projectId}/{userId}/turboPlan/detail/planId/{planId}",
            produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getTurboPlanDetailByPlanId(
        @ApiParam(value = "方案id", required = true)
        @PathVariable("planId")
        planId: String,
        @ApiParam(value = "蓝盾项目id", required = true)
        @PathVariable("projectId")
        projectId: String,
        @ApiParam(value = "用户信息", required = true)
        @PathVariable("userId")
        userId: String
    ): Response<TurboPlanDetailVO>
}
