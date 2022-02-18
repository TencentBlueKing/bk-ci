package com.tencent.devops.turbo.api

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.util.constants.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.turbo.pojo.TurboPlanInstanceModel
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

@Api(tags = ["BUILD_TURBO_PLAN_INSTANCE"], description = "编译加速实例插件接口")
@RequestMapping("/build/turboPlanInstance")
@FeignClient(name = "turbo", contextId = "IBuildTurboPlanInstanceController")
interface IBuildTurboPlanInstanceController {

    @ApiOperation("插入更新编译加速方案实例数据")
    @PostMapping(
        "",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun upsertTurboPlanInstanceByPipelineInfo(
        @ApiParam(value = "编译加速方案实例入参", required = true)
        @RequestBody
        @Validated
        turboPlanInstanceModel: TurboPlanInstanceModel,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<String>
}
