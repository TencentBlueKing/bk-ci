package com.tencent.bkrepo.repository.controller.user

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.service.packages.PackageRepairService
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserPackageRepairController(
    private val packageRepairService: PackageRepairService
) {

    @ApiOperation("修改历史版本")
    @GetMapping("/version/history/repair")
    fun repairHistoryVersion(): Response<Void> {
        packageRepairService.repairHistoryVersion()
        return ResponseBuilder.success()
    }
}
