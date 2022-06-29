/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.maven.service.impl

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.constant.PARAM_DOWNLOAD
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.artifact.view.ViewModelService
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.maven.artifact.MavenArtifactInfo
import com.tencent.bkrepo.maven.artifact.MavenDeleteArtifactInfo
import com.tencent.bkrepo.maven.exception.MavenBadRequestException
import com.tencent.bkrepo.maven.service.MavenService
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.list.HeaderItem
import com.tencent.bkrepo.repository.pojo.list.RowItem
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeListViewItem
import java.util.regex.PatternSyntaxException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class MavenServiceImpl(
    private val nodeClient: NodeClient,
    private val viewModelService: ViewModelService
) : ArtifactService(), MavenService {

    @Value("\${spring.application.name}")
    private var applicationName: String = "maven"

    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    override fun deploy(
        mavenArtifactInfo: MavenArtifactInfo,
        file: ArtifactFile
    ) {
        val context = ArtifactUploadContext(file)
        try {
            ArtifactContextHolder.getRepository().upload(context)
        } catch (e: PatternSyntaxException) {
            logger.warn(
                "Error [${e.message}] occurred during uploading ${mavenArtifactInfo.getArtifactFullPath()} " +
                    "in repo ${mavenArtifactInfo.getRepoIdentify()}"
            )
            throw MavenBadRequestException(e.message)
        }
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    override fun dependency(mavenArtifactInfo: MavenArtifactInfo) {
        // 为了兼容jfrog，当查询到目录时，会展示当前目录下所有子项，而不是直接报错
        with(mavenArtifactInfo) {
            val node = nodeClient.getNodeDetail(projectId, repoName, getArtifactFullPath()).data
            val download = HttpContextHolder.getRequest().getParameter(PARAM_DOWNLOAD)?.toBoolean() ?: false
            if (node != null) {
                if (node.folder && !download) {
                    logger.info("The folder: ${getArtifactFullPath()} will be displayed...")
                    renderListView(node, this)
                } else {
                    logger.info("The dependency file: ${getArtifactFullPath()} will be downloaded... ")
                    val context = ArtifactDownloadContext()
                    ArtifactContextHolder.getRepository().download(context)
                }
            } else {
                logger.info("The dependency file: ${getArtifactFullPath()} will be downloaded... ")
                val context = ArtifactDownloadContext()
                ArtifactContextHolder.getRepository().download(context)
            }
        }
    }

    /**
     * 当查询节点为目录时，将其子节点以页面形式展示
     */
    private fun renderListView(node: NodeDetail, artifactInfo: MavenArtifactInfo) {
        with(artifactInfo) {
            viewModelService.trailingSlash(applicationName)
            // listNodePage 接口没办法满足当前情况
            val nodeList = nodeClient.listNode(
                projectId = projectId,
                repoName = repoName,
                path = getArtifactFullPath(),
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

    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    override fun delete(mavenArtifactInfo: MavenDeleteArtifactInfo, packageKey: String, version: String?) {
        val context = ArtifactRemoveContext()
        ArtifactContextHolder.getRepository().remove(context)
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    override fun deleteDependency(mavenArtifactInfo: MavenArtifactInfo) {
        val context = ArtifactRemoveContext()
        ArtifactContextHolder.getRepository().remove(context)
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    override fun artifactDetail(mavenArtifactInfo: MavenArtifactInfo, packageKey: String, version: String?): Any? {
        val context = ArtifactQueryContext()
        return ArtifactContextHolder.getRepository().query(context)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MavenServiceImpl::class.java)
    }
}
