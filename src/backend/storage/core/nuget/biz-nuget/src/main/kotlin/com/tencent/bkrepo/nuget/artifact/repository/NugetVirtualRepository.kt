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

package com.tencent.bkrepo.nuget.artifact.repository

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.virtual.VirtualRepository
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.common.NugetRemoteAndVirtualCommon
import com.tencent.bkrepo.nuget.pojo.artifact.NugetRegistrationArtifactInfo
import com.tencent.bkrepo.nuget.pojo.v3.metadata.index.RegistrationPageItem
import com.tencent.bkrepo.nuget.util.NugetUtils
import com.tencent.bkrepo.nuget.util.NugetV3RegistrationUtils
import com.tencent.bkrepo.nuget.util.NugetV3RemoteRepositoryUtils
import com.tencent.bkrepo.nuget.util.NugetVersionUtils
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.Objects
import java.util.stream.Stream
import kotlin.streams.toList

@Component
class NugetVirtualRepository(
    private val commonUtils: NugetRemoteAndVirtualCommon
) : VirtualRepository(), NugetRepository {

    override fun feed(artifactInfo: NugetArtifactInfo): ResponseEntity<Any> {
        return try {
            var feedResource = NugetUtils.getFeedResource()
            feedResource = feedResource.replace(
                "@NugetV2Url", NugetUtils.getV2Url(artifactInfo)
            ).replace(
                "@NugetV3Url", NugetUtils.getV3Url(artifactInfo)
            )
            ResponseEntity.ok(feedResource)
        } catch (exception: IOException) {
            logger.error("unable to read resource: $exception")
            throw exception
        }
    }

    override fun registrationIndex(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        isSemver2Endpoint: Boolean
    ): ResponseEntity<Any> {
        val allRegistrationPageItems = try {
            collectAllRegistrationPageItems(artifactInfo, registrationPath, isSemver2Endpoint)
        } catch (ex: IllegalStateException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND.value)
                .body(ex.message)
        }
        val virtualV3RegistrationUrl: String = NugetUtils.getV3Url(artifactInfo) + "/" + registrationPath
        val registrationIndex = NugetV3RegistrationUtils.registrationPageItemToRegistrationIndex(
            allRegistrationPageItems, virtualV3RegistrationUrl
        )
        return ResponseEntity.ok(registrationIndex)
    }

    override fun registrationPage(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        isSemver2Endpoint: Boolean
    ): ResponseEntity<Any> {
        val allRegistrationPageItems = try {
            collectAllRegistrationPageItems(artifactInfo, registrationPath, isSemver2Endpoint)
        } catch (ex: IllegalStateException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND.value)
                .body(ex.message)
        }
        val virtualV3RegistrationUrl: String = NugetUtils.getV3Url(artifactInfo) + "/" + registrationPath
        val registrationPage = NugetV3RegistrationUtils.registrationPageItemToRegistrationPage(
            allRegistrationPageItems,
            artifactInfo.packageName,
            artifactInfo.lowerVersion,
            artifactInfo.upperVersion,
            virtualV3RegistrationUrl
        )
        return ResponseEntity.ok(registrationPage)
    }

    override fun registrationLeaf(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        isSemver2Endpoint: Boolean
    ): ResponseEntity<Any> {
        val allRegistrationPageItems = try {
            collectAllRegistrationPageItems(artifactInfo, registrationPath, isSemver2Endpoint)
        } catch (ex: IllegalStateException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND.value)
                .body(ex.message)
        }
        val virtualV3RegistrationUrl: String = NugetUtils.getV3Url(artifactInfo) + "/" + registrationPath
        val isListed = allRegistrationPageItems.first().catalogEntry.listed.let { false }
        val registrationLeaf = NugetV3RegistrationUtils.metadataToRegistrationLeaf(
            artifactInfo.packageName,
            artifactInfo.version,
            isListed,
            virtualV3RegistrationUrl
        )
        return ResponseEntity.ok(registrationLeaf)
    }

    private fun collectAllRegistrationPageItems(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        isSemver2Endpoint: Boolean
    ): List<RegistrationPageItem> {
        val packageName = PackageKeys.resolveNuget(artifactInfo.packageName)
        val v2BaseUrl = NugetUtils.getV2Url(artifactInfo)
        val v3BaseUrl = NugetUtils.getV3Url(artifactInfo)
        val v3RegistrationUrl = "$v3BaseUrl/$registrationPath".trimEnd('/')
        val context = ArtifactQueryContext()
        val virtualConfiguration = context.getVirtualConfiguration()
        val repoList = virtualConfiguration.repositoryList
        // 分隔出本地仓库和远程仓库
        val repoCategoryMap = repoList.map {
            repositoryClient.getRepoDetail(it.projectId, it.name).data!!
        }.groupBy { it.category }
        // 分别查询出本地仓库和远程仓库下面的数组，然后在进行整合
        val allRemoteReposPageItems = repoCategoryMap[RepositoryCategory.REMOTE].orEmpty().stream().map {
            extractOrFetchRegistrationResultPageItems(artifactInfo, registrationPath, v2BaseUrl, v3BaseUrl, it)
        }.filter { Objects.nonNull(it) }.flatMap { it }.map {
            NugetV3RemoteRepositoryUtils.registrationResultPageItemRewriter(
                it, packageName, v2BaseUrl, v3RegistrationUrl
            )
        }
        // 查询本地仓库下面的数据
        val allLocalReposPageItems = repoCategoryMap[RepositoryCategory.LOCAL].orEmpty().stream().map {
            getMetadataForPackage(artifactInfo, registrationPath, v2BaseUrl, v3BaseUrl, it)
        }.filter { Objects.nonNull(it) }.flatMap { it }
        // 将本地仓库与远程仓库的数据进行合并
        val registrationResultPageItems = mergeRegistrationPageItems(allLocalReposPageItems, allRemoteReposPageItems)
        if (registrationPath.isEmpty()) {
            throw IllegalStateException("must have at least one page item")
        }
        return registrationResultPageItems
    }

    private fun mergeRegistrationPageItems(
        allLocalReposPageItems: Stream<RegistrationPageItem>,
        allRemoteReposPageItems: Stream<RegistrationPageItem>
    ): List<RegistrationPageItem> {
        // 需要去重
        return Stream.concat(allLocalReposPageItems, allRemoteReposPageItems).sorted { o1, o2 ->
            NugetVersionUtils.compareSemVer(o1.catalogEntry.version, o2.catalogEntry.version)
        }.toList()
    }

    private fun getMetadataForPackage(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        v2BaseUrl: String,
        v3BaseUrl: String,
        it: RepositoryDetail
    ): Stream<RegistrationPageItem> {
        with(artifactInfo) {
            val packageVersionList =
                packageClient.listAllVersion(projectId, repoName, PackageKeys.ofNuget(packageName)).data
            val sortedVersionList = packageVersionList.orEmpty().stream().sorted { o1, o2 ->
                NugetVersionUtils.compareSemVer(o1.name, o2.name)
            }.toList()
            val v3RegistrationUrl = NugetUtils.getV3Url(artifactInfo) + '/' + registrationPath
            return sortedVersionList.stream().map {
                NugetV3RegistrationUtils.metadataToRegistrationPageItem(it, v3RegistrationUrl)
            }
        }
    }

    private fun extractOrFetchRegistrationResultPageItems(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        v2BaseUrl: String,
        v3BaseUrl: String,
        repoDetail: RepositoryDetail
    ): Stream<RegistrationPageItem> {
        val registrationIndex =
            this.commonUtils.downloadRemoteRegistrationIndex(artifactInfo, registrationPath, v2BaseUrl, v3BaseUrl)
        if (registrationIndex != null && registrationIndex.items.isNotEmpty()) {
            return registrationIndex.items.first().items?.let {
                registrationIndex.items.stream().map { it.items }.flatMap { it.orEmpty().stream() }
            } ?: registrationIndex.items.stream().map {
                val registrationArtifactInfo = NugetRegistrationArtifactInfo(
                    projectId = artifactInfo.projectId,
                    repoName = artifactInfo.repoName,
                    packageName = artifactInfo.packageName,
                    lowerVersion = it.lower,
                    upperVersion = it.upper
                )
                this.commonUtils.downloadRemoteRegistrationPage(
                    registrationArtifactInfo, registrationPath, v2BaseUrl, v3BaseUrl
                )
            }.map { it.items }.flatMap { it.stream() }
        }
        return Stream.empty()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(NugetVirtualRepository::class.java)
    }
}
