package com.tencent.devops.turbo.api

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.util.constants.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.turbo.vo.TurboOverviewStatRowVO
import com.tencent.devops.turbo.vo.TurboOverviewTrendVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

@Api(tags = ["TURBO_DAY_SUMMARY"], description = "编译加速总览页面接口")
@RequestMapping("/user/turboDaySummary")
@FeignClient(name = "turbo", contextId = "IUserTurboDaySummaryController")
interface IUserTurboDaySummaryController {

    @ApiOperation("获取总览页面统计栏数据")
    @GetMapping(
        "/statisticsRowData/{projectId}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getOverviewStatRowData(
        @ApiParam(value = "蓝盾项目id", required = true)
        @PathVariable("projectId")
        projectId: String,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<TurboOverviewStatRowVO>

    @ApiOperation("获取总览页面耗时分布趋势图数据")
    @GetMapping(
        "/timeConsumingTrend/dateType/{dateType}/projectId/{projectId}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getTimeConsumingTrendData(
        @ApiParam(value = "日期类型", required = true)
        @PathVariable("dateType")
        dateType: String,
        @ApiParam(value = "项目id", required = true)
        @PathVariable("projectId")
        projectId: String,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<List<TurboOverviewTrendVO>>

    @ApiOperation("获取总览页面编译次数趋势图数据")
    @GetMapping(
        "/compileNumber/dateType/{dateType}/projectId/{projectId}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getCompileNumberTrendData(
        @ApiParam(value = "日期类型", required = true)
        @PathVariable("dateType")
        dateType: String,
        @ApiParam(value = "项目id", required = true)
        @PathVariable("projectId")
        projectId: String,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<List<TurboOverviewTrendVO>>

    @ApiOperation("获取总览页格场景的加速次数趋势图数据")
    @GetMapping(
        "/executeCount/dateType/{dateType}/projectId/{projectId}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getExecuteCountTrendData(
        @ApiParam(value = "日期类型", required = true)
        @PathVariable("dateType")
        dateType: String,
        @ApiParam(value = "项目id", required = true)
        @PathVariable("projectId")
        projectId: String,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<List<TurboOverviewTrendVO>>
}
