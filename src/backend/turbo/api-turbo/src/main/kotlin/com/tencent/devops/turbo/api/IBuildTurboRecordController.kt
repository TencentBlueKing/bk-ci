package com.tencent.devops.turbo.api

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.util.constants.AUTH_HEADER_DEVOPS_USER_ID
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

@Api(tags = ["BUILD_TURBO_RECORD_INSTANCE"], description = "编译加速记录插件接口")
@RequestMapping("/build/turboRecord")
@FeignClient(name = "turbo", contextId = "IBuildTurboRecordController")
interface IBuildTurboRecordController {

    @ApiOperation("插件扫描完成更新记录状态")
    @PutMapping(
        "/buildId/{buildId}",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun updateRecordStatusForPlugin(
        @ApiParam(value = "编译加速构建id", required = true)
        @PathVariable("buildId")
        buildId: String,
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String
    ): Response<String?>
}
