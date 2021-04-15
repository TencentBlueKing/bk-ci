/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo.Companion.DEFAULT_MAPPING_URI
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.repository.service.file.ListViewService
import com.tencent.bkrepo.repository.util.PipelineRepoUtils
import io.swagger.annotations.Api
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Api("用户节点列表页")
@RestController
@RequestMapping("/api/list")
class UserListViewController(
    private val listViewService: ListViewService,
    private val permissionManager: PermissionManager
) {

    @Permission(type = ResourceType.NODE, action = PermissionAction.READ)
    @GetMapping(DEFAULT_MAPPING_URI)
    fun listNodeView(artifactInfo: ArtifactInfo) {
        // 禁止查询pipeline仓库
        PipelineRepoUtils.checkPipeline(artifactInfo.repoName)
        listViewService.listNodeView(artifactInfo)
    }

    @Principal(PrincipalType.ADMIN)
    @GetMapping
    fun listProjectView() {
        listViewService.listProjectView()
    }

    @GetMapping("/{projectId}")
    fun listRepositoryView(@PathVariable projectId: String) {
        permissionManager.checkProjectPermission(PermissionAction.READ, projectId)
        listViewService.listRepoView(projectId)
    }
}
