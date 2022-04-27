package com.tencent.devops.turbo.api

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.util.constants.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.util.constants.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.turbo.pojo.TurboRecordModel
import com.tencent.devops.turbo.vo.TurboListSelectVO
import com.tencent.devops.turbo.vo.TurboRecordDisplayVO
import com.tencent.devops.turbo.vo.TurboRecordHistoryVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Api(tags = ["TURBO_RECORD"], description = "编译加速记录接口")
@RequestMapping("/user/turboRecord")
@FeignClient(name = "turbo", contextId = "IUserTurboRecordController")
interface IUserTurboRecordController {
    @ApiOperation("获取加速历史列表")
    @PostMapping(
        "/list",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getTurboRecordHistoryList(
        @ApiParam(value = "页数", required = true)
        @RequestParam(value = "pageNum")
        pageNum: Int?,
        @ApiParam(value = "每页多少条", required = true)
        @RequestParam("pageSize")
        pageSize: Int?,
        @ApiParam(value = "排序字段", required = true)
        @RequestParam("sortField")
        sortField: String?,
        @ApiParam(value = "排序类型", required = true)
        @RequestParam("sortType")
        sortType: String?,

        @ApiParam(value = "编译加速历史请求数据信息", required = true)
        @RequestBody
        turboRecordModel: TurboRecordModel,
        @ApiParam(value = "蓝盾项目id", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<Page<TurboRecordHistoryVO>>

    @ApiOperation("加速历史,获取搜索条件数据")
    @GetMapping(
        "/detail/projectId/{projectId}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getPipelineAndPlanAndStatusList(
        @ApiParam(value = "项目id", required = true)
        @PathVariable("projectId")
        projectId: String
    ): Response<TurboListSelectVO>

    @ApiOperation("获取编译加速记录统计数据")
    @GetMapping(
        "/stats/id/{turboRecordId}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getTurboRecordStatInfo(
        @ApiParam(value = "编译加速记录id", required = true)
        @PathVariable("turboRecordId")
        turboRecordId: String
    ): Response<String?>

    @ApiOperation("获取编译加速记录信息")
    @GetMapping(
        "/id/{turboRecordId}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getTurboDisplayInfoById(
        @ApiParam(value = "编译加速记录id", required = true)
        @PathVariable("turboRecordId")
        turboRecordId: String,
        @ApiParam(value = "蓝盾项目id", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<TurboRecordDisplayVO>
}
