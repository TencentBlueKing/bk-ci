package com.tencent.devops.turbo.api

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.turbo.vo.TurboPlanInstanceVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Api(tags = ["TURBO_PLAN_INSTANCE"], description = "编译加速方案-列表页接口")
@RequestMapping("/user/planInstance")
@FeignClient(name = "turbo", contextId = "IUserTurboPlanInstanceController")
interface IUserTurboPlanInstanceController {
    @ApiOperation("加速方案-列表页,获取加速方案数据")
    @GetMapping(
        "/detail/turboPlanId/{turboPlanId}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getTurboPlanInstanceList(
        @ApiParam(value = "编译加速方案id", required = true)
        @PathVariable("turboPlanId")
        turboPlanId: String,
        @ApiParam(value = "页数", required = true)
        @RequestParam("pageNum")
        pageNum: Int?,
        @ApiParam(value = "每页多少条", required = true)
        @RequestParam("pageSize")
        pageSize: Int?,
        @ApiParam(value = "排序字段", required = true)
        @RequestParam("sortField")
        sortField: String?,
        @ApiParam(value = "排序类型", required = true)
        @RequestParam("sortType")
        sortType: String?
    ): Response<Page<TurboPlanInstanceVO>>
}
