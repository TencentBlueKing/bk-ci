package com.tencent.devops.turbo.api

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.util.constants.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.util.constants.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.turbo.pojo.ParamEnumModel
import com.tencent.devops.turbo.pojo.TurboEngineConfigModel
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
import org.springframework.web.bind.annotation.RequestParam

@Api(tags = ["TURBO_ENGINE_CONFIG"], description = "编译加速模式配置接口")
@RequestMapping("/user/turboEngineConfig")
@FeignClient(name = "turbo", contextId = "IUserTurboEngineConfigController")
interface IUserTurboEngineConfigController {

    @ApiOperation("加速方案-初始页,获取编译加速模式清单")
    @GetMapping(
        "/list/projectId/{projectId}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getEngineConfigList(
        @ApiParam(value = "项目id", required = true)
        @PathVariable("projectId")
        projectId: String
    ): Response<List<TurboEngineConfigVO>?>

    @ApiOperation("新增编译加速模式")
    @PostMapping("")
    fun addNewEngineConfig(
        @ApiParam(value = "编译加速模式信息", required = true)
        @RequestBody
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

    @ApiOperation("获取推荐的编译加速模式")
    @GetMapping(
        "/recommend/list",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getRecommendEngineConfig(): Response<List<TurboEngineConfigVO>>

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

    @ApiOperation("通过编译加速方案id获取编译加速模式")
    @GetMapping(
        "/engineInfo/planId/{planId}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getEngineInfoByPlanId(
        @ApiParam(value = "编译加速方案id", required = true)
        @PathVariable("planId")
        planId: String
    ): Response<TurboEngineConfigVO?>

    @ApiOperation("根据区域队列名获取对应的编译器版本清单")
    @GetMapping(
        "/{engineCode}/compilerVersions",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getCompilerVersionListByQueueName(
        @ApiParam(value = "引擎标识", required = true)
        @PathVariable("engineCode")
        engineCode: String,
        @ApiParam(value = "项目id", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam(value = "队列名称", required = false)
        @RequestParam("queueName")
        queueName: String?
    ): Response<List<ParamEnumModel>>
}
