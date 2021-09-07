package com.tencent.devops.turbo.api

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.util.constants.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.turbo.pojo.TurboPlanModel
import com.tencent.devops.turbo.validate.TurboPlanGroup
import com.tencent.devops.turbo.vo.TurboPlanDetailVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Api(tags = ["OP_TURBO_PLAN"], description = "编译加速方案配置op接口")
@RequestMapping("/op/turboPlan")
@FeignClient(name = "turbo", contextId = "IOpTurboPlanController")
interface IOpTurboPlanController {

    @ApiOperation("更新编译加速方案信息")
    @PutMapping(
        "/planId/{planId}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun updateTurboPlan(
        @ApiParam(value = "编译加速方案信息", required = true)
        @RequestBody
        @Validated(TurboPlanGroup.UpdateAll::class)
        turboPlanModel: TurboPlanModel,
        @ApiParam(value = "方案id", required = true)
        @PathVariable("planId")
        planId: String,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<Boolean>

    @ApiOperation("获取编译加速方案清单")
    @GetMapping(
        "/list",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getAllTurboPlanList(
        @ApiParam(value = "编译加速方案id", required = false)
        @RequestParam("turboPlanId")
        turboPlanId: String?,
        @ApiParam(value = "编译加速方案名", required = false)
        @RequestParam("planName")
        planName: String?,
        @ApiParam(value = "编译加速项目id", required = false)
        @RequestParam("projectId")
        projectId: String?,
        @ApiParam(value = "页数", required = false)
        @RequestParam("pageNum")
        pageNum: Int?,
        @ApiParam(value = "每页数量", required = false)
        @RequestParam("pageSize")
        pageSize: Int?
    ): Response<Page<TurboPlanDetailVO>>
}
