package com.tencent.bkrepo.helm.controller

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.helm.pojo.fixtool.DateTimeRepairResponse
import com.tencent.bkrepo.helm.pojo.fixtool.PackageManagerResponse
import com.tencent.bkrepo.helm.service.FixToolService
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HelmFixToolController(
    private val fixToolService: FixToolService
) {
    @ApiOperation("修复package管理功能")
    @GetMapping("/ext/package/populate")
    fun fixPackageVersion(): List<PackageManagerResponse> {
        return fixToolService.fixPackageVersion()
    }

    @ApiOperation("修复index.yaml文件中的制品包创建时间问题")
    @GetMapping("/ext/repairDateFormat")
    fun repairPackageCreatedDate(): Response<List<DateTimeRepairResponse>> {
        return ResponseBuilder.success(fixToolService.repairPackageCreatedDate())
    }
}
