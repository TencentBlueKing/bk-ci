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

package com.tencent.bkrepo.repository.service.file.impl

import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.view.ViewModelService
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.pojo.list.HeaderItem
import com.tencent.bkrepo.repository.pojo.list.RowItem
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.NodeListViewItem
import com.tencent.bkrepo.repository.pojo.project.ProjectListViewItem
import com.tencent.bkrepo.repository.pojo.repo.RepoListViewItem
import com.tencent.bkrepo.repository.service.file.ListViewService
import com.tencent.bkrepo.repository.service.node.NodeService
import com.tencent.bkrepo.repository.service.repo.ProjectService
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 列表视图服务
 */
@Service
class ListViewServiceImpl(
    private val projectService: ProjectService,
    private val repositoryService: RepositoryService,
    private val nodeService: NodeService,
    private val viewModelService: ViewModelService
) : ListViewService {

    @Value("\${service.name}")
    private var applicationName: String = "repository"

    override fun listNodeView(artifactInfo: ArtifactInfo) {
        val node = nodeService.getNodeDetail(artifactInfo)
            ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, artifactInfo.getArtifactFullPath())
        val response = HttpContextHolder.getResponse()
        response.contentType = MediaTypes.TEXT_HTML
        if (node.folder) {
            viewModelService.trailingSlash(applicationName)
            val listOption = NodeListOption(
                includeFolder = true,
                includeMetadata = false,
                deep = false
            )
            val nodeList = nodeService.listNode(artifactInfo, listOption)
            val currentPath = viewModelService.computeCurrentPath(node)
            val headerList = listOf(
                HeaderItem("Name"),
                HeaderItem("Created by"),
                HeaderItem("Last modified"),
                HeaderItem("Size"),
                HeaderItem("Sha256")
            )
            val itemList = nodeList.map { NodeListViewItem.from(it) }.sorted()
            val rowList = itemList.map { RowItem(listOf(it.name, it.createdBy, it.lastModified, it.size, it.sha256)) }
            viewModelService.render(currentPath, headerList, rowList)
        } else {
            val context = ArtifactDownloadContext()
            ArtifactContextHolder.getRepository().download(context)
        }
    }

    override fun listRepoView(projectId: String) {
        viewModelService.trailingSlash(applicationName)
        val itemList = repositoryService.listRepo(projectId).map { RepoListViewItem.from(it) }
        val title = "Repository[$projectId]"
        val headerList = listOf(
            HeaderItem("Name"),
            HeaderItem("Created by"),
            HeaderItem("Last modified"),
            HeaderItem("Category"),
            HeaderItem("Type"),
            HeaderItem("Public"),
            HeaderItem("Storage")
        )
        val rowList = itemList.sorted().map {
            RowItem(listOf(it.name, it.createdBy, it.lastModified, it.category, it.type, it.public, it.storage))
        }
        viewModelService.render(title, headerList, rowList)
    }

    override fun listProjectView() {
        viewModelService.trailingSlash(applicationName)
        val itemList = projectService.listPermissionProject(SecurityUtils.getUserId(), null)
            .map { ProjectListViewItem.from(it) }
        val headerList = listOf(
            HeaderItem("Name"),
            HeaderItem("Created by"),
            HeaderItem("Last modified"),
            HeaderItem("Sharding index")
        )
        val rowList = itemList.sorted().map {
            RowItem(listOf(it.name, it.createdBy, it.lastModified, it.shardingIndex))
        }
        viewModelService.render("Project", headerList, rowList)
    }
}
