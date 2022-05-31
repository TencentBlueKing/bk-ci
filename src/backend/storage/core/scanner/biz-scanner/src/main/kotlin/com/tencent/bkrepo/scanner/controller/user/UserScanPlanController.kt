/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.scanner.controller.user

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_NUMBER
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import com.tencent.bkrepo.common.api.exception.BadRequestException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.scanner.component.ScannerPermissionCheckHandler
import com.tencent.bkrepo.scanner.pojo.ScanPlan
import com.tencent.bkrepo.scanner.pojo.request.ArtifactPlanRelationRequest
import com.tencent.bkrepo.scanner.pojo.request.CreateScanPlanRequest
import com.tencent.bkrepo.scanner.pojo.request.PlanArtifactRequest
import com.tencent.bkrepo.scanner.pojo.request.UpdateScanPlanRequest
import com.tencent.bkrepo.scanner.pojo.response.ArtifactPlanRelation
import com.tencent.bkrepo.scanner.pojo.response.PlanArtifactInfo
import com.tencent.bkrepo.scanner.pojo.response.ScanPlanBase
import com.tencent.bkrepo.scanner.pojo.response.ScanPlanInfo
import com.tencent.bkrepo.scanner.service.ScanPlanService
import com.tencent.bkrepo.scanner.utils.ScanPlanConverter
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/scan/plan")
class UserScanPlanController(
    private val scanPlanService: ScanPlanService,
    private val permissionCheckHandler: ScannerPermissionCheckHandler
) {

    @ApiOperation("创建扫描方案")
    @PostMapping("/create")
    fun createScanPlan(@RequestBody request: CreateScanPlanRequest): Response<Boolean> {
        permissionCheckHandler.checkProjectPermission(request.projectId, PermissionAction.MANAGE)
        val scanPlan = ScanPlanConverter.convert(request)
        scanPlanService.create(scanPlan)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("查询扫描方案基础信息")
    @GetMapping("/detail/{projectId}/{id}")
    @Permission(ResourceType.PROJECT, PermissionAction.MANAGE)
    fun getScanPlan(
        @ApiParam(value = "projectId")
        @PathVariable
        projectId: String,
        @ApiParam(value = "方案id")
        @PathVariable
        id: String
    ): Response<ScanPlanBase?> {
        return ResponseBuilder.success(
            scanPlanService.find(projectId, id)?.let { ScanPlanConverter.convert(it) }
        )
    }

    @ApiOperation("删除扫描方案")
    @DeleteMapping("/delete/{projectId}/{id}")
    @Permission(ResourceType.PROJECT, PermissionAction.MANAGE)
    fun deleteScanPlan(
        @ApiParam(value = "projectId")
        @PathVariable projectId: String,
        @ApiParam(value = "方案id")
        @PathVariable id: String
    ): Response<Boolean> {
        scanPlanService.delete(projectId, id)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("更新扫描方案")
    @PostMapping("/update")
    fun updateScanPlan(@RequestBody request: UpdateScanPlanRequest): Response<Boolean> {
        val projectId = request.projectId ?: throw BadRequestException(CommonMessageCode.PARAMETER_INVALID)
        permissionCheckHandler.checkProjectPermission(projectId, PermissionAction.MANAGE)
        scanPlanService.update(request)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("扫描方案列表-分页")
    @GetMapping("/list/{projectId}")
    @Permission(ResourceType.PROJECT, PermissionAction.MANAGE)
    fun scanPlanList(
        @ApiParam(value = "projectId", required = true)
        @PathVariable
        projectId: String,
        @ApiParam(value = "方案类型(DEPENDENT/MOBILE)")
        @RequestParam
        type: String?,
        @ApiParam(value = "方案名")
        @RequestParam
        name: String?,
        @ApiParam("页数", required = false, defaultValue = "1")
        @RequestParam(required = false, defaultValue = DEFAULT_PAGE_NUMBER.toString())
        pageNumber: Int = DEFAULT_PAGE_NUMBER,
        @ApiParam("每页数量", required = false, defaultValue = "20")
        @RequestParam(required = false, defaultValue = DEFAULT_PAGE_SIZE.toString())
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): Response<Page<ScanPlanInfo>> {
        val page = scanPlanService.page(
            projectId = projectId, type = type, planNameContains = name, pageLimit = PageLimit(pageNumber, pageSize)
        )
        return ResponseBuilder.success(page)
    }

    @ApiOperation("所有扫描方案")
    @GetMapping("/all/{projectId}")
    @Permission(ResourceType.PROJECT, PermissionAction.READ)
    fun scanPlanList(
        @ApiParam(value = "projectId", required = true)
        @PathVariable
        projectId: String,
        @ApiParam(value = "方案类型")
        @RequestParam
        type: String?
    ): Response<List<ScanPlan>> {
        val planList = scanPlanService.list(projectId, type)
        planList.forEach { ScanPlanConverter.keepProps(it, KEEP_PROPS) }
        return ResponseBuilder.success(planList)
    }

    @ApiOperation("方案详情-统计数据")
    @GetMapping("/count/{projectId}/{id}")
    @Permission(ResourceType.PROJECT, PermissionAction.MANAGE)
    fun planDetailCount(
        @ApiParam(value = "projectId")
        @PathVariable
        projectId: String,

        @ApiParam(value = "方案id")
        @PathVariable
        id: String
    ): Response<ScanPlanInfo?> {
        return ResponseBuilder.success(scanPlanService.scanPlanInfo(projectId, id))
    }

    @ApiOperation("方案详情-制品信息")
    @GetMapping("/artifact")
    fun planArtifactList(planArtifactRequest: PlanArtifactRequest): Response<Page<PlanArtifactInfo>> {
        permissionCheckHandler.checkProjectPermission(planArtifactRequest.projectId, PermissionAction.MANAGE)
        return ResponseBuilder.success(scanPlanService.planArtifactPage(ScanPlanConverter.convert(planArtifactRequest)))
    }

    @ApiOperation("文件/包关联的扫描方案列表")
    @GetMapping("/relation/artifact")
    fun artifactPlanList(
        artifactRequest: ArtifactPlanRelationRequest
    ): Response<List<ArtifactPlanRelation>> {
        return ResponseBuilder.success(scanPlanService.artifactPlanList(artifactRequest))
    }

    companion object {
        private val KEEP_PROPS = listOf(ScanPlan::id, ScanPlan::name)
    }
}
