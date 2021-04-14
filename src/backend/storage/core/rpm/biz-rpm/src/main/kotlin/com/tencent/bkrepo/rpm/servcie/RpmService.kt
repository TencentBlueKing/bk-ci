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

package com.tencent.bkrepo.rpm.servcie

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.artifact.exception.ArtifactNotFoundException
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.configuration.RepositoryConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactRepository
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.repo.RepoUpdateRequest
import com.tencent.bkrepo.rpm.FILELISTS_XML
import com.tencent.bkrepo.rpm.OTHERS_XML
import com.tencent.bkrepo.rpm.PRIMARY_XML
import com.tencent.bkrepo.rpm.REPOMD_XML
import com.tencent.bkrepo.rpm.artifact.RpmArtifactInfo
import com.tencent.bkrepo.rpm.artifact.repository.RpmLocalRepository
import com.tencent.bkrepo.rpm.exception.RpmConfNotFoundException
import org.springframework.stereotype.Service

@Service
class RpmService(
    private val repositoryClient: RepositoryClient
) : ArtifactService() {

    // groups 中不允许的元素
    private val rpmIndexSet = mutableSetOf(REPOMD_XML, FILELISTS_XML, OTHERS_XML, PRIMARY_XML)

    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun install(rpmArtifactInfo: RpmArtifactInfo) {
        val context = ArtifactDownloadContext()
        if (rpmArtifactInfo.getArtifactFullPath().endsWith(REPOMD_XML)) {
            repository.downloadRetry(context)
        } else {
            repository.download(context)
        }
    }

    private fun ArtifactRepository.downloadRetry(context: ArtifactDownloadContext) {
        for (i in 1..4) {
            try {
                this.download(context)
                break
            } catch (e: ArtifactNotFoundException) {
                if (i == 4) throw e
                Thread.sleep(i * 1000L)
            }
        }
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun deploy(rpmArtifactInfo: RpmArtifactInfo, file: ArtifactFile) {
        val context = ArtifactUploadContext(file)
        repository.upload(context)
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun addGroups(rpmArtifactInfo: RpmArtifactInfo, groups: MutableSet<String>) {
        val context = ArtifactSearchContext()
        groups.removeAll(rpmIndexSet)
        val rpmConfiguration = getRpmRepoConf(context.projectId, context.repoName)
        val oldGroups = (
                rpmConfiguration.getSetting<MutableList<String>>("groupXmlSet")
                    ?: mutableListOf()
                ).toMutableSet()
        oldGroups.addAll(groups)
        rpmConfiguration.settings["groupXmlSet"] = oldGroups
        val repoUpdateRequest = createRepoUpdateRequest(context, rpmConfiguration)
        repositoryClient.updateRepo(repoUpdateRequest)
        val repository = ArtifactContextHolder.getRepository(RepositoryCategory.LOCAL)
        (repository as RpmLocalRepository).flushAllRepoData(context)
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun deleteGroups(rpmArtifactInfo: RpmArtifactInfo, groups: MutableSet<String>) {
        val context = ArtifactSearchContext()
        val rpmConfiguration = getRpmRepoConf(context.projectId, context.repoName)
        val oldGroups = (
                rpmConfiguration.getSetting<MutableList<String>>("groupXmlSet")
                    ?: mutableListOf()
                ).toMutableSet()
        oldGroups.removeAll(groups)
        rpmConfiguration.settings["groupXmlSet"] = oldGroups
        val repoUpdateRequest = createRepoUpdateRequest(context, rpmConfiguration)
        repositoryClient.updateRepo(repoUpdateRequest)
        val repository = ArtifactContextHolder.getRepository(RepositoryCategory.LOCAL)
        (repository as RpmLocalRepository).flushAllRepoData(context)
    }

    private fun createRepoUpdateRequest(
        context: ArtifactSearchContext,
        rpmConfiguration: RepositoryConfiguration
    ): RepoUpdateRequest {
        return RepoUpdateRequest(
            context.artifactInfo.projectId,
            context.artifactInfo.repoName,
            context.repositoryDetail.public,
            context.repositoryDetail.description,
            rpmConfiguration,
            context.userId
        )
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun delete(@ArtifactPathVariable rpmArtifactInfo: RpmArtifactInfo) {
        val context = ArtifactRemoveContext()
        repository.remove(context)
    }

    private fun getRpmRepoConf(project: String, repoName: String): RepositoryConfiguration {
        val repositoryInfo = repositoryClient.getRepoInfo(project, repoName).data
            ?: throw RpmConfNotFoundException("can not found $project | $repoName conf")
        return repositoryInfo.configuration
    }
}
