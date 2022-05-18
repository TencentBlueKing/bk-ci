package com.tencent.devops.turbo.api

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.util.constants.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.util.constants.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.turbo.pojo.TurboPlanModel
import com.tencent.devops.turbo.validate.TurboPlanGroup
import com.tencent.devops.turbo.vo.TurboMigratedPlanVO
import com.tencent.devops.turbo.vo.TurboPlanDetailVO
import com.tencent.devops.turbo.vo.TurboPlanPageVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Api(tags = ["TURBO_PLAN"], description = "编译加速方案接口")
@RequestMapping("/user/turboPlan")
@FeignClient(name = "turbo", contextId = "IUserTurboPlanController")
interface IUserTurboPlanController {

    @ApiOperation("加速方案-创建页,新增加速方案")
    @PostMapping(
        "",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun addNewTurboPlan(
        @ApiParam(value = "新增加速方案请求数据信息", required = true)
        @RequestBody
        @Validated(TurboPlanGroup.Create::class)
        turboPlanModel: TurboPlanModel,
        @ApiParam(value = "蓝盾项目id", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<String?>

    @ApiOperation("加速方案-详情页,获取方案详情信息")
    @GetMapping(
        "/planId/{planId}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getTurboPlanDetailByPlanId(
        @ApiParam(value = "方案id", required = true)
        @PathVariable("planId")
        planId: String,
        @ApiParam(value = "蓝盾项目id", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<TurboPlanDetailVO>

    @ApiOperation("加速方案-列表页,获取方案清单数据")
    @GetMapping(
        "/detail/projectId/{projectId}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getTurboPlanStatRowData(
        @ApiParam(value = "项目id", required = true)
        @PathVariable("projectId")
        projectId: String,
        @ApiParam(value = "页数", required = true)
        @RequestParam(value = "pageNum")
        pageNum: Int?,
        @ApiParam(value = "每页多少条", required = true)
        @RequestParam("pageSize")
        pageSize: Int?,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<TurboPlanPageVO>

    @ApiOperation("加速方案-详情页,编辑方案名称及开启状态")
    @PutMapping(
        "/name/planId/{planId}",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun putTurboPlanDetailNameAndOpenStatus(
        @ApiParam(value = "编辑方案名称和开启状态请求数据信息", required = true)
        @RequestBody
        @Validated(TurboPlanGroup.UpdateDetail::class)
        turboPlanModel: TurboPlanModel,
        @ApiParam(value = "方案id", required = true)
        @PathVariable("planId")
        planId: String,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String,
        @ApiParam(value = "蓝盾项目id", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String
    ): Response<Boolean>

    @ApiOperation("加速方案-详情页,编辑配置参数值")
    @PutMapping(
        "/configParam/planId/{planId}",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun putTurboPlanConfigParam(
        @ApiParam(value = "编辑配置参数值请求数据信息", required = true)
        @RequestBody
        @Validated(TurboPlanGroup.UpdateParam::class)
        turboPlanModel: TurboPlanModel,
        @ApiParam(value = "方案id", required = true)
        @PathVariable("planId")
        planId: String,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String,
        @ApiParam(value = "蓝盾项目id", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String
    ): Response<Boolean>

    @ApiOperation("加速方案-列表页,编辑方案清单置顶状态")
    @PutMapping(
        "/topStatus/planId/{planId}/topStatus/{topStatus}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun putTurboPlanTopStatus(
        @ApiParam(value = "方案id", required = true)
        @PathVariable("planId")
        planId: String,
        @ApiParam(value = "置顶状态", required = true)
        @PathVariable("topStatus")
        topStatus: String,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<Boolean>

    @ApiOperation("获取有效编译加速方案清单")
    @GetMapping(
        "/list/projectId/{projectId}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getAvailableTurboPlanList(
        @ApiParam(value = "项目id", required = true)
        @PathVariable("projectId")
        projectId: String,
        @ApiParam(value = "页数", required = true)
        @RequestParam(value = "pageNum")
        pageNum: Int?,
        @ApiParam(value = "每页多少条", required = true)
        @RequestParam("pageSize")
        pageSize: Int?
    ): Response<Page<TurboPlanDetailVO>>

    @ApiOperation("根据流水线位置寻找编译加速方案id")
    @GetMapping(
        "/projectId/{projectId}/pipelineId/{pipelineId}/pipelineElementId/{pipelineElementId}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun findTurboPlanIdByProjectIdAndPipelineInfo(
        @ApiParam(value = "蓝盾项目id", required = true)
        @PathVariable("projectId")
        projectId: String,
        @ApiParam(value = "流水线id", required = true)
        @PathVariable("pipelineId")
        pipelineId: String,
        @ApiParam(value = "流水线原子id", required = true)
        @PathVariable("pipelineElementId")
        pipelineElementId: String
    ): Response<TurboMigratedPlanVO?>
}
