package com.tencent.devops.turbo.api

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.util.constants.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.turbo.vo.TurboPlanDetailVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

@Api(tags = ["BUILD_TURBO_PLAN"], description = "编译加速方案插件接口")
@RequestMapping("/build/turboPlan")
@FeignClient(name = "turbo", contextId = "IBuildTurboPlanController")
interface IBuildTurboPlanController {

    @ApiOperation("根据流水线位置寻找编译加速方案id")
    @GetMapping(
        "/pipelineId/{pipelineId}/pipelineElementId/{pipelineElementId}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun findTurboPlanIdByProjectIdAndPipelineInfo(
        @ApiParam(value = "蓝盾项目id", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam(value = "流水线id", required = true)
        @PathVariable("pipelineId")
        pipelineId: String,
        @ApiParam(value = "流水线原子id", required = true)
        @PathVariable("pipelineElementId")
        pipelineElementId: String
    ): Response<String?>

    @ApiOperation("通过编译加速方案id查询详细值")
    @GetMapping(
        "/{turboPlanId}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun findTurboPlanDetailById(
        @ApiParam(value = "编译加速方案id", required = true)
        @PathVariable("turboPlanId")
        turboPlanId: String
    ): Response<TurboPlanDetailVO>
}
