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
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.service.RepoSearchService
import com.tencent.devops.artifactory.service.pojo.JFrogAQLFileInfo
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_FILE_NAME
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ArtifactorySearchService @Autowired constructor(
    val jFrogAQLService: JFrogAQLService,
    val pipelineService: PipelineService,
    val artifactoryService: ArtifactoryService
) : RepoSearchService {
    fun search(
        userId: String,
        projectId: String,
        searchProps: SearchProps,
        offset: Int,
        limit: Int
    ): Pair<Long, List<FileInfo>> {
        logger.info("search, userId: $userId, projectId: $projectId, searchProps: $searchProps, offset: $offset, limit: $limit")
        val repoPathPrefix = JFrogUtil.getRepoPath()
        val pipelinePathPrefix = "/" + JFrogUtil.getPipelinePathPrefix(projectId).removePrefix(repoPathPrefix)
        val customDirPathPrefix = "/" + JFrogUtil.getCustomDirPathPrefix(projectId).removePrefix(repoPathPrefix)

        val relativePathSet = setOf(pipelinePathPrefix, customDirPathPrefix)
        val pipelineHasPermissionList = pipelineService.filterPipeline(userId, projectId)

        val fileNameSet = mutableSetOf<String>()
        searchProps.fileNames?.forEach {
            fileNameSet.add(it)
        }

        val props = mutableListOf<Pair<String, String>>()
        searchProps.props.forEach {
            if (it.key == ARCHIVE_PROPS_FILE_NAME) {
                fileNameSet.add(it.value)
            } else {
                props.add(Pair(it.key, it.value))
            }
        }

        val finalOffset = if (limit == -1) null else offset
        val finalLimit = if (limit == -1) null else limit

        val jFrogAQLFileInfoList = jFrogAQLService.searchByProperty(
            repoPathPrefix,
            relativePathSet,
            fileNameSet,
            props,
            finalOffset,
            finalLimit
        )
        val fileInfoList =
            artifactoryService.transferJFrogAQLFileInfo(projectId, jFrogAQLFileInfoList, pipelineHasPermissionList, true)
        return Pair(LocalDateTime.now().timestamp(), fileInfoList)
    }

    fun serviceSearch(
        projectId: String,
        searchProps: List<Property>,
        offset: Int,
        limit: Int
    ): Pair<Long, List<FileInfo>> {
        logger.info("serviceSearch, projectId: $projectId, searchProps: $searchProps, offset: $offset, limit: $limit")
        val repoPathPrefix = JFrogUtil.getRepoPath()
        val pipelinePathPrefix = "/" + JFrogUtil.getPipelinePathPrefix(projectId).removePrefix(repoPathPrefix)
        val customDirPathPrefix = "/" + JFrogUtil.getCustomDirPathPrefix(projectId).removePrefix(repoPathPrefix)
        val relativePathSet = setOf(pipelinePathPrefix, customDirPathPrefix)
        val fileNameSet = mutableSetOf<String>()
        val props = mutableListOf<Pair<String, String>>()
        searchProps.forEach {
            if (it.key == ARCHIVE_PROPS_FILE_NAME) {
                fileNameSet.add(it.value)
            } else {
                props.add(Pair(it.key, it.value))
            }
        }

        val finalOffset = if (limit == -1) null else offset
        val finalLimit = if (limit == -1) null else limit
        val jFrogAQLFileInfoList = jFrogAQLService.searchByProperty(
            repoPathPrefix,
            relativePathSet,
            fileNameSet,
            props,
            finalOffset,
            finalLimit
        )
        val fileInfoList =
            artifactoryService.transferJFrogAQLFileInfo(projectId, jFrogAQLFileInfoList, emptyList(), false)
        return Pair(LocalDateTime.now().timestamp(), fileInfoList)
    }

    override fun searchFileAndProperty(userId: String, projectId: String, searchProps: SearchProps): Pair<Long, List<FileInfo>> {
        logger.info("searchFileAndProperty, userId: $userId, projectId: $projectId, searchProps: $searchProps")
        val repoPathPrefix = JFrogUtil.getRepoPath()
        val pipelinePathPrefix = "/" + JFrogUtil.getPipelinePathPrefix(projectId).removePrefix(repoPathPrefix)
        val customDirPathPrefix = "/" + JFrogUtil.getCustomDirPathPrefix(projectId).removePrefix(repoPathPrefix)

        val relativePathSet = setOf(pipelinePathPrefix, customDirPathPrefix)
        val pipelineHasPermissionList = pipelineService.filterPipeline(userId, projectId)
        val fileNameSet = mutableSetOf<String>()
        searchProps.fileNames?.forEach {
            fileNameSet.add(it)
        }

        val props = mutableListOf<Pair<String, String>>()
        searchProps.props.forEach {
            if (it.key == ARCHIVE_PROPS_FILE_NAME) {
                fileNameSet.add(it.value)
            } else {
                props.add(Pair(it.key, it.value))
            }
        }

        val jFrogAQLFileInfoList =
            jFrogAQLService.searchFileAndPropertyByPropertyByAnd(repoPathPrefix, relativePathSet, fileNameSet, props)
        val fileInfoList =
            artifactoryService.transferJFrogAQLFileInfo(projectId, jFrogAQLFileInfoList, pipelineHasPermissionList)
                .sortedWith(
                    Comparator { file1, file2 -> -file1.modifiedTime.compareTo(file2.modifiedTime) }
                )
        return Pair(LocalDateTime.now().timestamp(), fileInfoList)
    }

    override fun serviceSearchFileByRegex(
        projectId: String,
        pipelineId: String,
        buildId: String,
        regexPath: String,
        customized: Boolean
    ): Pair<Long, List<FileInfo>> {
        logger.info("serviceSearchFileByRegex, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId, regexPath: $regexPath, customized: $customized")
        val repoPathPrefix = JFrogUtil.getRepoPath()
        val pipelinePathPrefix = "/" + JFrogUtil.getPipelinePathPrefix(projectId).removePrefix(repoPathPrefix)
        val customDirPathPrefix = "/" + JFrogUtil.getCustomDirPathPrefix(projectId).removePrefix(repoPathPrefix)

        val fullRegexPath = if (customized) {
            "$customDirPathPrefix${regexPath.removePrefix("/")}"
        } else {
            "$pipelinePathPrefix$pipelineId/$buildId/${regexPath.removePrefix("/")}"
        }

        val fileName = JFrogUtil.getFileName(fullRegexPath)
        val relativePath = fullRegexPath.removeSuffix(fileName)

        val jFrogAQLFileInfoList =
            jFrogAQLService.searchFileByRegex(repoPathPrefix, setOf(relativePath), setOf(fileName))
        val fileInfoList =
            artifactoryService.transferJFrogAQLFileInfo(projectId, jFrogAQLFileInfoList, emptyList(), false).sortedWith(
                Comparator { file1, file2 -> -file1.modifiedTime.compareTo(file2.modifiedTime) }
            )
        return Pair(LocalDateTime.now().timestamp(), fileInfoList)
    }

    override fun serviceSearchFileAndProperty(
        projectId: String,
        searchProps: List<Property>,
        customized: Boolean?,
        generateShortUrl: Boolean
    ): Pair<Long, List<FileInfo>> {
        logger.info("serviceSearchFileAndProperty, projectId: $projectId, searchProps: $searchProps, customized: $customized, generateShortUrl: $generateShortUrl")
        val repoPathPrefix = JFrogUtil.getRepoPath()
        val pipelinePathPrefix = "/" + JFrogUtil.getPipelinePathPrefix(projectId).removePrefix(repoPathPrefix)
        val customDirPathPrefix = "/" + JFrogUtil.getCustomDirPathPrefix(projectId).removePrefix(repoPathPrefix)
        val relativePathSet = when (customized) {
            null -> setOf(pipelinePathPrefix, customDirPathPrefix)
            true -> setOf(customDirPathPrefix)
            false -> setOf(pipelinePathPrefix)
        }

        val fileNameSet = mutableSetOf<String>()
        val props = mutableListOf<Pair<String, String>>()
        searchProps.forEach {
            if (it.key == ARCHIVE_PROPS_FILE_NAME) {
                fileNameSet.add(it.value)
            } else {
                props.add(Pair(it.key, it.value))
            }
        }

        val jFrogAQLFileInfoList = jFrogAQLService.searchFileAndPropertyByPropertyByAnd(repoPathPrefix, relativePathSet, fileNameSet, props)
        val fileInfoList = artifactoryService.transferJFrogAQLFileInfo(projectId, jFrogAQLFileInfoList, emptyList(), generateShortUrl)
            .sortedWith(Comparator { file1, file2 -> -file1.modifiedTime.compareTo(file2.modifiedTime) })
        return Pair(LocalDateTime.now().timestamp(), fileInfoList)
    }

    override fun serviceSearchFileAndPropertyByOr(
        projectId: String,
        searchProps: List<Property>,
        customized: Boolean?
    ): Pair<Long, List<FileInfo>> {
        logger.info("serviceSearchFileAndPropertyByOr, projectId: $projectId, searchProps: $searchProps, customized: $customized")
        val repoPathPrefix = JFrogUtil.getRepoPath()
        val pipelinePathPrefix = "/" + JFrogUtil.getPipelinePathPrefix(projectId).removePrefix(repoPathPrefix)
        val customDirPathPrefix = "/" + JFrogUtil.getCustomDirPathPrefix(projectId).removePrefix(repoPathPrefix)
        val relativePathSet = when (customized) {
            null -> setOf(pipelinePathPrefix, customDirPathPrefix)
            true -> setOf(customDirPathPrefix)
            false -> setOf(pipelinePathPrefix)
        }

        val fileNameSet = mutableSetOf<String>()
        val props = mutableListOf<Pair<String, String>>()
        searchProps.forEach {
            if (it.key == ARCHIVE_PROPS_FILE_NAME) {
                fileNameSet.add(it.value)
            } else {
                props.add(Pair(it.key, it.value))
            }
        }
        val jFrogAQLFileInfoList = jFrogAQLService.searchFileAndPropertyByPropertyByOr(repoPathPrefix, relativePathSet, fileNameSet, props)
        val fileInfoList = artifactoryService.transferJFrogAQLFileInfo(projectId, jFrogAQLFileInfoList, emptyList(), false)
            .sortedWith(Comparator { file1, file2 -> -file1.modifiedTime.compareTo(file2.modifiedTime) })
        return Pair(LocalDateTime.now().timestamp(), fileInfoList)
    }

    override fun getJforgInfoByteewTime(page: Int, pageSize: Int, startTime: Long, endTime: Long): List<FileInfo> {
        logger.info("getJforgInfoByteewTime, page: $page, pageSize: $pageSize, startTime: $startTime, endTime: $endTime")
        val pageNotNull = page ?: 0
        var pageSizeNotNull = pageSize ?: 500
        if (pageSizeNotNull > 500) {
            pageSizeNotNull = 500
        }
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val list = jFrogAQLService.searchFileByTime(startTime, endTime, limit.limit, limit.offset)

        val jfrogInfoMapbyProject: MutableMap<String, MutableList<JFrogAQLFileInfo>> = mutableMapOf()
        list.forEach {
            var projectId: String? = null
            val properties = it.properties
            if (properties != null) {
                properties.forEach { p ->
                    if (p.key.equals("projectId")) {
                        projectId = p.value!!
                    }
                }
                if (!projectId.isNullOrBlank()) {
                    var jfrogInfoList = mutableListOf<JFrogAQLFileInfo>()
                    if (jfrogInfoMapbyProject.get(projectId) != null) {
                        jfrogInfoList = jfrogInfoMapbyProject.get(projectId)!!
                        jfrogInfoList.add(it)
                    } else {
                        jfrogInfoList.add(it)
                        jfrogInfoMapbyProject.put(projectId!!, jfrogInfoList)
                    }
                }
            }
        }
        var fileInfoList: MutableList<FileInfo> = mutableListOf()
        jfrogInfoMapbyProject?.forEach { projectId, list ->
            fileInfoList.addAll(artifactoryService.transferJFrogAQLFileInfo(projectId, list, emptyList(), false))
        }

        return fileInfoList
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}