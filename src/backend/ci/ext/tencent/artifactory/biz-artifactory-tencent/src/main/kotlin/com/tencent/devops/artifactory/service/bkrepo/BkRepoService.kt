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

package com.tencent.devops.artifactory.service.bkrepo

import com.tencent.devops.artifactory.pojo.AppFileInfo
import com.tencent.devops.artifactory.pojo.CopyToCustomReq
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.CustomFileSearchCondition
import com.tencent.devops.artifactory.pojo.DockerUser
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FilePipelineInfo
import com.tencent.devops.artifactory.pojo.FolderSize
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.service.RepoService
import com.tencent.devops.artifactory.service.ShortUrlService
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.artifactory.util.StringUtil
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_NAME
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.archive.pojo.QueryNodeInfo
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import javax.ws.rs.BadRequestException
import javax.ws.rs.NotFoundException

@Service
class BkRepoService @Autowired constructor(
    val pipelineService: PipelineService,
    val bkRepoCustomDirService: BkRepoCustomDirService,
    val bkRepoPipelineDirService: BkRepoPipelineDirService,
    val bkRepoClient: BkRepoClient,
    val commonConfig: CommonConfig,
    val client: Client,
    val shortUrlService: ShortUrlService
) : RepoService {
    override fun list(userId: String, projectId: String, artifactoryType: ArtifactoryType, path: String): List<FileInfo> {
        logger.info("list, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, path: $path")
        return when (artifactoryType) {
            ArtifactoryType.PIPELINE -> {
                bkRepoPipelineDirService.list(userId, projectId, path)
            }
            ArtifactoryType.CUSTOM_DIR -> {
                bkRepoCustomDirService.list(userId, projectId, path)
            }
        }
    }

    override fun show(userId: String, projectId: String, artifactoryType: ArtifactoryType, path: String): FileDetail {
        logger.info("show, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, path: $path")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
        val fileDetail = bkRepoClient.getFileDetail("", projectId, RepoUtils.getRepoByType(artifactoryType), normalizedPath)
            ?: throw NotFoundException("文件不存在")

        return RepoUtils.toFileDetail(fileDetail)
    }

    override fun folderSize(userId: String, projectId: String, artifactoryType: ArtifactoryType, path: String): FolderSize {
        logger.info("folderSize, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, path: $path")
        val sizeInfo = bkRepoClient.getFileSize(userId, projectId, RepoUtils.getRepoByType(artifactoryType), path)
        return FolderSize(sizeInfo.size)
    }

    override fun setDockerProperties(projectId: String, imageName: String, tag: String, properties: Map<String, String>) {
        logger.info("setDockerProperties, projectId: $projectId, imageName: $imageName, String: $String, properties: $properties")
        throw OperationException("not supported")
    }

    override fun setProperties(
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        properties: Map<String, String>
    ) {
        logger.info("setProperties, projectId: $projectId, artifactoryType: $artifactoryType, argPath: $argPath, properties: $properties")
        if (properties.isEmpty()) {
            logger.info("property empty")
            return
        }
        val path = PathUtils.checkAndNormalizeAbsPath(argPath)
        bkRepoClient.setMetadata("admin", projectId, RepoUtils.getRepoByType(artifactoryType), path, properties)
    }

    override fun getProperties(projectId: String, artifactoryType: ArtifactoryType, path: String): List<Property> {
        logger.info("getProperties, projectId: $projectId, artifactoryType: $artifactoryType, path: $path")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
        val matadataMap = bkRepoClient.listMetadata("", projectId, RepoUtils.getRepoByType(artifactoryType), normalizedPath)

        val propertyList = mutableListOf<Property>()
        matadataMap.forEach {
            propertyList.add(Property(it.key, it.value))
        }
        if (matadataMap.containsKey(ARCHIVE_PROPS_PIPELINE_ID)) {
            val pipelineName = pipelineService.getPipelineName(projectId, matadataMap[ARCHIVE_PROPS_PIPELINE_ID]!!)
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
        logger.info("getPropertiesByRegex, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId, " +
            "artifactoryType: $artifactoryType, argPath: $argPath, crossProjectId: $crossProjectId, " +
            "crossPipineId: $crossPipineId, crossBuildNo: $crossBuildNo")

        var targetProjectId = projectId
        var targetPipelineId = pipelineId
        var targetBuildId = buildId
        if (!crossProjectId.isNullOrBlank()) {
            val lastModifyUser = client.get(ServicePipelineResource::class)
                .getPipelineInfo(projectId, pipelineId, null).data!!.lastModifyUser

            targetProjectId = crossProjectId!!
            if (artifactoryType == ArtifactoryType.CUSTOM_DIR && !pipelineService.hasPermission(lastModifyUser, targetProjectId)) {
                throw PermissionForbiddenException("用户（$lastModifyUser) 没有项目（$targetProjectId）下载权限)")
            }
            if (artifactoryType == ArtifactoryType.PIPELINE) {
                targetPipelineId = crossPipineId ?: throw BadRequestException("Invalid Parameter pipelineId")
                pipelineService.validatePermission(
                    userId = lastModifyUser,
                    projectId = targetProjectId,
                    pipelineId = targetPipelineId,
                    permission = AuthPermission.DOWNLOAD,
                    message = "用户($lastModifyUser)在项目($crossProjectId)下没有流水线($crossPipineId)下载构建权限"
                )

                val targetBuild = client.get(ServiceBuildResource::class).getSingleHistoryBuild(
                    projectId = targetProjectId,
                    pipelineId = targetPipelineId,
                    buildNum = crossBuildNo ?: throw BadRequestException("invalid buildNo"),
                    channelCode = ChannelCode.BS
                ).data
                targetBuildId = (targetBuild ?: throw BadRequestException("构建不存在($crossBuildNo)")).id
            }
        }
        logger.info("targetProjectId: $targetProjectId, targetPipelineId: $targetPipelineId, targetBuildId: $targetBuildId")

        val regex = Pattern.compile(",|;")
        val pathArray = regex.split(argPath)

        val resultList = mutableListOf<FileDetail>()
        pathArray.forEach { path ->
            val absPath = "/${JFrogUtil.normalize(path).removePrefix("/")}"
            val filePath = if (artifactoryType == ArtifactoryType.PIPELINE) {
                "/$targetPipelineId/$targetBuildId/${JFrogUtil.getParentFolder(absPath).removePrefix("/")}" // /$projectId/$pipelineId/$buildId/path/
            } else {
                "/${JFrogUtil.getParentFolder(absPath).removePrefix("/")}" // /path/
            }
            val fileName = JFrogUtil.getFileName(path) // *.txt

            bkRepoClient.queryByPathEqOrNameMatchOrMetadataEqAnd(
                userId = "",
                projectId = projectId,
                repoNames = listOf(RepoUtils.getRepoByType(artifactoryType)),
                filePaths = listOf(filePath),
                fileNames = listOf(fileName),
                metadata = mapOf(),
                page = 0,
                pageSize = 10000
            ).forEach {
                resultList.add(RepoUtils.toFileDetail(it))
            }
        }
        return resultList
    }

    override fun getOwnFileList(userId: String, projectId: String, offset: Int, limit: Int): Pair<Long, List<FileInfo>> {
        logger.info("getOwnFileList, userId: $userId, projectId: $projectId, offset: $offset, limit: $limit")
        // not support
        return Pair(LocalDateTime.now().timestamp(), listOf())
    }

    override fun getBuildFileList(userId: String, projectId: String, pipelineId: String, buildId: String): List<AppFileInfo> {
        logger.info("getBuildFileList, userId: $userId, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId")
        pipelineService.validatePermission(userId, projectId, pipelineId, AuthPermission.DOWNLOAD, "用户($userId)在项目($projectId)下没有流水线${pipelineId}下载构建权限")

        val startTimestamp = System.currentTimeMillis()
        try {
            val nodeList = bkRepoClient.queryByNameAndMetadata(
                userId = userId,
                projectId = projectId,
                repoNames = listOf(RepoUtils.PIPELINE_REPO, RepoUtils.CUSTOM_REPO),
                fileNames = listOf(),
                metadata = mapOf(ARCHIVE_PROPS_PIPELINE_ID to pipelineId, ARCHIVE_PROPS_BUILD_ID to buildId),
                page = 0,
                pageSize = 10000
            )

            val fileInfoList = transferFileInfo(projectId, nodeList, listOf(), false)
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
                        it.properties!!.forEach { property ->
                            if (property.key == ARCHIVE_PROPS_PIPELINE_ID && pipelineCanDownloadList.contains(property.value)) {
                                canDownload = true
                                return@checkProperty
                            }
                        }
                    }
                }

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
                    version = it.appVersion
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
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
        val metadataMap = bkRepoClient.listMetadata("", projectId, RepoUtils.getRepoByType(artifactoryType), normalizedPath)
        if (!metadataMap.containsKey(ARCHIVE_PROPS_PIPELINE_ID) || metadataMap[ARCHIVE_PROPS_PIPELINE_ID].isNullOrBlank()) {
            throw RuntimeException("元数据(pipelineId)不存在")
        }
        val pipelineId = metadataMap[ARCHIVE_PROPS_PIPELINE_ID]
        val pipelineName = pipelineService.getPipelineName(projectId, metadataMap[ARCHIVE_PROPS_PIPELINE_ID]!!)

        return FilePipelineInfo(pipelineId!!, pipelineName)
    }

    override fun show(projectId: String, artifactoryType: ArtifactoryType, path: String): FileDetail {
        logger.info("show, projectId: $projectId, artifactoryType: $artifactoryType, path: $path")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
        val fileDetail = bkRepoClient.getFileDetail("", projectId, RepoUtils.getRepoByType(artifactoryType), normalizedPath)
            ?: throw NotFoundException("文件不存在")

        return RepoUtils.toFileDetail(fileDetail)
    }

    override fun check(projectId: String, artifactoryType: ArtifactoryType, path: String): Boolean {
        logger.info("check, projectId: $projectId, artifactoryType: $artifactoryType, path: $path")
        bkRepoClient.getFileDetail("", projectId, RepoUtils.getRepoByType(artifactoryType), path)
            ?: return false
        return true
    }

    fun transferFileInfo(
        projectId: String,
        fileList: List<QueryNodeInfo>,
        pipelineHasPermissionList: List<String>,
        checkPermission: Boolean = true,
        generateShortUrl: Boolean = false
    ): List<FileInfo> {
        val startTimestamp = System.currentTimeMillis()
        try {
            val pipelineIdList = mutableListOf<String>()
            val buildIdList = mutableListOf<String>()
            fileList.forEach {
                if (it.repoName == RepoUtils.PIPELINE_REPO) {
                    pipelineIdList.add(pipelineService.getPipelineId(it.path))
                    buildIdList.add(pipelineService.getBuildId(it.path))
                }
            }
            val pipelineIdToNameMap = pipelineService.getPipelineNames(projectId, pipelineIdList.toSet())
            val buildIdToNameMap = pipelineService.getBuildNames(buildIdList.toSet())

            val fileInfoList = mutableListOf<FileInfo>()
            fileList.forEach {
                var appVersion: String? = null
                val properties: List<Property> = if (it.metadata == null) {
                    listOf()
                } else {
                    it.metadata!!.map { itp ->
                        if (itp.key == "appVersion") {
                            appVersion = itp.value ?: ""
                        }
                        Property(itp.key, itp.value ?: "")
                    }
                }
                if (RepoUtils.isPipelineFile(it)) {
                    val pipelineId = pipelineService.getPipelineId(it.path)
                    val buildId = pipelineService.getBuildId(it.path)
                    val shortUrl = if (generateShortUrl && (it.name.endsWith(".ipa") || it.name.endsWith(".apk"))) {
                        shortUrlService.createShortUrl(PathUtils.buildArchiveLink(projectId, pipelineId, buildId), 300)
                    } else {
                        ""
                    }

                    if ((!checkPermission || pipelineHasPermissionList.contains(pipelineId)) &&
                        pipelineIdToNameMap.containsKey(pipelineId) && buildIdToNameMap.containsKey(buildId)
                    ) {
                        val pipelineName = pipelineIdToNameMap[pipelineId]!!
                        val buildName = buildIdToNameMap[buildId]!!
                        fileInfoList.add(
                            FileInfo(
                                name = it.name,
                                fullName = pipelineService.getFullName(it.fullPath, pipelineId, pipelineName, buildId, buildName),
                                path = it.fullPath,
                                fullPath = it.fullPath,
                                size = it.size,
                                folder = it.folder,
                                modifiedTime = LocalDateTime.parse(it.lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                                artifactoryType = RepoUtils.getTypeByRepo(it.repoName),
                                properties = properties,
                                appVersion = appVersion,
                                shortUrl = shortUrl
                            )
                        )
                    }
                } else {
                    fileInfoList.add(
                        FileInfo(
                            name = it.name,
                            fullName = it.fullPath,
                            path = it.fullPath, // bug?
                            fullPath = it.fullPath,
                            size = it.size,
                            folder = it.folder,
                            modifiedTime = LocalDateTime.parse(it.lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                            artifactoryType = RepoUtils.getTypeByRepo(it.repoName),
                            properties = properties,
                            appVersion = appVersion
                        )
                    )
                }
            }
            return fileInfoList
        } finally {
            logger.info("transferFileInfo cost: ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    override fun createDockerUser(projectCode: String): DockerUser {
        logger.info("createDockerUser, projectCode: $projectCode")
        throw OperationException("not supported")
    }

    override fun listCustomFiles(projectId: String, condition: CustomFileSearchCondition): List<String> {
        logger.info("listCustomFiles, projectId: $projectId, condition: $condition")
        var pathNamePairs = mutableListOf<Pair<String, String>>()
        if (!condition.glob.isNullOrEmpty()) {
            condition.glob!!.split(",").map { globItem ->
                val absPath = "/${JFrogUtil.normalize(globItem).removePrefix("/")}"
                if (absPath.endsWith("/")) {
                    pathNamePairs.add(Pair(absPath, "*"))
                } else {
                    val fileName = absPath.split("/").last()
                    val filePath = absPath.removeSuffix(fileName)
                    pathNamePairs.add(Pair(filePath, fileName))
                }
            }
        }
        val fileList = bkRepoClient.queryByPathNamePairOrMetadataEqAnd(
            userId = "",
            projectId = projectId,
            repoNames = listOf(RepoUtils.CUSTOM_REPO),
            pathNamePairs = pathNamePairs,
            metadata = condition.properties,
            page = 0,
            pageSize = 10000
        )

        return fileList.sortedByDescending { it.lastModifiedDate }.map { it.fullPath }
    }

    override fun copyToCustom(userId: String, projectId: String, pipelineId: String, buildId: String, copyToCustomReq: CopyToCustomReq) {
        logger.info("copyToCustom, userId: $userId, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId, copyToCustomReq: $copyToCustomReq")
        copyToCustomReq.check()
        pipelineService.validatePermission(userId, projectId)

        val pipelineName = pipelineService.getPipelineName(projectId, pipelineId)
        val buildNo = pipelineService.getBuildName(buildId)
        val fromPath = "$pipelineId/$buildId"
        val toPath = "_from_pipeline/$pipelineName/$buildNo"

        copyToCustomReq.files.forEach { file ->
            val fileName = file.removePrefix("/")
            bkRepoClient.copy(
                "",
                projectId,
                RepoUtils.getRepoByType(ArtifactoryType.PIPELINE),
                "$fromPath/$fileName",
                projectId,
                RepoUtils.getRepoByType(ArtifactoryType.CUSTOM_DIR),
                "$toPath/$fileName"
            )
        }
    }

    private fun parsePipelineIdAndBuildId(path: String): Pair<String, String> {
        val splits = path.removePrefix("/").split("/")
        return Pair(splits[0], splits[1])
    }

    override fun acrossProjectCopy(projectId: String, artifactoryType: ArtifactoryType, path: String, targetProjectId: String, targetPath: String): Count {
        logger.info("acrossProjectCopy, projectId: $projectId, artifactoryType: $artifactoryType, path: $path, targetProjectId: $targetProjectId, targetPath: $targetPath")
        var userId = ""
        val absPath = "/${PathUtils.normalize(path).removePrefix("/")}"
        val pathNamePair = if (absPath.endsWith("/")) {
            Pair(absPath, "*")
        } else {
            val fileName = absPath.split("/").last()
            val filePath = absPath.removeSuffix(fileName)
            Pair(filePath, fileName)
        }
        var srcFiles = bkRepoClient.queryByPathNamePairOrMetadataEqAnd(
            userId = userId,
            projectId = projectId,
            repoNames = listOf(RepoUtils.getRepoByType(artifactoryType)),
            pathNamePairs = listOf(pathNamePair),
            metadata = mapOf(),
            page = 0,
            pageSize = 10000
        ).map { it.fullPath }
        logger.info("match files: $srcFiles")

        val destPathFolder = "/share/$projectId/${PathUtils.normalize(targetPath).removePrefix("/")}"
        srcFiles.forEach { srcFile ->
            bkRepoClient.copy(
                userId,
                projectId,
                if (artifactoryType == ArtifactoryType.PIPELINE) RepoUtils.PIPELINE_REPO else RepoUtils.CUSTOM_REPO,
                srcFile,
                targetProjectId,
                RepoUtils.CUSTOM_REPO,
                "$destPathFolder/${File(srcFile).name}"
            )
        }
        return Count(srcFiles.size)
    }

    fun getFileDownloadUrl(param: ArtifactorySearchParam): List<String> {
        return bkRepoClient.getFileDownloadUrl(param)
    }

    fun externalDownloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        fullPath: String,
        ttl: Int
    ): String {
        logger.info("externalDownloadUrl, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, fullPath: $fullPath, ttl: $ttl")
        val shareUri = bkRepoClient.createShareUri(
            userId = userId,
            projectId = projectId,
            repoName = RepoUtils.getRepoByType(artifactoryType),
            fullPath = fullPath,
            downloadUsers = listOf(),
            downloadIps = listOf(),
            timeoutInSeconds = ttl.toLong()
        )
        return StringUtil.chineseUrlEncode("${HomeHostUtil.getHost(commonConfig.devopsOuterHostGateWay!!)}/bkrepo/api/external/repository$shareUri")
    }

    fun internalDownloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int
    ): String {
        logger.info("internalDownloadUrl, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, path: $path, ttl: $ttl")
        val shareUri = bkRepoClient.createShareUri(
            userId = userId,
            projectId = projectId,
            repoName = RepoUtils.getRepoByType(artifactoryType),
            fullPath = path,
            downloadUsers = listOf(),
            downloadIps = listOf(),
            timeoutInSeconds = ttl.toLong()
        )
        return "${HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)}/bkrepo/api/external/repository$shareUri"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
