/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.oci.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.oci.pojo.OciDomainInfo
import com.tencent.bkrepo.oci.pojo.artifact.OciArtifactInfo
import com.tencent.bkrepo.oci.pojo.artifact.OciArtifactInfo.Companion.OCI_PACKAGE_DELETE_URL
import com.tencent.bkrepo.oci.pojo.artifact.OciArtifactInfo.Companion.OCI_VERSION_DELETE_URL
import com.tencent.bkrepo.oci.pojo.artifact.OciArtifactInfo.Companion.OCI_VERSION_DETAIL
import com.tencent.bkrepo.oci.pojo.user.PackageVersionInfo
import com.tencent.bkrepo.oci.service.OciOperationService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@Api("oci产品接口")
@RestController
@RequestMapping("/ext")
class UserOciController(
    private val operationService: OciOperationService
) {

    @ApiOperation("查询包的版本详情")
    @GetMapping(OCI_VERSION_DETAIL)
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun detailVersion(
        @RequestAttribute
        userId: String,
        @ArtifactPathVariable artifactInfo: OciArtifactInfo,
        @ApiParam(value = "包唯一Key", required = true)
        @RequestParam packageKey: String,
        @ApiParam(value = "包版本", required = true)
        @RequestParam version: String
    ): Response<PackageVersionInfo> {
        return ResponseBuilder.success(
            operationService.detailVersion(userId, artifactInfo, packageKey, version)
        )
    }

    @ApiOperation("删除仓库下的包")
    @DeleteMapping(OCI_PACKAGE_DELETE_URL)
    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun deletePackage(
        @RequestAttribute userId: String,
        artifactInfo: OciArtifactInfo,
        @ApiParam(value = "包唯一key", required = true)
        @RequestParam packageKey: String
    ): Response<Void> {
        operationService.deletePackage(userId, artifactInfo)
        return ResponseBuilder.success()
    }

    @ApiOperation("删除仓库下的包版本")
    @DeleteMapping(OCI_VERSION_DELETE_URL)
    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun deleteVersion(
        @RequestAttribute userId: String,
        artifactInfo: OciArtifactInfo,
        @ApiParam(value = "包唯一key", required = true)
        @RequestParam packageKey: String,
        @ApiParam(value = "包版本", required = true)
        @RequestParam version: String
    ): Response<Void> {
        operationService.deleteVersion(userId, artifactInfo)
        return ResponseBuilder.success()
    }

    @ApiOperation("获取Oci域名地址")
    @GetMapping("/address")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun getRegistryDomain(): Response<OciDomainInfo> {
        return ResponseBuilder.success(operationService.getRegistryDomain())
    }
}
