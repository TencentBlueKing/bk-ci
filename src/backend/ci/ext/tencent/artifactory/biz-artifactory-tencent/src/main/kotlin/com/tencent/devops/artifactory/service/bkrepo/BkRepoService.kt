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

import com.tencent.bkrepo.repository.pojo.node.NodeInfo
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
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_NAME
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.archive.shorturl.ShortUrlApi
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.ws.rs.NotFoundException

@Service
class BkRepoService @Autowired constructor(
    val pipelineService: PipelineService,
    val shortUrlApi: ShortUrlApi,
    val bkRepoClient: BkRepoClient
) : RepoService {

    override fun list(userId: String, projectId: String, artifactoryType: ArtifactoryType, path: String): List<FileInfo> {
        logger.info("list, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, path: $path")
        return bkRepoClient.listFile(userId, projectId, RepoUtils.getRepoByType(artifactoryType), path, includeFolders = true, deep = false).map {
            RepoUtils.toFileInfo(it)
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
        // todo 实现
        throw OperationException("not implemented")
    }

    override fun getOwnFileList(userId: String, projectId: String, offset: Int, limit: Int): Pair<Long, List<FileInfo>> {
        logger.info("getOwnFileList, userId: $userId, projectId: $projectId, offset: $offset, limit: $limit")
        // not support
        return Pair(LocalDateTime.now().timestamp(), listOf())
    }

    override fun getBuildFileList(userId: String, projectId: String, pipelineId: String, buildId: String): List<AppFileInfo> {
        logger.info("getBuildFileList, userId: $userId, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId")
        // todo 实现
        return listOf()
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
        fileList: List<NodeInfo>,
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

                //                var appVersion: String? = null
//                val properties = it.properties!!.map { itp ->
//                    if (itp.key == "appVersion") {
//                        appVersion = itp.value ?: ""
//                    }
//                    Property(itp.key, itp.value ?: "")
//                }
                if (RepoUtils.isPipelineFile(it)) {
                    val pipelineId = pipelineService.getPipelineId(it.path)
                    val buildId = pipelineService.getBuildId(it.path)
//                    外部URL?
//                    val url =
//                        "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=buildArchive&projectId=$projectId&pipelineId=$pipelineId&buildId=$buildId"
//                    val shortUrl = shortUrlApi.getShortUrl(url, 300)

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
                                properties = listOf() //
                                // appVersion?
                                // shortUrl?
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
                            properties = listOf() //
                            // appVersion?
                        )
                    )
                }
            }
            return fileInfoList
        } finally {
            logger.info("transferJFrogAQLFileInfo cost: ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    override fun createDockerUser(projectId: String): DockerUser {
        logger.info("createDockerUser, projectId: $projectId")
        throw OperationException("Not Supported")
    }

    override fun listCustomFiles(projectId: String, condition: CustomFileSearchCondition): List<String> {
        logger.info("listCustomFiles, projectId: $projectId, condition: $condition")
        val allFiles = bkRepoClient.searchFile(
            "",
            projectId,
            listOf(RepoUtils.CUSTOM_REPO),
            listOf(),
            condition.properties,
            0,
            10000
        ).records

        if (condition.glob.isNullOrEmpty()) {
            return allFiles.map { it.path }
        }

        val globs = condition.glob!!.split(",").map {
            it.trim().removePrefix("/").removePrefix("./")
        }.filter { it.isNotEmpty() }
        val matchers = globs.map {
            FileSystems.getDefault().getPathMatcher("glob:$it")
        }
        val matchedFiles = mutableListOf<NodeInfo>()
        matchers.forEach { matcher ->
            allFiles.forEach {
                if (matcher.matches(Paths.get(it.path.removePrefix("/")))) {
                    matchedFiles.add(it)
                }
            }
        }

        return matchedFiles.toSet().toList().sortedByDescending { it.lastModifiedDate }.map { it.path }
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

    override fun acrossProjectCopy(projectId: String, artifactoryType: ArtifactoryType, path: String, targetProjectId: String, targetPath: String): Count {
        bkRepoClient.copy(
            "",
            projectId,
            RepoUtils.getRepoByType(artifactoryType),
            path,
            targetProjectId,
            RepoUtils.getRepoByType(artifactoryType),
            targetPath
        )
        return Count(-1) // todo 返回拷贝文件个数
    }

    fun getFileDownloadUrl(param: ArtifactorySearchParam): List<String> {
        logger.info("getFileDownloadUrl, param: $param")
        // todo
        throw OperationException("not implemented")
    }

    fun externalDownloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int,
        directed: Boolean
    ): String {
        logger.info("externalDownloadUrl, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, " +
            "path: $path, ttl: $ttl, directed: $directed")

        return bkRepoClient.externalDownloadUrl(
            userId = userId,
            projectId = projectId,
            repoName = RepoUtils.getRepoByType(artifactoryType),
            path = path,
            downloadUser = userId,
            ttl = ttl,
            directed = directed
        )
    }

    fun internalDownloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int,
        directed: Boolean
    ): String {
        logger.info("internalDownloadUrl, userId: $userId, projectId: $projectId, artifactoryType: $artifactoryType, " +
            "path: $path, ttl: $ttl, directed: $directed")
        // todo
        throw OperationException("not implemented")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}