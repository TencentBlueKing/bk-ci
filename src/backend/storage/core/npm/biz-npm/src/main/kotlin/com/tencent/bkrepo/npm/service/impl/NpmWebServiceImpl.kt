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

package com.tencent.bkrepo.npm.service.impl

import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.constants.LATEST
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.TGZ_FULL_PATH_WITH_DASH_SEPARATOR
import com.tencent.bkrepo.npm.exception.NpmArtifactNotFoundException
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.model.metadata.NpmVersionMetadata
import com.tencent.bkrepo.npm.pojo.NpmDomainInfo
import com.tencent.bkrepo.npm.pojo.user.BasicInfo
import com.tencent.bkrepo.npm.pojo.user.DependenciesInfo
import com.tencent.bkrepo.npm.pojo.user.PackageVersionInfo
import com.tencent.bkrepo.npm.pojo.user.VersionDependenciesInfo
import com.tencent.bkrepo.npm.pojo.user.request.PackageDeleteRequest
import com.tencent.bkrepo.npm.pojo.user.request.PackageVersionDeleteRequest
import com.tencent.bkrepo.npm.service.NpmClientService
import com.tencent.bkrepo.npm.service.NpmWebService
import com.tencent.bkrepo.npm.utils.NpmUtils
import com.tencent.bkrepo.repository.api.PackageDependentsClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NpmWebServiceImpl : NpmWebService, AbstractNpmService() {

    @Autowired
    private lateinit var packageDependentsClient: PackageDependentsClient

    @Autowired
    private lateinit var npmClientService: NpmClientService

    @Transactional(rollbackFor = [Throwable::class])
    override fun detailVersion(artifactInfo: NpmArtifactInfo, packageKey: String, version: String): PackageVersionInfo {
        val name = PackageKeys.resolveNpm(packageKey)
        val packageMetadata = queryPackageInfo(artifactInfo, name, false)
        if (!packageMetadata.versions.map.keys.contains(version)) {
            throw NpmArtifactNotFoundException("version [$version] don't found in package [$name].")
        }
        val pathWithDash = packageMetadata.versions.map[version]?.dist?.tarball?.substringAfter(name)
            ?.contains(TGZ_FULL_PATH_WITH_DASH_SEPARATOR) ?: true
        val fullPath = NpmUtils.getTgzPath(name, version, pathWithDash)
        with(artifactInfo) {
            checkRepositoryExist(projectId, repoName)
            val nodeDetail = nodeClient.getNodeDetail(projectId, repoName, fullPath).data ?: run {
                logger.warn("node [$fullPath] don't found.")
                throw NpmArtifactNotFoundException("node [$fullPath] don't found.")
            }
            val packageVersion = packageClient.findVersionByName(projectId, repoName, packageKey, version).data ?: run {
                logger.warn("packageKey [$packageKey] don't found.")
                throw NpmArtifactNotFoundException("packageKey [$packageKey] don't found.")
            }
            val basicInfo = buildBasicInfo(nodeDetail, packageVersion)
            val versionDependenciesInfo =
                queryVersionDependenciesInfo(artifactInfo, packageKey, packageMetadata.versions.map[version]!!)
            return PackageVersionInfo(basicInfo, emptyMap(), versionDependenciesInfo)
        }
    }

    private fun queryVersionDependenciesInfo(
        artifactInfo: NpmArtifactInfo,
        packageKey: String,
        versionMetadata: NpmVersionMetadata
    ): VersionDependenciesInfo {
        val packageDependents = packageDependentsClient.queryDependents(artifactInfo.projectId,
            artifactInfo.repoName, packageKey).data.orEmpty()
        val dependenciesList = parseDependencies(versionMetadata)
        val devDependenciesList = parseDevDependencies(versionMetadata)
        return VersionDependenciesInfo(dependenciesList, devDependenciesList, packageDependents)
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun deletePackage(artifactInfo: NpmArtifactInfo, deleteRequest: PackageDeleteRequest) {
        logger.info("npm delete package request: [$deleteRequest]")
        with(deleteRequest) {
            checkRepositoryExist(projectId, repoName)
            npmClientService.deletePackage(operator, artifactInfo, name)
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun deleteVersion(artifactInfo: NpmArtifactInfo, deleteRequest: PackageVersionDeleteRequest) {
        logger.info("npm delete package version request: [$deleteRequest]")
        with(deleteRequest) {
            checkRepositoryExist(projectId, repoName)
            val packageMetadata = queryPackageInfo(artifactInfo, name, false)
            val versionEntries = packageMetadata.versions.map.entries
            val iterator = versionEntries.iterator()
            // 如果删除最后一个版本直接删除整个包
            if (versionEntries.size == 1 && iterator.hasNext() && iterator.next().key == version) {
                val deletePackageRequest = PackageDeleteRequest(projectId, repoName, name, operator)
                deletePackage(artifactInfo, deletePackageRequest)
                return
            }
            val tgzPath =
                packageMetadata.versions.map[version]?.dist?.tarball?.substringAfterLast(
                    artifactInfo.getRepoIdentify()
                ).orEmpty()
            npmClientService.deleteVersion(artifactInfo, name, version, tgzPath)
            // 修改package.json文件的内容
            updatePackageWithDeleteVersion(artifactInfo, this, packageMetadata)
        }
    }

    override fun getRegistryDomain(): NpmDomainInfo {
        return NpmDomainInfo(UrlFormatter.formatHost(npmProperties.domain))
    }

    fun updatePackageWithDeleteVersion(
        artifactInfo: NpmArtifactInfo,
        deleteRequest: PackageVersionDeleteRequest,
        packageMetaData: NpmPackageMetaData
    ) {
        with(deleteRequest) {
            val latest = NpmUtils.getLatestVersionFormDistTags(packageMetaData.distTags)
            if (version != latest) {
                // 删除versions里面对应的版本
                deleteVersion(packageMetaData)
            } else {
                val newLatest = findNewLatest(this)
                packageMetaData.versions.map.remove(version)
                packageMetaData.time.getMap().remove(version)
                packageMetaData.distTags.set(LATEST, newLatest)
            }
            reUploadPackageJsonFile(artifactInfo, packageMetaData)
        }
    }

    private fun PackageVersionDeleteRequest.deleteVersion(packageMetaData: NpmPackageMetaData) {
        packageMetaData.versions.map.remove(version)
        packageMetaData.time.getMap().remove(version)
        val iterator = packageMetaData.distTags.getMap().entries.iterator()
        while (iterator.hasNext()) {
            if (version == iterator.next().value) {
                iterator.remove()
            }
        }
    }

    private fun findNewLatest(request: PackageVersionDeleteRequest): String {
        return with(request) {
            packageClient.findPackageByKey(projectId, repoName, PackageKeys.ofNpm(name)).data?.latest
                ?: run {
                    val message =
                        "delete version by web operator to find new latest version failed with package [$name]"
                    logger.error(message)
                    throw NpmArtifactNotFoundException(message)
                }
        }
    }

    fun reUploadPackageJsonFile(artifactInfo: NpmArtifactInfo, packageMetaData: NpmPackageMetaData) {
        with(artifactInfo) {
            val fullPath = NpmUtils.getPackageMetadataPath(packageMetaData.name!!)
            val inputStream = JsonUtils.objectMapper.writeValueAsString(packageMetaData).byteInputStream()
            val artifactFile = inputStream.use { ArtifactFileFactory.build(it) }
            val context = ArtifactUploadContext(artifactFile)
            context.putAttribute(NPM_FILE_FULL_PATH, fullPath)

            ArtifactContextHolder.getRepository().upload(context).also {
                logger.info(
                    "user [${context.userId}] upload npm package metadata file [$fullPath] " +
                        "to repo [$projectId/$repoName] success."
                )
            }
            artifactFile.delete()
        }
    }

    private fun parseDependencies(versionMetadata: NpmVersionMetadata): MutableList<DependenciesInfo> {
        val dependenciesList: MutableList<DependenciesInfo> = mutableListOf()
        if (versionMetadata.dependencies != null) {
            versionMetadata.dependencies!!.entries.forEach { (key, value) ->
                dependenciesList.add(
                    DependenciesInfo(
                        key,
                        value
                    )
                )
            }
        }
        return dependenciesList
    }

    private fun parseDevDependencies(versionMetadata: NpmVersionMetadata): MutableList<DependenciesInfo> {
        val devDependenciesList: MutableList<DependenciesInfo> = mutableListOf()
        if (versionMetadata.devDependencies != null) {
            versionMetadata.devDependencies!!.entries.forEach { (key, value) ->
                devDependenciesList.add(
                    DependenciesInfo(
                        key,
                        value
                    )
                )
            }
        }
        return devDependenciesList
    }

    companion object {

        val logger: Logger = LoggerFactory.getLogger(NpmWebServiceImpl::class.java)

        fun buildBasicInfo(nodeDetail: NodeDetail, packageVersion: PackageVersion): BasicInfo {
            with(nodeDetail) {
                return BasicInfo(
                    packageVersion.name,
                    fullPath,
                    size,
                    sha256!!,
                    md5!!,
                    packageVersion.stageTag,
                    projectId,
                    repoName,
                    packageVersion.downloads,
                    createdBy,
                    createdDate,
                    lastModifiedBy,
                    lastModifiedDate
                )
            }
        }
    }
}
