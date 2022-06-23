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

package com.tencent.bkrepo.generic.service

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.constant.PARAM_DOWNLOAD
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.artifact.view.ViewModelService
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.generic.artifact.GenericArtifactInfo
import com.tencent.bkrepo.generic.artifact.configuration.AutoIndexRepositorySettings
import com.tencent.bkrepo.generic.constant.GenericMessageCode
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.list.HeaderItem
import com.tencent.bkrepo.repository.pojo.list.RowItem
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeListViewItem
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 通用文件下载服务类
 */
@Service
class DownloadService(
    private val nodeClient: NodeClient,
    private val viewModelService: ViewModelService
) : ArtifactService() {

    @Value("\${spring.application.name}")
    private var applicationName: String = "generic"

    fun download(artifactInfo: GenericArtifactInfo) {
        with(artifactInfo) {
            val node = nodeClient.getNodeDetail(projectId, repoName, getArtifactFullPath()).data
                ?: throw NodeNotFoundException(getArtifactFullPath())
            val download = HttpContextHolder.getRequest().getParameter(PARAM_DOWNLOAD)?.toBoolean() ?: false
            val context = ArtifactDownloadContext()

            // 仓库未开启自动创建目录索引时不允许访问目录
            val autoIndexSettings = AutoIndexRepositorySettings.from(context.repositoryDetail.configuration)
            if (node.folder && autoIndexSettings?.enabled == false) {
                throw ErrorCodeException(
                    messageCode = GenericMessageCode.LIST_DIR_NOT_ALLOWED,
                    params = arrayOf(context.repoName, getArtifactFullPath()),
                    status = HttpStatus.FORBIDDEN
                )
            }

            if (node.folder && !download) {
                renderListView(node, this)
            } else {
                repository.download(context)
            }
        }
    }

    fun batchDownload(artifactInfoList: List<GenericArtifactInfo>) {
        val context = ArtifactDownloadContext(
            artifact = artifactInfoList.first(),
            artifacts = artifactInfoList,
            repo = ArtifactContextHolder.getRepoDetail()
        )
        repository.download(context)
    }

    private fun renderListView(node: NodeDetail, artifactInfo: GenericArtifactInfo) {
        viewModelService.trailingSlash(applicationName)
        val nodeList = nodeClient.listNode(
            projectId = artifactInfo.projectId,
            repoName = artifactInfo.repoName,
            path = artifactInfo.getArtifactFullPath(),
            includeFolder = true,
            deep = false
        ).data
        val currentPath = viewModelService.computeCurrentPath(node)
        val headerList = listOf(
            HeaderItem("Name"),
            HeaderItem("Created by"),
            HeaderItem("Last modified"),
            HeaderItem("Size"),
            HeaderItem("Sha256")
        )
        val itemList = nodeList?.map { NodeListViewItem.from(it) }?.sorted()
        val rowList = itemList?.map {
            RowItem(listOf(it.name, it.createdBy, it.lastModified, it.size, it.sha256))
        } ?: listOf()
        viewModelService.render(currentPath, headerList, rowList)
    }
}
