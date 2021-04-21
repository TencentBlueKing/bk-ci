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

package com.tencent.bkrepo.repository.controller.user

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo.Companion.DEFAULT_MAPPING_URI
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.pojo.share.BatchShareRecordCreateRequest
import com.tencent.bkrepo.repository.pojo.share.ShareRecordCreateRequest
import com.tencent.bkrepo.repository.pojo.share.ShareRecordInfo
import com.tencent.bkrepo.repository.service.file.ShareService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 用户分享接口
 */
@Api("节点分享用户接口")
@RestController
@RequestMapping("/api/share")
class UserShareController(
    private val permissionManager: PermissionManager,
    private val shareService: ShareService
) {

    @ApiOperation("创建分享链接")
    @Permission(type = ResourceType.NODE, action = PermissionAction.WRITE)
    @PostMapping(DEFAULT_MAPPING_URI)
    fun share(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: ArtifactInfo,
        @RequestBody shareRecordCreateRequest: ShareRecordCreateRequest
    ): Response<ShareRecordInfo> {
        return ResponseBuilder.success(shareService.create(userId, artifactInfo, shareRecordCreateRequest))
    }

    @ApiOperation("批量创建分享链接")
    @PostMapping("/batch")
    fun batchShare(
        @RequestAttribute userId: String,
        @RequestBody batchShareRecordCreateRequest: BatchShareRecordCreateRequest
    ): Response<List<ShareRecordInfo>> {
        with(batchShareRecordCreateRequest) {
            val shareRecordCreateRequest = ShareRecordCreateRequest(authorizedUserList, authorizedIpList, expireSeconds)
            val recordInfoList = fullPathList.map {
                val fullPath = PathUtils.normalizeFullPath(it)
                permissionManager.checkNodePermission(PermissionAction.WRITE, projectId, repoName, fullPath)
                val artifactInfo = DefaultArtifactInfo(projectId, repoName, fullPath)
                shareService.create(userId, artifactInfo, shareRecordCreateRequest)
            }
            return ResponseBuilder.success(recordInfoList)
        }
    }

    @ApiOperation("下载分享文件")
    @GetMapping(DEFAULT_MAPPING_URI)
    fun download(
        @RequestAttribute userId: String,
        @RequestParam token: String,
        @ArtifactPathVariable artifactInfo: ArtifactInfo
    ) {
        shareService.download(userId, token, artifactInfo)
    }
}
