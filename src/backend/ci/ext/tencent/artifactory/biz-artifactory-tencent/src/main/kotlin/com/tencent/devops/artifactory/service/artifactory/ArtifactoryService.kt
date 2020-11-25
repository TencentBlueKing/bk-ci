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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.artifactory.service.artifactory

import com.tencent.devops.artifactory.client.JFrogAQLService
import com.tencent.devops.artifactory.client.JFrogApiService
import com.tencent.devops.artifactory.pojo.AppFileInfo
import com.tencent.devops.artifactory.pojo.CopyToCustomReq
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.CustomFileSearchCondition
import com.tencent.devops.artifactory.pojo.DockerUser
import com.tencent.devops.artifactory.pojo.FileChecksums
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FilePipelineInfo
import com.tencent.devops.artifactory.pojo.FolderSize
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.JFrogService
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.service.RepoService
import com.tencent.devops.artifactory.service.ShortUrlService
import com.tencent.devops.artifactory.service.pojo.JFrogAQLFileInfo
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.api.JFrogPropertiesApi
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_NAME
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PROJECT_ID
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BkAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import javax.ws.rs.BadRequestException

@Service
class ArtifactoryService @Autowired constructor(
    private val pipelineService: PipelineService,
    private val jFrogApiService: JFrogApiService,
    private val jFrogAQLService: JFrogAQLService,
    private val jFrogService: JFrogService,
    private val artifactoryPipelineDirService: ArtifactoryPipelineDirService,
    private val artifactoryCustomDirService: ArtifactoryCustomDirService,
    private val jFrogPropertiesApi: JFrogPropertiesApi,
    private val client: Client,
    private val shortUrlService: ShortUrlService
) : RepoService {
    // 待下线
    fun hasDownloadPermission(
        userId: String,
        projectId: String,
        serviceCode: BkAuthServiceCode,
        resourceType: AuthResourceType,
        path: String
    ): Boolean {
        return if (serviceCode == BkAuthServiceCode.PIPELINE && resourceType == AuthResourceType.PIPELINE_DEFAULT) {
            val pipelineId = pipelineService.getPipelineId(path)
            pipelineService.hasPermission(userId, projectId, pipelineId, AuthPermission.EXECUTE)
        } else {
            false
        }
    }

    override fun list(userId: String, projectId: String, artifactoryType: ArtifactoryType, path: String): List<FileInfo> {
        logger.info("list, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, path: $path")
        return when (artifactoryType) {
            ArtifactoryType.PIPELINE -> {
                artifactoryPipelineDirService.list(userId, projectId, path)
            }
            ArtifactoryType.CUSTOM_DIR -> {
                artifactoryCustomDirService.list(userId, projectId, path)
            }
        }
    }

    override fun show(userId: String, projectId: String, artifactoryType: ArtifactoryType, path: String): FileDetail {
        logger.info("show, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, path: $path")
        return when (artifactoryType) {
            ArtifactoryType.PIPELINE -> {
                artifactoryPipelineDirService.show(userId, projectId, path)
            }
            ArtifactoryType.CUSTOM_DIR -> {
                artifactoryCustomDirService.show(userId, projectId, path)
            }
        }
    }

    override fun folderSize(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String): FolderSize {
        logger.info("folderSize, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, argPath: $argPath")
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        if (!jFrogService.exist(realPath)) {
            logger.error("Path $path not exist")
            throw BadRequestException("文件夹($path)不存在")
        }
        return FolderSize(jFrogApiService.folderCount(realPath))
    }

    override fun setDockerProperties(projectId: String, imageName: String, tag: String, properties: Map<String, String>) {
        logger.info("setDockerProperties, projectId: $projectId, imageName: $imageName, tag: $tag, properties: $properties")
        if (properties.isEmpty()) {
            return
        }

        val path = JFrogUtil.normalize("$imageName/$tag")

        val realPath = JFrogUtil.getDockerRealPath(projectId, path)
        val propertiesMap = mutableMapOf<String, List<String>>()
        properties.forEach {
            propertiesMap[it.key] = listOf(it.value)
        }
        setDockerPropertiesImpl(realPath, propertiesMap)
    }

    private fun setDockerPropertiesImpl(path: String, properties: Map<String, List<String>>) {
        for (i in 0 until 20) {
            var success = true
            try {
                jFrogPropertiesApi.setProperties(path, properties)
            } catch (e: RuntimeException) {
                success = false
                Thread.sleep(30 * 1000L)
            }
            if (success) {
                return
            }
        }
    }

    override fun setProperties(
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        properties: Map<String, String>
    ) {
        logger.info("setProperties, projectId: $projectId, artifactoryType: $artifactoryType, argPath: $argPath, properties: $properties")
        if (properties.isEmpty()) {
            return
        }

        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }
        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        val propertiesMap = mutableMapOf<String, List<String>>()
        properties.forEach {
            propertiesMap[it.key] = listOf(it.value)
        }
        jFrogPropertiesApi.setProperties(realPath, propertiesMap)
    }

    override fun getProperties(projectId: String, artifactoryType: ArtifactoryType, argPath: String): List<Property> {
        logger.info("getProperties, projectId: $projectId, artifactoryType: $artifactoryType, argPath: $argPath")
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        val jFrogProperties = jFrogPropertiesApi.getProperties(realPath)
        val propertyList = mutableListOf<Property>()
        jFrogProperties.forEach {
            propertyList.add(Property(it.key, it.value.joinToString(",")))
        }
        if (jFrogProperties.containsKey(ARCHIVE_PROPS_PIPELINE_ID)) {
            val pipelineId = jFrogProperties[ARCHIVE_PROPS_PIPELINE_ID]!!.first()
            val pipelineName = pipelineService.getPipelineName(projectId, pipelineId)
            propertyList.add(Property(ARCHIVE_PROPS_PIPELINE_NAME, pipelineName))
        }
        return propertyList
    }

    override fun getPropertiesByRegex(
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        crossProjectId: String?,
        crossPipineId: String?,
        crossBuildNo: String?
    ): List<FileDetail> {
        logger.info("getPropertiesByRegex, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId" +
            ", artifactoryType: $artifactoryType, argPath: $argPath, crossProjectId: $crossProjectId, crossPipineId: $crossPipineId" +
            ", crossBuildNo: $crossBuildNo")
        var targetProjectId = projectId
        var targetPipelineId = pipelineId
        var targetBuildId = buildId
        if (!crossProjectId.isNullOrBlank()) {
            val lastModifyUser = client.get(ServicePipelineResource::class)
                .getPipelineInfo(projectId, pipelineId, null).data!!.lastModifyUser

            targetProjectId = crossProjectId!!
            if (artifactoryType == ArtifactoryType.CUSTOM_DIR && !pipelineService.hasPermission(lastModifyUser, targetProjectId)) {
                throw BadRequestException("用户（$lastModifyUser) 没有项目（$targetProjectId）下载权限)")
            }
            if (artifactoryType == ArtifactoryType.PIPELINE) {
                targetPipelineId = crossPipineId ?: throw BadRequestException("invalid pipelineId")
                pipelineService.validatePermission(lastModifyUser, targetProjectId, targetPipelineId, AuthPermission.DOWNLOAD, "用户($lastModifyUser)在项目($crossProjectId)下没有流水线($crossPipineId)下载构建权限")
                val targetBuild = client.get(ServiceBuildResource::class).getSingleHistoryBuild(
                    targetProjectId,
                    targetPipelineId,
                    crossBuildNo ?: throw BadRequestException("Invalid Parameter buildNo"),
                    ChannelCode.BS
                ).data
                targetBuildId = (targetBuild ?: throw BadRequestException("构建不存在($crossBuildNo)")).id
            }
        }
        logger.info("targetProjectId: $targetProjectId, targetPipelineId: $targetPipelineId, targetBuildId: $targetBuildId")

        val regex = Pattern.compile(",|;")
        val pathArray = regex.split(argPath)

        val repoPathPrefix = JFrogUtil.getRepoPath() // "generic-local/"
        val pipelinePathPrefix = "/" + JFrogUtil.getPipelinePathPrefix(targetProjectId).removePrefix(repoPathPrefix) // "bk-archive/$projectId/"
        val customDirPathPrefix = "/" + JFrogUtil.getCustomDirPathPrefix(targetProjectId).removePrefix(repoPathPrefix) // // "bk-custom/$projectId/"
        val ret = mutableListOf<FileDetail>()

        pathArray.forEach { path ->
            val normalizedPath = JFrogUtil.normalize(path)
            val realPath = if (path.startsWith("/")) normalizedPath else "/$normalizedPath" // /path/*.txt
            val pathPrefix = if (artifactoryType == ArtifactoryType.PIPELINE) {
                "/" + JFrogUtil.getPipelinePathPrefix(targetProjectId).removePrefix(repoPathPrefix) + "$targetPipelineId/$targetBuildId/" + JFrogUtil.getParentFolder(
                    realPath
                ).removePrefix("/") // bk-archive/$projectId/$pipelineId/$buildId/path/
            } else {
                "/" + JFrogUtil.getCustomDirPathPrefix(targetProjectId).removePrefix(repoPathPrefix) + JFrogUtil.getParentFolder(
                    realPath
                ).removePrefix("/") // bk-archive/$projectId/path/
            }
            val fileName = JFrogUtil.getFileName(path) // *.txt

            val jFrogAQLFileInfoList = jFrogAQLService.searchFileByRegex(repoPathPrefix, setOf(pathPrefix), setOf(fileName))
            logger.info("Path($path) match file list: $jFrogAQLFileInfoList")

            jFrogAQLFileInfoList.forEach {
                val pathTemp = if (it.path.startsWith(pipelinePathPrefix)) {
                    "/" + it.path.removePrefix(pipelinePathPrefix)
                } else {
                    "/" + it.path.removePrefix(customDirPathPrefix)
                }
                ret.add(show(targetProjectId, artifactoryType, pathTemp))
            }
        }
        return ret
    }

    override fun getOwnFileList(userId: String, projectId: String, offset: Int, limit: Int): Pair<Long, List<FileInfo>> {
        val startTimestamp = System.currentTimeMillis()

        try {
            val repoPathPrefix = JFrogUtil.getRepoPath()
            val pipelinePathPrefix = "/" + JFrogUtil.getPipelinePathPrefix(projectId).removePrefix(repoPathPrefix)
            val customDirPathPrefix = "/" + JFrogUtil.getCustomDirPathPrefix(projectId).removePrefix(repoPathPrefix)

            val relativePathSet = setOf(pipelinePathPrefix, customDirPathPrefix)
            val pipelineHasPermissionList = pipelineService.filterPipeline(userId, projectId)

            val jFrogAQLFileInfoList =
                jFrogAQLService.listByCreateTimeDesc(repoPathPrefix, relativePathSet, offset, limit)
            val fileInfoList = transferJFrogAQLFileInfo(projectId, jFrogAQLFileInfoList, pipelineHasPermissionList)

            return Pair(LocalDateTime.now().timestamp(), fileInfoList)
        } finally {
            logger.info("getOwnFileList cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    override fun getBuildFileList(userId: String, projectId: String, pipelineId: String, buildId: String): List<AppFileInfo> {
        logger.info("getBuildFileList, userId: $userId, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId")
        val startTimestamp = System.currentTimeMillis()

        try {
            val repoPathPrefix = JFrogUtil.getRepoPath()
            val pipelinePathPrefix = "/" + JFrogUtil.getPipelinePathPrefix(projectId).removePrefix(repoPathPrefix)
            val customDirPathPrefix = "/" + JFrogUtil.getCustomDirPathPrefix(projectId).removePrefix(repoPathPrefix)

            val relativePathSet = setOf(pipelinePathPrefix, customDirPathPrefix)

            val props = listOf(
                Pair(ARCHIVE_PROPS_PIPELINE_ID, pipelineId),
                Pair(ARCHIVE_PROPS_BUILD_ID, buildId)
            )

            val jFrogAQLFileInfoList =
                jFrogAQLService.searchFileAndPropertyByPropertyByAnd(repoPathPrefix, relativePathSet, emptySet(), props)
            val fileInfoList = transferJFrogAQLFileInfo(projectId, jFrogAQLFileInfoList, emptyList(), false)
            val pipelineCanDownloadList = pipelineService.filterPipeline(userId, projectId)
            return fileInfoList.map {
                val show = when {
                    it.name.endsWith(".apk") && !it.name.endsWith(".shell.apk") -> {
                        val shellFileName = "${it.path.removeSuffix(".apk")}.shell.apk"
                        var flag = true
                        fileInfoList.forEach { file ->
                            if (file.path == shellFileName) {
                                flag = false
                            }
                        }
                        flag
                    }
                    it.name.endsWith(".shell.apk") -> {
                        true
                    }
                    it.name.endsWith(".ipa") && !it.name.endsWith("_enterprise_sign.ipa") -> {
                        val enterpriseSignFileName = "${it.path.removeSuffix(".ipa")}_enterprise_sign.ipa"
                        var flag = true
                        fileInfoList.forEach { file ->
                            if (file.path == enterpriseSignFileName) {
                                flag = false
                            }
                        }
                        flag
                    }
                    it.name.endsWith("_enterprise_sign.ipa") -> {
                        true
                    }
                    else -> {
                        false
                    }
                }

                var canDownload = false
                if (it.properties != null) {
                    kotlin.run checkProperty@{
                        it.properties!!.forEach {
                            if (it.key == ARCHIVE_PROPS_PIPELINE_ID && pipelineCanDownloadList.contains(it.value)) {
                                canDownload = true
                                return@checkProperty
                            }
                        }
                    }
                }

                var appVersion: String? = null
                appVersion = it.appVersion

                AppFileInfo(
                    name = it.name,
                    fullName = it.fullName,
                    path = it.path,
                    fullPath = it.fullPath,
                    size = it.size,
                    folder = it.folder,
                    modifiedTime = it.modifiedTime,
                    artifactoryType = it.artifactoryType,
                    show = show,
                    canDownload = canDownload,
                    version = appVersion
                )
            }
        } finally {
            logger.info("getBuildFileList cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    override fun getFilePipelineInfo(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): FilePipelineInfo {
        logger.info("getFilePipelineInfo, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, path: $path")
        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        val properties = jFrogPropertiesApi.getProperties(realPath)
        if (!properties.containsKey(ARCHIVE_PROPS_PIPELINE_ID)) {
            throw RuntimeException("元数据(pipelineId)不存在")
        }
        val pipelineId = properties[ARCHIVE_PROPS_PIPELINE_ID]!!.first()
        val pipelineName = pipelineService.getPipelineName(projectId, pipelineId)

        return FilePipelineInfo(pipelineId, pipelineName)
    }

    override fun show(projectId: String, artifactoryType: ArtifactoryType, path: String): FileDetail {
        logger.info("show, projectId: $projectId, artifactoryType: $artifactoryType, path: $path")
        val realPath = if (artifactoryType == ArtifactoryType.PIPELINE) {
            JFrogUtil.getPipelinePath(projectId, path)
        } else {
            JFrogUtil.getCustomDirPath(projectId, path)
        }

        val jFrogFileInfo = jFrogService.file(realPath)
        val jFrogProperties = jFrogPropertiesApi.getProperties(realPath)
        val jFrogPropertiesMap = mutableMapOf<String, String>()
        jFrogProperties.forEach {
            jFrogPropertiesMap[it.key] = it.value.joinToString(",")
        }
        if (jFrogProperties.containsKey(ARCHIVE_PROPS_PIPELINE_ID)) {
            val pipelineId = jFrogProperties[ARCHIVE_PROPS_PIPELINE_ID]!!.first()
            val pipelineName = pipelineService.getPipelineName(projectId, pipelineId)
            jFrogPropertiesMap[ARCHIVE_PROPS_PIPELINE_NAME] = pipelineName
        }
        val checksums = jFrogFileInfo.checksums
        return if (checksums == null) {
            FileDetail(
                JFrogUtil.getFileName(path),
                path,
                path,
                path,
                jFrogFileInfo.size,
                LocalDateTime.parse(jFrogFileInfo.created, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                LocalDateTime.parse(jFrogFileInfo.lastModified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                FileChecksums("", "", ""),
                jFrogPropertiesMap
            )
        } else {
            FileDetail(
                JFrogUtil.getFileName(path),
                path,
                path,
                path,
                jFrogFileInfo.size,
                LocalDateTime.parse(jFrogFileInfo.created, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                LocalDateTime.parse(jFrogFileInfo.lastModified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                FileChecksums(
                    checksums.sha256,
                    checksums.sha1,
                    checksums.md5
                ),
                jFrogPropertiesMap
            )
        }
    }

    override fun check(projectId: String, artifactoryType: ArtifactoryType, path: String): Boolean {
        logger.info("check, projectId: $projectId, artifactoryType: $artifactoryType, path: $path")
        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        return jFrogService.exist(realPath)
    }

    override fun acrossProjectCopy(
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        targetProjectId: String,
        targetPath: String
    ): Count {
        logger.info("acrossProjectCopy, projectId: $projectId, artifactoryType: $artifactoryType, path: $path, targetProjectId: $targetProjectId, targetPath: $targetPath")
        val normalizePath = JFrogUtil.normalize(path)
        val normalizeTargetPath = JFrogUtil.normalize(targetPath)
        val destPathFolder =
            JFrogUtil.getCustomDirPath(targetProjectId, "/share/$projectId/${normalizeTargetPath.removePrefix("/")}")

        val repoPathPrefix = JFrogUtil.getRepoPath()
        val pathPrefix = if (artifactoryType == ArtifactoryType.PIPELINE) {
            "/" + JFrogUtil.getPipelinePathPrefix(projectId).removePrefix(repoPathPrefix) + JFrogUtil.getParentFolder(
                normalizePath
            ).removePrefix("/")
        } else {
            "/" + JFrogUtil.getCustomDirPathPrefix(projectId).removePrefix(repoPathPrefix) + JFrogUtil.getParentFolder(
                normalizePath
            ).removePrefix("/")
        }
        val fileName = JFrogUtil.getFileName(normalizePath)

        val jFrogFileInfoList =
            jFrogAQLService.searchByProperty(repoPathPrefix, setOf(pathPrefix), setOf(fileName), emptyList())
        logger.info("across project copy match: $jFrogFileInfoList")
        val fileInfoList = transferJFrogAQLFileInfo(projectId, jFrogFileInfoList, emptyList(), false)
        fileInfoList.forEach {
            val sourcePath = if (it.artifactoryType == ArtifactoryType.PIPELINE) {
                JFrogUtil.getPipelinePath(projectId, it.fullPath)
            } else {
                JFrogUtil.getCustomDirPath(projectId, it.fullPath)
            }
            val destPath = "${destPathFolder.removeSuffix("/")}/${it.name}"
            jFrogService.copy(sourcePath, destPath)

            // 删除原先的元数据
            jFrogPropertiesApi.deleteProperties(
                destPath,
                listOf(ARCHIVE_PROPS_PROJECT_ID, ARCHIVE_PROPS_PIPELINE_ID, ARCHIVE_PROPS_BUILD_ID)
            )
            jFrogPropertiesApi.setProperties(
                destPath,
                mapOf(ARCHIVE_PROPS_PROJECT_ID to listOf(targetProjectId))
            )
        }

        return Count(fileInfoList.size)
    }

    fun transferJFrogAQLFileInfo(
        projectId: String,
        jFrogAQLFileInfoList: List<JFrogAQLFileInfo>,
        pipelineHasPermissionList: List<String>,
        checkPermission: Boolean = true,
        generateShortUrl: Boolean = false
    ): List<FileInfo> {
        val startTimestamp = System.currentTimeMillis()

        try {
            val repoPathPrefix = JFrogUtil.getRepoPath()
            val pipelinePathPrefix = "/" + JFrogUtil.getPipelinePathPrefix(projectId).removePrefix(repoPathPrefix)
            val customDirPathPrefix = "/" + JFrogUtil.getCustomDirPathPrefix(projectId).removePrefix(repoPathPrefix)

            val pipelineIdList = mutableListOf<String>()
            val buildIdList = mutableListOf<String>()
            jFrogAQLFileInfoList.forEach {
                if (it.path.startsWith(pipelinePathPrefix)) {
                    val path = "/" + it.path.removePrefix(pipelinePathPrefix)
                    pipelineIdList.add(pipelineService.getPipelineId(path))
                    buildIdList.add(pipelineService.getBuildId(path))
                }
            }

            val pipelineIdToNameMap = pipelineService.getPipelineNames(projectId, pipelineIdList.toSet())
            val buildIdToNameMap = pipelineService.getBuildNames(buildIdList.toSet())
            val fileInfoList = mutableListOf<FileInfo>()
            jFrogAQLFileInfoList.forEach {
                var appVersion: String? = null
                val properties = it.properties!!.map { itp ->
                    if (itp.key == "appVersion") {
                        appVersion = itp.value ?: ""
                    }
                    Property(itp.key, itp.value ?: "")
                }

                if (it.path.startsWith(pipelinePathPrefix)) {
                    val path = "/" + it.path.removePrefix(pipelinePathPrefix)
                    val pipelineId = pipelineService.getPipelineId(path)
                    val buildId = pipelineService.getBuildId(path)

                    if ((!checkPermission || pipelineHasPermissionList.contains(pipelineId)) &&
                        pipelineIdToNameMap.containsKey(pipelineId) && buildIdToNameMap.containsKey(buildId)
                    ) {
                        val shortUrl = if (generateShortUrl && (it.name.endsWith(".ipa") || it.name.endsWith(".apk"))) {
                            shortUrlService.createShortUrl(PathUtils.buildArchiveLink(projectId, pipelineId, buildId), 300)
                        } else {
                            ""
                        }
                        val pipelineName = pipelineIdToNameMap[pipelineId]!!
                        val buildName = buildIdToNameMap[buildId]!!
                        val fullName = pipelineService.getFullName(path, pipelineId, pipelineName, buildId, buildName)
                        fileInfoList.add(
                            FileInfo(
                                name = it.name,
                                fullName = fullName,
                                path = path,
                                fullPath = path,
                                size = it.size,
                                folder = false,
                                modifiedTime = LocalDateTime.parse(it.modified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                                artifactoryType = ArtifactoryType.PIPELINE,
                                properties = properties,
                                appVersion = appVersion,
                                shortUrl = shortUrl
                            )
                        )
                    }
                } else {
                    val path = "/" + it.path.removePrefix(customDirPathPrefix)
                    fileInfoList.add(
                        FileInfo(
                            name = it.name,
                            fullName = path,
                            path = path,
                            fullPath = path,
                            size = it.size,
                            folder = false,
                            modifiedTime = LocalDateTime.parse(it.modified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                            artifactoryType = ArtifactoryType.CUSTOM_DIR,
                            properties = properties,
                            appVersion = appVersion
                        )
                    )
                }
            }

            return fileInfoList
        } finally {
            logger.info("transferJFrogAQLFileInfo cost: ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    override fun createDockerUser(projectCode: String): DockerUser {
        logger.info("createDockerUser, projectCode: $projectCode")
        return jFrogApiService.createDockerUser(projectCode)
    }

    override fun listCustomFiles(projectId: String, condition: CustomFileSearchCondition): List<String> {
        logger.info("listCustomFiles, projectId: $projectId, condition: $condition")
        val allFiles = jFrogAQLService.searchByPathAndProperties(
            path = "generic-local/bk-custom/$projectId",
            properties = condition.properties
        )

        if (condition.glob.isNullOrEmpty()) {
            return allFiles.map { it.path }
        }

        val globs = condition.glob!!.split(",").map {
            it.trim().removePrefix("/").removePrefix("./")
        }.filter { it.isNotEmpty() }
        val matchers = globs.map {
            FileSystems.getDefault().getPathMatcher("glob:$it")
        }
        val matchedFiles = mutableListOf<JFrogAQLFileInfo>()
        matchers.forEach { matcher ->
            allFiles.forEach {
                if (matcher.matches(Paths.get(it.path.removePrefix("/")))) {
                    matchedFiles.add(it)
                }
            }
        }

        return matchedFiles.toSet().toList().sortedByDescending { it.modified }.map { it.path }
    }

    override fun copyToCustom(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        copyToCustomReq: CopyToCustomReq
    ) {
        logger.info("copyToCustom, userId: $userId, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId, copyToCustomReq: $copyToCustomReq")
        copyToCustomReq.check()
        pipelineService.validatePermission(userId, projectId)

        val pipelineName = pipelineService.getPipelineName(projectId, pipelineId)
        val buildNo = pipelineService.getBuildName(buildId)
        val fromPath = JFrogUtil.getPipelineBuildPath(projectId, pipelineId, buildId)
        val toPath = JFrogUtil.getPipelineToCustomPath(projectId, pipelineName, buildNo)
        if (copyToCustomReq.copyAll) {
            jFrogService.tryDelete(toPath)
            jFrogService.copy(fromPath, toPath)
        } else {
            copyToCustomReq.files.forEach { file ->
                val fileName = file.removePrefix("/")
                val fromFilePath = "$fromPath/$fileName"
                jFrogService.file(fromFilePath)
                jFrogService.copy("$fromPath/$fileName", "$toPath/$fileName")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactoryService::class.java)
    }
}