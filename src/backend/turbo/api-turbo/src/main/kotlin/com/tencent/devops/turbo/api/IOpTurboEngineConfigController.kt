package com.tencent.devops.turbo.api

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.util.constants.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.turbo.pojo.ParamEnumModel
import com.tencent.devops.turbo.pojo.ParamEnumSimpleModel
import com.tencent.devops.turbo.pojo.TurboEngineConfigModel
import com.tencent.devops.turbo.pojo.TurboEngineConfigPriorityModel
import com.tencent.devops.turbo.validate.TurboEngineConfigGroup
import com.tencent.devops.turbo.vo.TurboEngineConfigVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

@Api(tags = ["OP_TURBO_ENGINE_CONFIG"], description = "编译加速模式配置op接口")
@RequestMapping("/op/turboEngineConfig")
@FeignClient(name = "turbo", contextId = "IOpTurboEngineConfigController")
interface IOpTurboEngineConfigController {

    @ApiOperation("新增编译加速模式")
    @PostMapping(
        "",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun addNewEngineConfig(
        @ApiParam(value = "编译加速模式信息", required = true)
        @RequestBody
        @Validated(TurboEngineConfigGroup.Create::class)
        turboEngineConfigModel: TurboEngineConfigModel,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<Long?>

    @ApiOperation("删除编译加速模式")
    @DeleteMapping(
        "/engineCode/{engineCode}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun deleteEngineConfig(
        @ApiParam(value = "编译加速模式引擎代码", required = true)
        @PathVariable("engineCode")
        engineCode: String,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<Boolean>

    @ApiOperation("更新编译加速模式状态，true为启用，false为禁用")
    @PutMapping(
        "/engineCode/{engineCode}/status/{status}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun updateEngineConfig(
        @ApiParam(value = "编译加速模式引擎代码", required = true)
        @PathVariable("engineCode")
        engineCode: String,
        @ApiParam(value = "编译加速模式状态", required = true)
        @PathVariable("status")
        status: Boolean,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<Boolean>

    @ApiOperation("获取加速模式清单")
    @GetMapping(
        "/all",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getAllEngineConfigList(): Response<List<TurboEngineConfigVO>>

    @ApiOperation("获取特定编译加速模式")
    @GetMapping(
        "/engineCode/{engineCode}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getEngineConfigByEngineCode(
        @ApiParam(value = "编译加速引擎代码", required = true)
        @PathVariable("engineCode")
        engineCode: String
    ): Response<TurboEngineConfigVO>

    @ApiOperation("更新特定编译加速模式")
    @PutMapping(
        "/engineCode/{engineCode}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun updateEngineConfig(
        @ApiParam(value = "编译加速引擎代码", required = true)
        @PathVariable("engineCode")
        engineCode: String,
        @ApiParam(value = "编译加速模式信息", required = true)
        @RequestBody
        @Validated(TurboEngineConfigGroup.Update::class)
        turboEngineConfigModel: TurboEngineConfigModel,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<Boolean>

    @ApiOperation("更新编译加速模式优先级")
    @PutMapping(
        "/priority",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun updateEngineConfigPriority(
        @ApiParam(value = "编译加速模式信息", required = true)
        @RequestBody
        @Validated
        turboPriorityList: List<TurboEngineConfigPriorityModel>,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<Boolean>

    @ApiOperation("更新编译加速模式worker信息")
    @PostMapping(
        "/workVersion/engineCode/{engineCode}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun addWorkVersion(
        @ApiParam(value = "编译加速引擎代码", required = true)
        @PathVariable("engineCode")
        engineCode: String,
        @ApiParam(value = "参数枚举值详细信息", required = true)
        @RequestBody
        paramEnum: ParamEnumModel
    ): Response<Boolean>

    @ApiOperation("更新编译加速模式worker信息")
    @DeleteMapping(
        "/workVersion/engineCode/{engineCode}/paramValue/{paramValue}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun deleteWorkVersion(
        @ApiParam(value = "编译加速引擎代码", required = true)
        @PathVariable("engineCode")
        engineCode: String,
        @ApiParam(value = "参数key值", required = true)
        @PathVariable("paramValue")
        paramValue: String
    ): Response<Boolean>

    @ApiOperation("更新编译加速模式worker信息")
    @PutMapping(
        "/workVersion/engineCode/{engineCode}/paramValue/{paramValue}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun updateWorkVersion(
        @ApiParam(value = "编译加速引擎代码", required = true)
        @PathVariable("engineCode")
        engineCode: String,
        @ApiParam(value = "参数key值", required = true)
        @PathVariable("paramValue")
        paramValue: String,
        @ApiParam(value = "参数枚举值详细信息", required = true)
        @RequestBody
        paramEnum: ParamEnumSimpleModel
    ): Response<Boolean>
}
