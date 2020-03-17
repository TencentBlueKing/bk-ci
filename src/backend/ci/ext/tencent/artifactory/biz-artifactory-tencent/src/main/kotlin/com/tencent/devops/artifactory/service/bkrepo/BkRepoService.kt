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
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.artifactory.util.StringUtil
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_NAME
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.archive.pojo.QueryNodeInfo
import com.tencent.devops.common.archive.shorturl.ShortUrlApi
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.code.BSRepoAuthServiceCode
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
import java.nio.file.FileSystems
import java.nio.file.Paths
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
    val shortUrlApi: ShortUrlApi,
    val bkRepoClient: BkRepoClient,
    val commonConfig: CommonConfig,
    val authProjectApi: BSAuthProjectApi,
    val client: Client,
    val artifactoryAuthServiceCode: BSRepoAuthServiceCode
) : RepoService {

    override fun list(userId: String, projectId: String, artifactoryType: ArtifactoryType, path: String): List<FileInfo> {
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
            if (artifactoryType == ArtifactoryType.CUSTOM_DIR &&
                !authProjectApi.getProjectUsers(artifactoryAuthServiceCode, targetProjectId).contains(lastModifyUser)) {
                throw BadRequestException("用户（$lastModifyUser) 没有项目（$targetProjectId）下载权限)")
            }
            if (artifactoryType == ArtifactoryType.PIPELINE) {
                targetPipelineId = crossPipineId ?: throw BadRequestException("Invalid Parameter pipelineId")
                pipelineService.validatePermission(
                    lastModifyUser,
                    targetProjectId,
                    targetPipelineId,
                    AuthPermission.DOWNLOAD,
                    "用户($lastModifyUser)在项目($crossProjectId)下没有流水线($crossPipineId)下载构建权限")

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

        val startTimestamp = System.currentTimeMillis()
        try {
            val nodeList = bkRepoClient.queryByNameAndMetadata(
                userId,
                projectId,
                listOf(RepoUtils.PIPELINE_REPO, RepoUtils.CUSTOM_REPO),
                listOf(),
                mapOf(
                    ARCHIVE_PROPS_PIPELINE_ID to pipelineId,
                    ARCHIVE_PROPS_BUILD_ID to buildId
                ),
                0,
                10000
            )

            val fileInfoList = transferFileInfo(projectId, nodeList, listOf(), false)
            val pipelineCanDownloadList = pipelineService.filterPipeline(userId, projectId, AuthPermission.DOWNLOAD)
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
        bkRepoClient.getFileDetail("", projectId, RepoUtils.getRepoByType(ArtifactoryType.CUSTOM_DIR), path)
            ?: return false
        return true
    }

    fun transferFileInfo(
        projectId: String,
        fileList: List<QueryNodeInfo>,
        pipelineHasPermissionList: List<String>,
        checkPermission: Boolean = true
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
                val properties = it.metadata.map { itp ->
                    if (itp.key == "appVersion") {
                        appVersion = itp.value ?: ""
                    }
                    Property(itp.key, itp.value ?: "")
                }
                if (RepoUtils.isPipelineFile(it)) {
                    val pipelineId = pipelineService.getPipelineId(it.path)
                    val buildId = pipelineService.getBuildId(it.path)
                    val shortUrl = if (it.name.endsWith(".ipa") || it.name.endsWith(".apk")) {
                        val url = "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=buildArchive&projectId=$projectId&pipelineId=$pipelineId&buildId=$buildId"
                        shortUrlApi.getShortUrl(url, 300)
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
        throw OperationException("Not Supported")
    }

    override fun listCustomFiles(projectId: String, condition: CustomFileSearchCondition): List<String> {
        logger.info("listCustomFiles, projectId: $projectId, condition: $condition")
        val allFiles = bkRepoClient.queryByNameAndMetadata(
            "",
            projectId,
            listOf(RepoUtils.CUSTOM_REPO),
            listOf(),
            condition.properties,
            0,
            30000
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
        val matchedFiles = mutableListOf<QueryNodeInfo>()
        matchers.forEach { matcher ->
            allFiles.forEach {
                if (matcher.matches(Paths.get(it.path.removePrefix("/")))) {
                    matchedFiles.add(it)
                }
            }
        }

        return matchedFiles.toSet().toList().sortedByDescending { it.lastModifiedDate }.map { it.fullPath }
    }

    override fun copyToCustom(userId: String, projectId: String, pipelineId: String, buildId: String, copyToCustomReq: CopyToCustomReq) {
        if (copyToCustomReq.files.isEmpty()) {
            throw OperationException("invalid request")
        }

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

    private fun parsePipeineIdAndBuildId(path: String): Pair<String, String> {
        val splits = path.removePrefix("/").split("/")
        return Pair(splits[0], splits[1])
    }

    override fun acrossProjectCopy(projectId: String, artifactoryType: ArtifactoryType, path: String, targetProjectId: String, targetPath: String): Count {
        logger.info("acrossProjectCopy, projectId: $projectId, artifactoryType: $artifactoryType, path: $path, targetProjectId: $targetProjectId, targetPath: $targetPath")
        val normalizeSrcPath = PathUtils.normalize(path)
        val srcFiles = if (artifactoryType == ArtifactoryType.PIPELINE) {
            val pipeineIdAndBuildId = parsePipeineIdAndBuildId(normalizeSrcPath)
            val pipelineId = pipeineIdAndBuildId.first
            val buildId = pipeineIdAndBuildId.second
            val pathPrefix = "/$pipelineId/$buildId/"
            bkRepoClient.listFileByRegex(
                "",
                projectId,
                RepoUtils.PIPELINE_REPO,
                pathPrefix,
                normalizeSrcPath.removePrefix(pathPrefix)
            ).map { it.fullPath }
        } else {
            bkRepoClient.listFileByRegex(
                "",
                projectId,
                RepoUtils.CUSTOM_REPO,
                "/",
                normalizeSrcPath.removePrefix("/")
            ).map { it.fullPath }
        }
        logger.info("match files: $srcFiles")

        val destPathFolder = "/share/$projectId/${PathUtils.normalize(targetPath).removePrefix("/")}"
        srcFiles.forEach { srcFile ->
            bkRepoClient.copy(
                "",
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
