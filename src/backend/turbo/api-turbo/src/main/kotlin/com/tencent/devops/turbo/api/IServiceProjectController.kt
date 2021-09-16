package com.tencent.devops.turbo.api

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.turbo.vo.ProjectVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Api(tags = ["SERVICE_PROJECT"], description = "turbo服务配置project服务依赖接口")
@RequestMapping("/service/projects")
@FeignClient(name = "project", contextId = "IServiceProjectController", path = "/api")
interface IServiceProjectController {

    @ApiOperation("查询指定EN项目")
    @GetMapping(
        "/{projectId}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun get(
        @ApiParam("项目ID", required = true)
        @PathVariable("projectId")
        englishName: String
    ): Response<ProjectVO?>
}
