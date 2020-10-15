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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.helm.artifact.repository

import com.tencent.bkrepo.common.artifact.constant.ATTRIBUTE_MD5MAP
import com.tencent.bkrepo.common.artifact.constant.ATTRIBUTE_SHA256MAP
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactTransferContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.helm.constants.FULL_PATH
import com.tencent.bkrepo.helm.constants.INDEX_YAML
import com.tencent.bkrepo.helm.exception.HelmFileAlreadyExistsException
import com.tencent.bkrepo.helm.exception.HelmFileNotFoundException
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HelmLocalRepository : LocalRepository() {

    override fun determineArtifactName(context: ArtifactTransferContext): String {
        val fileName = context.artifactInfo.artifactUri.trimStart('/')
        return if (StringUtils.isBlank(fileName)) INDEX_YAML else fileName
    }

    override fun onUploadBefore(context: ArtifactUploadContext) {
        // 判断是否是强制上传
        val isForce = context.request.getParameter("force")?.let { true } ?: false
        context.contextAttributes["force"] = isForce
        val repositoryInfo = context.repositoryInfo
        val projectId = repositoryInfo.projectId
        val repoName = repositoryInfo.name
        context.artifactFileMap.entries.forEach { (name, _) ->
            val fullPath = context.contextAttributes[name + "_full_path"] as String
            val isExist = nodeClient.exist(projectId, repoName, fullPath).data!!
            if (isExist && !isOverwrite(fullPath, isForce)) {
                throw HelmFileAlreadyExistsException("${fullPath.trimStart('/')} already exists")
            }
        }
    }

    override fun onUpload(context: ArtifactUploadContext) {
        context.artifactFileMap.entries.forEach { (name, _) ->
            val nodeCreateRequest = getNodeCreateRequest(name, context)
            storageService.store(
                nodeCreateRequest.sha256!!,
                context.getArtifactFile(name) ?: context.getArtifactFile(),
                context.storageCredentials
            )
            nodeClient.create(nodeCreateRequest)
        }
    }

    private fun getNodeCreateRequest(name: String, context: ArtifactUploadContext): NodeCreateRequest {
        val repositoryInfo = context.repositoryInfo
        val artifactFile = context.getArtifactFile(name) ?: context.getArtifactFile()
        val fileSha256Map = context.contextAttributes[ATTRIBUTE_SHA256MAP] as Map<*, *>
        val fileMd5Map = context.contextAttributes[ATTRIBUTE_MD5MAP] as Map<*, *>
        val sha256 = fileSha256Map[name] as String
        val md5 = fileMd5Map[name] as String
        val fullPath = context.contextAttributes[name + "_full_path"] as String
        val isForce = context.contextAttributes["force"] as Boolean
        return NodeCreateRequest(
            projectId = repositoryInfo.projectId,
            repoName = repositoryInfo.name,
            folder = false,
            fullPath = fullPath,
            size = artifactFile.getSize(),
            sha256 = sha256,
            md5 = md5,
            operator = context.userId,
            metadata = parseMetaData(fullPath, isForce),
            overwrite = isOverwrite(fullPath, isForce)
        )
    }

    private fun parseMetaData(fullPath: String, isForce: Boolean): Map<String, String>? {
        if (isOverwrite(fullPath, isForce) || !fullPath.endsWith(".tgz")) {
            return emptyMap()
        }
        val substring = fullPath.trimStart('/').substring(0, fullPath.lastIndexOf('.') - 1)
        val name = substring.substringBeforeLast('-')
        val version = substring.substringAfterLast('-')
        return mapOf("name" to name, "version" to version)
    }

    private fun isOverwrite(fullPath: String, isForce: Boolean): Boolean {
        return isForce || !(fullPath.trim().endsWith(".tgz", true) || fullPath.trim().endsWith(".prov", true))
    }

    override fun determineArtifactUri(context: ArtifactDownloadContext): String {
        return context.contextAttributes[FULL_PATH] as String
    }

    override fun search(context: ArtifactSearchContext): ArtifactInputStream {
        val fullPath = context.contextAttributes[FULL_PATH] as String
        return this.onSearch(context) ?: throw HelmFileNotFoundException("Artifact[$fullPath] does not exist")
    }

    private fun onSearch(context: ArtifactSearchContext): ArtifactInputStream? {
        val repositoryInfo = context.repositoryInfo
        val projectId = repositoryInfo.projectId
        val repoName = repositoryInfo.name
        val fullPath = context.contextAttributes[FULL_PATH] as String
        val node = nodeClient.detail(projectId, repoName, fullPath).data
        if (node == null || node.folder) return null
        return storageService.load(
            node.sha256!!, Range.ofFull(node.size), context.storageCredentials
        )?.also { logger.info("search artifact [$fullPath] success!") }
    }

    override fun remove(context: ArtifactRemoveContext) {
        val repositoryInfo = context.repositoryInfo
        val projectId = repositoryInfo.projectId
        val repoName = repositoryInfo.name
        val fullPath = context.contextAttributes[FULL_PATH] as String
        val userId = context.userId
        val isExist = nodeClient.exist(projectId, repoName, fullPath).data!!
        if (!isExist) {
            throw HelmFileNotFoundException("remove $fullPath failed: no such file or directory")
        }
        nodeClient.delete(NodeDeleteRequest(projectId, repoName, fullPath, userId))
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(HelmLocalRepository::class.java)
    }
}
