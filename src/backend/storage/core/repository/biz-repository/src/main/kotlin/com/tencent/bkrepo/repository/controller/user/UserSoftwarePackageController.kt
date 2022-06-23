package com.tencent.bkrepo.repository.controller.user

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.pojo.software.ProjectPackageOverview
import com.tencent.bkrepo.repository.service.packages.SoftwarePackageService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Api("软件源包接口")
@RestController
@RequestMapping("/api/software/package")
class UserSoftwarePackageController(
    private val softwarePackageService: SoftwarePackageService
) {

    @ApiOperation("软件源包查询接口")
    @PostMapping("/search")
    fun searchPackage(
        @RequestBody queryModel: QueryModel
    ): Response<Page<MutableMap<*, *>>> {
        return ResponseBuilder.success(softwarePackageService.searchPackage(queryModel))
    }

    @ApiOperation("仓库 包数量 总览")
    @GetMapping("/search/overview")
    fun packageOverview(
        @RequestParam repoType: RepositoryType,
        @RequestParam projectId: String?,
        @RequestParam repoName: String?,
        @RequestParam packageName: String?
    ): Response<List<ProjectPackageOverview>> {
        return ResponseBuilder.success(
            softwarePackageService.packageOverview(
                repoType = repoType,
                projectId = projectId,
                repoName = repoName,
                packageName = packageName
            )
        )
    }
}
