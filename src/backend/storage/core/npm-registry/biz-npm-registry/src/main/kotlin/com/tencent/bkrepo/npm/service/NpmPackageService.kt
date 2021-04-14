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

package com.tencent.bkrepo.npm.service

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo
import com.tencent.bkrepo.common.artifact.manager.PackageManager
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.artifactStream
import com.tencent.bkrepo.common.artifact.util.http.ArtifactResourceWriter
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.common.artifact.util.version.SemVersion
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.npm.constant.CREATED
import com.tencent.bkrepo.npm.constant.DEFAULT_REV
import com.tencent.bkrepo.npm.constant.LATEST
import com.tencent.bkrepo.npm.constant.MAINTAINERS
import com.tencent.bkrepo.npm.constant.MODIFIED
import com.tencent.bkrepo.npm.constant.NpmProperties
import com.tencent.bkrepo.npm.constant.PACKAGE
import com.tencent.bkrepo.npm.constant.PACKAGE_JSON
import com.tencent.bkrepo.npm.constant.TAG_DEPRECATED
import com.tencent.bkrepo.npm.pojo.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.pojo.artifact.NpmPublishInfo
import com.tencent.bkrepo.npm.pojo.artifact.NpmUpdateInfo
import com.tencent.bkrepo.npm.pojo.metadata.NpmPackageMetadata
import com.tencent.bkrepo.npm.pojo.metadata.NpmVersionMetadata
import com.tencent.bkrepo.npm.pojo.response.NpmRegistrySummary
import com.tencent.bkrepo.npm.pojo.response.NpmResponse
import com.tencent.bkrepo.npm.util.NpmUtils
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.packages.request.PackageUpdateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionUpdateRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Suppress("UNCHECKED_CAST")
@Service
class NpmPackageService(
    private val npmProperties: NpmProperties,
    private val packageClient: PackageClient,
    private val packageManager: PackageManager
) : ArtifactService() {

    fun info(artifactInfo: DefaultArtifactInfo): NpmRegistrySummary {
        with(artifactInfo) {
            val count = packageClient.getPackageCount(projectId, repoName).data ?: 0
            return NpmRegistrySummary(projectId, repoName, count)
        }
    }

    fun listAll(artifactInfo: NpmArtifactInfo): List<Map<String, Any>> {
        with(artifactInfo) {
            return packageClient.listPackagePage(projectId, repoName).data?.records.orEmpty().map {
                mapOf(
                    "name" to it.name,
                    "keywords" to listOf("111", "222"),
                    "version" to "1.0.0",
                    "description" to "description",
                    "author" to mapOf("name" to "author"),
                    "date" to LocalDateTime.now()
                )
            }
            // return packageClient.listAllPackageNames(projectId, repoName).data.orEmpty()
        }
    }

    fun listAllName(artifactInfo: NpmArtifactInfo): List<String> {
        with(artifactInfo) {
            return packageClient.listAllPackageNames(projectId, repoName).data.orEmpty()
        }
    }

    fun listShorts(artifactInfo: NpmArtifactInfo): List<String> {
        with(artifactInfo) {
            return packageClient.listAllPackageNames(projectId, repoName).data.orEmpty()
        }
    }

    fun publish(publishInfo: NpmPublishInfo) {
        val artifactFile = ArtifactFileFactory.build(publishInfo.tarball.inputStream())
        val context = ArtifactUploadContext(artifactFile)
        repository.upload(context)
    }

    fun deprecate(publishInfo: NpmPublishInfo) {
        with(publishInfo) {
            packageMetadata.versions.filterValues { !it.deprecated.isNullOrEmpty() }.forEach { (name, content) ->
                val version = packageManager.findVersionByName(projectId, repoName, packageName, name)
                val versionMetadata = NpmUtils.resolveVersionMetadata(version)
                versionMetadata.deprecated = content.deprecated
                val extension = version.extension.toMutableMap().apply {
                    this[PACKAGE] = versionMetadata.toJsonString()
                }
                val tags = version.tags.toMutableList().apply {
                    add(TAG_DEPRECATED)
                }
                val updateRequest = PackageVersionUpdateRequest(
                    projectId = projectId,
                    repoName = repoName,
                    packageKey = packageName,
                    versionName = name,
                    tags = tags,
                    extension = extension
                )
                packageClient.updateVersion(updateRequest)
                NpmResponse.success()
            }
        }
        val response = HttpContextHolder.getResponse()
        response.status = HttpStatus.CREATED.value
        response.writer.write(NpmResponse.success().toJsonString())
    }

    fun updatePackage(publishInfo: NpmUpdateInfo) {
        with(publishInfo) {
            if (packageMetadata.versions.isNotEmpty()) {
                updateVersions(this)
            } else if (packageMetadata.maintainers != null) {
                updateMaintainers(this)
            }
        }
    }

    private fun updateMaintainers(publishInfo: NpmUpdateInfo) {
        with(publishInfo) {
            val maintainers = packageMetadata.maintainers.orEmpty()
            if (maintainers.isEmpty()) {
                throw PermissionException("Can not remove all maintainers")
            }
            // 更新maintainers
            val packageInfo = packageManager.findPackageByKey(projectId, repoName, packageName)
            val newExtension = packageInfo.extension.toMutableMap().apply {
                this[MAINTAINERS] = maintainers
            }
            // 更新package
            val updateRequest = PackageUpdateRequest(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageName,
                extension = newExtension
            )
            packageClient.updatePackage(updateRequest)
        }
    }

    private fun updateVersions(publishInfo: NpmUpdateInfo) {
        with(publishInfo) {
            val packageInfo = packageManager.findPackageByKey(projectId, repoName, packageName)
            val existedVersions = packageClient.listAllVersion(projectId, repoName, packageName).data.orEmpty()
            if (existedVersions.isEmpty()) {
                return
            }
            val versions = packageMetadata.versions
            // 计算需要移除的版本
            val removeVersions = mutableListOf<String>()
            val removeVersionMaps = mutableMapOf<String, Boolean>()
            val remainVersions = mutableListOf<String>()
            existedVersions.forEach { version ->
                if (!versions.containsKey(version.name)) {
                    removeVersions.add(version.name)
                    removeVersionMaps[version.name] = true
                } else {
                    remainVersions.add(version.name)
                }
            }
            if (removeVersions.isEmpty()) {
                return
            }
            // 寻找需要移除的tag
            val versionTag = packageInfo.versionTag.toMutableMap()
            val removeTags = versionTag.filter { removeVersionMaps.containsKey(it.value) }.map { it.key }
            if (removeTags.isEmpty()) {
                return
            }
            // 移除tag
            removeTags.forEach { versionTag.remove(it) }
            // 生成新的latest
            if (removeTags.contains(LATEST) && remainVersions.isNotEmpty()) {
                versionTag[LATEST] = remainVersions.first()
                logger.debug("latest tag removed, generate with new version: ${remainVersions.first()}")
            }
            // 更新package
            val updateRequest = PackageUpdateRequest(
                projectId = projectId,
                repoName = repoName,
                packageKey = packageName,
                versionTag = versionTag
            )
            packageClient.updatePackage(updateRequest)
        }
    }

    fun download(artifactInfo: NpmArtifactInfo) {
        repository.download(ArtifactDownloadContext())
    }

    fun deleteVersion(artifactInfo: NpmArtifactInfo) {
        repository.remove(ArtifactRemoveContext())
    }

    fun deletePackage(artifactInfo: NpmArtifactInfo) {
        repository.remove(ArtifactRemoveContext())
    }

    fun getPackageMetadata(artifactInfo: NpmArtifactInfo) {
        with(artifactInfo) {
            // 查询包，检查是否存在
            val packageInfo = packageManager.findPackageByKey(projectId, repoName, packageName)
            // 查询所有版本
            val versionList = packageClient.listAllVersion(projectId, repoName, packageName).data.orEmpty()
            // 若版本数量为0，status=404
            if (versionList.isEmpty()) {
                throw NotFoundException(ArtifactMessageCode.PACKAGE_NOT_FOUND, packageName)
            }
            // 解析 startUsers 和 maintainers
            val starUserMap = NpmUtils.resolveStarUsers(packageInfo.extension).associateWith { true }
            val maintainers = NpmUtils.resolveMaintainers(packageInfo.extension)
            // 查询distTag
            val distTag = packageInfo.versionTag
            val latest = distTag[LATEST] ?: packageInfo.latest
            val times = mutableMapOf(
                CREATED to NpmUtils.formatDate(packageInfo.createdDate),
                MODIFIED to NpmUtils.formatDate(packageInfo.lastModifiedDate)
            )
            var readme: String? = null
            var latestVersion: NpmVersionMetadata? = null
            // 7. 遍历version
            val versions = mutableMapOf<String, NpmVersionMetadata>()
            versionList.forEach {
                val versionMetadata = NpmUtils.resolveVersionMetadata(it)
                // 寻找latest的readme作为包的readme
                if (latestVersion == null || latest == it.name) {
                    latestVersion = versionMetadata
                    readme = versionMetadata.readme.orEmpty()
                }
                // 设置TarBall下载地址
                versionMetadata.dist.tarball = generateDownloadUrl(versionMetadata, it.contentPath)
                // 设置version publish_time
                versionMetadata.publishTime = versionMetadata.publishTime ?: NpmUtils.formatDate(it.createdDate)
                // 删除readme
                versionMetadata.readme = null
                // 设置统一maintainer
                versionMetadata.maintainers = maintainers
                versions[versionMetadata.version] = versionMetadata
                times[versionMetadata.version] = versionMetadata.publishTime ?: NpmUtils.formatDate(it.createdDate)
            }

            val npmPackage = NpmPackageMetadata(
                id = packageName,
                rev = DEFAULT_REV,
                name = packageName,
                description = latestVersion?.description,
                distTags = distTag,
                maintainers = maintainers,
                time = times,
                users = starUserMap,
                author = latestVersion?.author,
                repository = latestVersion?.repository,
                versions = versions,
                readme = readme,
                readmeFilename = latestVersion?.readmeFilename,
                homepage = latestVersion?.homepage,
                bugs = latestVersion?.bugs,
                license = latestVersion?.license
            )
            val content = npmPackage.toJsonString().toByteArray()
            val artifactResource = ArtifactResource(
                inputStream = content.inputStream().artifactStream(Range.full(content.size.toLong())),
                artifact = PACKAGE_JSON,
                useDisposition = false
            )
            ArtifactResourceWriter.write(artifactResource)
        }
    }

    /**
     * 根据version、tag查找包
     */
    fun getVersionMetadata(artifactInfo: NpmArtifactInfo) {
        with(artifactInfo) {
            val packageInfo = packageManager.findPackageByKey(projectId, repoName, packageName)
            val tag = if (artifactInfo.version == "*") LATEST else artifactInfo.version
            val versionName = if (!SemVersion.validate(tag)) {
                packageManager.findVersionNameByTag(projectId, repoName, packageName, tag)
            } else tag
            val versionInfo = packageManager.findVersionByName(projectId, repoName, packageName, versionName)
            val versionMetadata = NpmUtils.resolveVersionMetadata(versionInfo)
            // 设置TarBall下载地址
            versionMetadata.dist.tarball = generateDownloadUrl(versionMetadata, versionInfo.contentPath)
            versionMetadata.publishTime = versionMetadata.publishTime ?: NpmUtils.formatDate(versionInfo.createdDate)
            // 解析 startUsers 和 maintainers
            versionMetadata.maintainers = (packageInfo.extension[MAINTAINERS] as MutableList<Map<String, String>>?)
            versionMetadata.distTags = packageInfo.versionTag

            val content = versionMetadata.toJsonString().toByteArray()
            val artifactResource = ArtifactResource(
                inputStream = content.inputStream().artifactStream(Range.full(content.size.toLong())),
                artifact = PACKAGE_JSON,
                useDisposition = false
            )
            ArtifactResourceWriter.write(artifactResource)
        }
    }

    private fun generateDownloadUrl(versionMetadata: NpmVersionMetadata, tarballPath: String?): String {
        return if (npmProperties.tarball.prefix.isNotBlank()) {
            val queryString = HttpContextHolder.getRequest().queryString
            UrlFormatter.format(npmProperties.tarball.prefix, tarballPath, queryString)
        } else versionMetadata.dist.tarball
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NpmPackageService::class.java)
    }
}
