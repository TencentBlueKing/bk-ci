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

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.util.JsonUtils.objectMapper
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.handler.NpmDependentHandler
import com.tencent.bkrepo.npm.handler.NpmPackageHandler
import com.tencent.bkrepo.npm.constants.ATTRIBUTE_OCTET_STREAM_SHA1
import com.tencent.bkrepo.npm.constants.CREATED
import com.tencent.bkrepo.npm.constants.LATEST
import com.tencent.bkrepo.npm.constants.MODIFIED
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_PACKAGE_TGZ_FILE
import com.tencent.bkrepo.npm.constants.SEARCH_REQUEST
import com.tencent.bkrepo.npm.constants.SIZE
import com.tencent.bkrepo.npm.exception.NpmArtifactExistException
import com.tencent.bkrepo.npm.exception.NpmArtifactNotFoundException
import com.tencent.bkrepo.npm.exception.NpmBadRequestException
import com.tencent.bkrepo.npm.exception.NpmTagNotExistException
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.model.metadata.NpmVersionMetadata
import com.tencent.bkrepo.npm.pojo.NpmSearchInfoMap
import com.tencent.bkrepo.npm.pojo.NpmSearchResponse
import com.tencent.bkrepo.npm.pojo.NpmSuccessResponse
import com.tencent.bkrepo.npm.model.properties.PackageProperties
import com.tencent.bkrepo.npm.pojo.enums.NpmOperationAction
import com.tencent.bkrepo.npm.pojo.metadata.MetadataSearchRequest
import com.tencent.bkrepo.npm.pojo.metadata.disttags.DistTags
import com.tencent.bkrepo.npm.properties.NpmProperties
import com.tencent.bkrepo.npm.service.NpmClientService
import com.tencent.bkrepo.npm.utils.BeanUtils
import com.tencent.bkrepo.npm.utils.NpmUtils
import com.tencent.bkrepo.npm.utils.TimeUtil
import com.tencent.bkrepo.repository.api.MetadataClient
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.io.InputStream
import kotlin.system.measureTimeMillis

@Service
class NpmClientServiceImpl(
    private val npmDependentHandler: NpmDependentHandler,
    private val metadataClient: MetadataClient,
    private val npmPackageHandler: NpmPackageHandler,
    private val npmProperties: NpmProperties
) : NpmClientService, AbstractNpmService() {

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    @Transactional(rollbackFor = [Throwable::class])
    override fun publishOrUpdatePackage(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        name: String
    ): NpmSuccessResponse {
        try {
            val npmPackageMetaData =
                objectMapper.readValue(HttpContextHolder.getRequest().inputStream, NpmPackageMetaData::class.java)
            when {
                isUploadRequest(npmPackageMetaData) -> {
                    measureTimeMillis {
                        handlerPackagePublish(userId, artifactInfo, npmPackageMetaData)
                    }.apply {
                        logger.info(
                            "user [$userId] public npm package [$name] " +
                                "to repo [${artifactInfo.getRepoIdentify()}] success, elapse $this ms"
                        )
                    }
                    return NpmSuccessResponse.createEntitySuccess()
                }
                isDeprecateRequest(npmPackageMetaData) -> {
                    handlerPackageDeprecated(userId, artifactInfo, npmPackageMetaData)
                    return NpmSuccessResponse.updatePkgSuccess()
                }
                else -> {
                    val message = "Unknown npm put/update request, check the debug logs for further information."
                    logger.warn(message)
                    logger.debug(
                        "Unknown npm put/update request: {}",
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(npmPackageMetaData)
                    )
                    // 异常声明为npm模块的异常
                    throw NpmBadRequestException(message)
                }
            }
        } catch (exception: IOException) {
            logger.error("Exception while reading package metadata: ${exception.message}")
            throw exception
        }
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    @Transactional(rollbackFor = [Throwable::class])
    override fun packageInfo(artifactInfo: NpmArtifactInfo, name: String): NpmPackageMetaData {
        with(artifactInfo) {
            logger.info("handling query package metadata request for package [$name] in repo [$projectId/$repoName]")
            return queryPackageInfo(artifactInfo, name)
        }
    }

    private fun queryPackageInfo(
        artifactInfo: NpmArtifactInfo,
        name: String,
        showCustomTarball: Boolean = true
    ): NpmPackageMetaData {
        val packageFullPath = NpmUtils.getPackageMetadataPath(name)
        val context = ArtifactQueryContext()
        context.putAttribute(NPM_FILE_FULL_PATH, packageFullPath)
        val inputStream =
            ArtifactContextHolder.getRepository().query(context) as? InputStream
                ?: throw NpmArtifactNotFoundException("document not found")
        val packageMetaData = inputStream.use { objectMapper.readValue(it, NpmPackageMetaData::class.java) }
        if (showCustomTarball) {
            val versionsMap = packageMetaData.versions.map
            val iterator = versionsMap.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                modifyVersionMetadataTarball(artifactInfo, name, entry.value)
            }
        }
        return packageMetaData
    }

    private fun modifyVersionMetadataTarball(
        artifactInfo: NpmArtifactInfo,
        name: String,
        versionMetadata: NpmVersionMetadata
    ) {
        val oldTarball = versionMetadata.dist?.tarball!!
        versionMetadata.dist?.tarball =
            NpmUtils.buildPackageTgzTarball(oldTarball, npmProperties.tarball.prefix, name, artifactInfo)
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    @Transactional(rollbackFor = [Throwable::class])
    override fun packageVersionInfo(artifactInfo: NpmArtifactInfo, name: String, version: String): NpmVersionMetadata {
        with(artifactInfo) {
            logger.info(
                "handling query package version metadata request for package [$name] " +
                    "and version [$version] in repo [$projectId/$repoName]"
            )
            if (StringUtils.equals(version, LATEST)) {
                return searchLatestVersionMetadata(artifactInfo, name)
            }
            return searchVersionMetadata(artifactInfo, name, version)
        }
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    @Transactional(rollbackFor = [Throwable::class])
    override fun download(artifactInfo: NpmArtifactInfo) {
        val context = ArtifactDownloadContext()
        context.putAttribute(NPM_FILE_FULL_PATH, context.artifactInfo.getArtifactFullPath())
        ArtifactContextHolder.getRepository().download(context)
    }

    @Suppress("UNCHECKED_CAST")
    @Permission(ResourceType.REPO, PermissionAction.READ)
    @Transactional(rollbackFor = [Throwable::class])
    override fun search(artifactInfo: NpmArtifactInfo, searchRequest: MetadataSearchRequest): NpmSearchResponse {
        val context = ArtifactSearchContext()
        context.putAttribute(SEARCH_REQUEST, searchRequest)
        val npmSearchInfoMapList = ArtifactContextHolder.getRepository().search(context) as List<NpmSearchInfoMap>
        return NpmSearchResponse(npmSearchInfoMapList)
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    override fun getDistTags(artifactInfo: NpmArtifactInfo, name: String): DistTags {
        with(artifactInfo) {
            logger.info("handling get distTags request for package [$name] in repo [$projectId/$repoName]")
            val packageMetaData = queryPackageInfo(artifactInfo, name, false)
            return packageMetaData.distTags.getMap()
        }
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    override fun addDistTags(userId: String, artifactInfo: NpmArtifactInfo, name: String, tag: String) {
        logger.info(
            "handling add distTags [$tag] request for package [$name] " +
                "in repo [${artifactInfo.getRepoIdentify()}]"
        )
        val packageMetaData = queryPackageInfo(artifactInfo, name, false)
        val version = objectMapper.readValue(HttpContextHolder.getRequest().inputStream, String::class.java)
        if ((LATEST == tag && packageMetaData.versions.map.containsKey(version)) || LATEST != tag) {
            packageMetaData.distTags.getMap()[tag] = version
            doPackageFileUpload(userId, artifactInfo, packageMetaData)
        }
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    override fun deleteDistTags(userId: String, artifactInfo: NpmArtifactInfo, name: String, tag: String) {
        logger.info(
            "handling delete distTags [$tag] request for package [$name] " +
                "in repo [${artifactInfo.getRepoIdentify()}]"
        )
        if (LATEST == tag) {
            logger.warn(
                "dist tag for [latest] with package [$name] " +
                    "in repo [${artifactInfo.getRepoIdentify()}] cannot be deleted."
            )
            return
        }
        val packageMetaData = queryPackageInfo(artifactInfo, name, false)
        packageMetaData.distTags.getMap().remove(tag)
        doPackageFileUpload(userId, artifactInfo, packageMetaData)
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    override fun updatePackage(userId: String, artifactInfo: NpmArtifactInfo, name: String) {
        logger.info("handling update package request for package [$name] in repo [${artifactInfo.getRepoIdentify()}]")
        val packageMetadata =
            objectMapper.readValue(HttpContextHolder.getRequest().inputStream, NpmPackageMetaData::class.java)
        doPackageFileUpload(userId, artifactInfo, packageMetadata)
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    override fun deleteVersion(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        name: String,
        version: String,
        tgzPath: String
    ) {
        logger.info("handling delete version [$version] request for package [$name]")
        val fullPathList = mutableListOf<String>()
        with(artifactInfo) {
            if (tgzPath.isEmpty() || !exist(projectId, repoName, tgzPath)) {
                throw NpmArtifactNotFoundException("can not find version [$version] for package [$name]")
            }
            fullPathList.add(tgzPath)
            fullPathList.add(NpmUtils.getVersionPackageMetadataPath(name, version))
            val context = ArtifactRemoveContext()
            context.putAttribute(NPM_FILE_FULL_PATH, fullPathList)
            ArtifactContextHolder.getRepository().remove(context)
            logger.info("userId [$userId] delete version [$version] for package [$name] success.")
            // 删除包管理中对应的version
            npmPackageHandler.deleteVersion(userId, name, version, artifactInfo)
        }
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    override fun deletePackage(userId: String, artifactInfo: NpmArtifactInfo, name: String) {
        logger.info("handling delete package request for package [$name]")
        val fullPathList = mutableListOf<String>()
        val packageMetaData = queryPackageInfo(artifactInfo, name, false)
        fullPathList.add(".npm/$name")
        fullPathList.add(name)
        val context = ArtifactRemoveContext()
        context.putAttribute(NPM_FILE_FULL_PATH, fullPathList)
        ArtifactContextHolder.getRepository().remove(context).also {
            logger.info("userId [$userId] delete package [$name] success.")
        }
        npmDependentHandler.updatePackageDependents(userId, artifactInfo, packageMetaData, NpmOperationAction.UNPUBLISH)
        npmPackageHandler.deletePackage(userId, name, artifactInfo)
    }

    private fun searchLatestVersionMetadata(artifactInfo: NpmArtifactInfo, name: String): NpmVersionMetadata {
        logger.info("handling query latest version metadata request for package [$name]")
        try {
            val context = ArtifactQueryContext()
            val packageFullPath = NpmUtils.getPackageMetadataPath(name)
            context.putAttribute(NPM_FILE_FULL_PATH, packageFullPath)
            val inputStream =
                ArtifactContextHolder.getRepository().query(context) as? InputStream
                    ?: throw NpmArtifactNotFoundException("document not found")
            val npmPackageMetaData = inputStream.use { objectMapper.readValue(it, NpmPackageMetaData::class.java) }
            val distTags = npmPackageMetaData.distTags
            if (!distTags.getMap().containsKey(LATEST)) {
                val message =
                    "the dist tag [latest] is not found in package [$name] in repo [${artifactInfo.getRepoIdentify()}]"
                logger.error(message)
                throw NpmTagNotExistException(message)
            }
            val latestVersion = distTags.getMap()[LATEST]!!
            return searchVersionMetadata(artifactInfo, name, latestVersion)
        } catch (exception: IOException) {
            val message = "Unable to get npm metadata for package $name and version latest"
            logger.error(message)
            throw NpmBadRequestException(message)
        }
    }

    private fun searchVersionMetadata(
        artifactInfo: NpmArtifactInfo,
        name: String,
        version: String
    ): NpmVersionMetadata {
        try {
            val context = ArtifactQueryContext()
            val packageFullPath = NpmUtils.getVersionPackageMetadataPath(name, version)
            context.putAttribute(NPM_FILE_FULL_PATH, packageFullPath)
            val inputStream =
                ArtifactContextHolder.getRepository().query(context) as? InputStream
                    ?: throw NpmArtifactNotFoundException("document not found")
            val versionMetadata = inputStream.use { objectMapper.readValue(it, NpmVersionMetadata::class.java) }
            modifyVersionMetadataTarball(artifactInfo, name, versionMetadata)
            return versionMetadata
        } catch (exception: IOException) {
            val message = "Unable to get npm metadata for package $name and version $version"
            logger.error(message)
            throw NpmBadRequestException(message)
        }
    }

    private fun handlerPackagePublish(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        npmPackageMetaData: NpmPackageMetaData
    ) {
        val attachments = npmPackageMetaData.attachments
        attachments ?: run {
            val message = "Missing attachments with tarball data, aborting upload for '${npmPackageMetaData.name}'"
            logger.warn(message)
            throw NpmBadRequestException(message)
        }
        try {
            val size = attachments.getMap().values.iterator().next().length!!.toLong()
            handlerAttachmentsUpload(userId, artifactInfo, npmPackageMetaData)
            handlerPackageFileUpload(userId, artifactInfo, npmPackageMetaData, size)
            handlerVersionFileUpload(userId, artifactInfo, npmPackageMetaData, size)
            npmDependentHandler.updatePackageDependents(
                userId,
                artifactInfo,
                npmPackageMetaData,
                NpmOperationAction.PUBLISH
            )
            val versionMetadata = npmPackageMetaData.versions.map.values.iterator().next()
            npmPackageHandler.createVersion(userId, artifactInfo, versionMetadata, size)
        } catch (exception: IOException) {
            val version = NpmUtils.getLatestVersionFormDistTags(npmPackageMetaData.distTags)
            logger.error(
                "userId [$userId] publish package [${npmPackageMetaData.name}] for version [$version] " +
                    "to repo [${artifactInfo.projectId}/${artifactInfo.repoName}] failed."
            )
        }
    }

    private fun handlerPackageFileUpload(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        npmPackageMetaData: NpmPackageMetaData,
        size: Long
    ) {
        with(artifactInfo) {
            val packageFullPath = NpmUtils.getPackageMetadataPath(npmPackageMetaData.name!!)
            val gmtTime = TimeUtil.getGMTTime()
            val npmMetadata = npmPackageMetaData.versions.map.values.iterator().next()
            if (!npmMetadata.dist!!.any().containsKey(SIZE)) {
                npmMetadata.dist!!.set(SIZE, size)
            }
            // 第一次上传
            if (!exist(projectId, repoName, packageFullPath)) {
                npmPackageMetaData.time.add(CREATED, gmtTime)
                npmPackageMetaData.time.add(MODIFIED, gmtTime)
                npmPackageMetaData.time.add(npmMetadata.version!!, gmtTime)
                doPackageFileUpload(userId, artifactInfo, npmPackageMetaData)
                return
            }
            val originalPackageInfo = queryPackageInfo(artifactInfo, npmPackageMetaData.name!!, false)
            originalPackageInfo.versions.map.putAll(npmPackageMetaData.versions.map)
            originalPackageInfo.distTags.getMap().putAll(npmPackageMetaData.distTags.getMap())
            originalPackageInfo.time.add(MODIFIED, gmtTime)
            originalPackageInfo.time.add(npmMetadata.version!!, gmtTime)
            doPackageFileUpload(userId, artifactInfo, originalPackageInfo)
        }
    }

    private fun doPackageFileUpload(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        npmPackageMetaData: NpmPackageMetaData
    ) {
        with(artifactInfo) {
            val fullPath = NpmUtils.getPackageMetadataPath(npmPackageMetaData.name!!)
            val inputStream = objectMapper.writeValueAsString(npmPackageMetaData).byteInputStream()
            val artifactFile = inputStream.use { ArtifactFileFactory.build(it) }
            val context = ArtifactUploadContext(artifactFile)
            context.putAttribute(NPM_FILE_FULL_PATH, fullPath)
            ArtifactContextHolder.getRepository().upload(context).also {
                logger.info(
                    "user [$userId] upload npm package metadata file [$fullPath] " +
                        "into repo [$projectId/$repoName] success."
                )
            }
            artifactFile.delete()
        }
    }

    private fun handlerVersionFileUpload(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        npmPackageMetaData: NpmPackageMetaData,
        size: Long
    ) {
        with(artifactInfo) {
            val npmMetadata = npmPackageMetaData.versions.map.values.iterator().next()
            if (!npmMetadata.dist!!.any().containsKey(SIZE)) {
                npmMetadata.dist!!.set(SIZE, size)
            }
            val fullPath = NpmUtils.getVersionPackageMetadataPath(npmMetadata.name!!, npmMetadata.version!!)
            val inputStream = objectMapper.writeValueAsString(npmMetadata).byteInputStream()
            val artifactFile = inputStream.use { ArtifactFileFactory.build(it) }
            val context = ArtifactUploadContext(artifactFile)
            context.putAttribute(NPM_FILE_FULL_PATH, fullPath)
            context.putAttribute(ATTRIBUTE_OCTET_STREAM_SHA1, npmMetadata.dist?.shasum!!)
            ArtifactContextHolder.getRepository().upload(context).also {
                logger.info(
                    "user [$userId] upload npm package version metadata file [$fullPath] " +
                        "into repo [$projectId/$repoName] success."
                )
            }
            artifactFile.delete()
        }
    }

    private fun handlerAttachmentsUpload(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        npmPackageMetaData: NpmPackageMetaData
    ) {
        val attachmentEntry = npmPackageMetaData.attachments!!.getMap().entries.iterator().next()
        val versionMetadata = npmPackageMetaData.versions.map.values.iterator().next()
        val filename = attachmentEntry.key
        val fullPath = "${versionMetadata.name}/-/$filename"
        val withDownloadFullPath = "${versionMetadata.name}/download/$filename"
        with(artifactInfo) {
            if (exist(projectId, repoName, fullPath) || exist(projectId, repoName, withDownloadFullPath)) {
                throw NpmArtifactExistException(
                    "You cannot publish over the previously published versions: ${versionMetadata.version}."
                )
            }
            logger.info("user [$userId] deploying npm package [$fullPath] into repo [$projectId/$repoName]")
            try {
                val inputStream = tgzContentToInputStream(attachmentEntry.value.data!!)
                val artifactFile = inputStream.use { ArtifactFileFactory.build(it) }
                val context = ArtifactUploadContext(artifactFile)
                context.putAttribute(NPM_FILE_FULL_PATH, fullPath)
                context.putAttribute("attachments.content_type", attachmentEntry.value.contentType!!)
                context.putAttribute("attachments.length", attachmentEntry.value.length!!)
                context.putAttribute("name", NPM_PACKAGE_TGZ_FILE)
                // context.putAttribute(NPM_METADATA, buildProperties(versionMetadata))
                // 将attachments移除
                npmPackageMetaData.attachments = null
                ArtifactContextHolder.getRepository().upload(context)
                artifactFile.delete()
            } catch (exception: IOException) {
                logger.error(
                    "Failed deploying npm package [$fullPath] into repo [$projectId/$repoName] due to : $exception"
                )
            }
        }
    }

    private fun buildProperties(npmVersionMetadata: NpmVersionMetadata?): Map<String, String> {
        return npmVersionMetadata?.let {
            val npmProperties = PackageProperties(
                it.license,
                it.keywords,
                it.name!!,
                it.version!!,
                it.maintainers,
                it.any()["deprecated"] as? String
            )
            BeanUtils.beanToMap(npmProperties)
        } ?: emptyMap()
    }

    private fun tgzContentToInputStream(data: String): InputStream {
        return Base64.decodeBase64(data).inputStream()
    }

    private fun handlerPackageDeprecated(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        npmPackageMetaData: NpmPackageMetaData
    ) {
        logger.info(
            "userId [$userId] handler deprecated request: [$npmPackageMetaData] " +
                "in repo [${artifactInfo.projectId}]"
        )
        doPackageFileUpload(userId, artifactInfo, npmPackageMetaData)
        // 元数据增加过期信息
        val iterator = npmPackageMetaData.versions.map.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val tgzFullPath = NpmUtils.getTgzPath(npmPackageMetaData.name!!, entry.key)
            if (entry.value.any().containsKey("deprecated")) {
                metadataClient.saveMetadata(
                    MetadataSaveRequest(
                        artifactInfo.projectId,
                        artifactInfo.repoName,
                        tgzFullPath,
                        buildProperties(entry.value),
                        userId
                    )
                )
            }
        }
    }

    companion object {

        fun isUploadRequest(npmPackageMetaData: NpmPackageMetaData): Boolean {
            val attachments = npmPackageMetaData.attachments
            return attachments != null && attachments.getMap().entries.isNotEmpty()
        }

        fun isDeprecateRequest(npmPackageMetaData: NpmPackageMetaData): Boolean {
            val versions = npmPackageMetaData.versions
            val iterator = versions.map.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val npmMetadata = entry.value
                if (npmMetadata.any().containsKey("deprecated")) {
                    return true
                }
            }

            return false
        }

        private val logger: Logger = LoggerFactory.getLogger(NpmClientServiceImpl::class.java)
    }
}
