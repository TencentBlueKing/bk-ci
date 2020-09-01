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

import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.service.RepoSearchService
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_FILE_NAME
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BkRepoSearchService @Autowired constructor(
    val bkRepoClient: BkRepoClient,
    val pipelineService: PipelineService,
    val bkRepoService: BkRepoService
) : RepoSearchService {
    fun search(
        userId: String,
        projectId: String,
        searchProps: SearchProps,
        page: Int,
        pageSize: Int
    ): Pair<Long, List<FileInfo>> {
        logger.info("search, userId: $userId, projectId: $projectId, searchProps: $searchProps, page: $page, pageSize: $pageSize")
        val fileNameSet = mutableSetOf<String>()
        searchProps.fileNames?.forEach {
            fileNameSet.add(it)
        }

        // 属性为 fileName 要按文件名搜索？
        val props = mutableListOf<Pair<String, String>>()
        searchProps.props.forEach {
            if (it.key == ARCHIVE_PROPS_FILE_NAME) {
                fileNameSet.add(it.value)
            } else {
                props.add(Pair(it.key, it.value))
            }
        }

        val nodeList = bkRepoClient.queryByNameAndMetadata(
            userId,
            projectId,
            listOf(RepoUtils.PIPELINE_REPO, RepoUtils.CUSTOM_REPO),
            fileNameSet.toList(),
            props.associate { it },
            page,
            pageSize
        )

        val pipelineHasPermissionList = pipelineService.filterPipeline(userId, projectId)
        val fileInfoList = bkRepoService.transferFileInfo(projectId, nodeList, pipelineHasPermissionList)
        return Pair(LocalDateTime.now().timestamp(), fileInfoList)
    }

    fun serviceSearch(
        projectId: String,
        searchProps: List<Property>,
        page: Int,
        pageSize: Int
    ): Pair<Long, List<FileInfo>> {
        logger.info("serviceSearch, projectId: $projectId, searchProps: $searchProps")
        // 属性为 fileName 要按文件名搜索？
        val fileNameSet = mutableSetOf<String>()
        val props = mutableListOf<Pair<String, String>>()
        searchProps.forEach {
            if (it.key == ARCHIVE_PROPS_FILE_NAME) {
                fileNameSet.add(it.value)
            } else {
                props.add(Pair(it.key, it.value))
            }
        }

        val nodeList = bkRepoClient.queryByNameAndMetadata(
            "",
            projectId,
            listOf(RepoUtils.PIPELINE_REPO, RepoUtils.CUSTOM_REPO),
            fileNameSet.toList(),
            props.associate { it },
            page,
            pageSize
        )

        val fileInfoList = bkRepoService.transferFileInfo(projectId, nodeList, emptyList(), false)
        return Pair(LocalDateTime.now().timestamp(), fileInfoList)
    }

    override fun searchFileAndProperty(userId: String, projectId: String, searchProps: SearchProps): Pair<Long, List<FileInfo>> {
        logger.info("searchFileAndProperty, projectId: $projectId, searchProps: $searchProps")
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

        val nodeList = bkRepoClient.queryByNameAndMetadata(
            "",
            projectId,
            listOf(RepoUtils.PIPELINE_REPO, RepoUtils.CUSTOM_REPO),
            fileNameSet.toList(),
            props.associate { it },
            0,
            10000
        )

        val pipelineHasPermissionList = pipelineService.filterPipeline(userId, projectId)
        val fileInfoList = bkRepoService.transferFileInfo(projectId, nodeList, pipelineHasPermissionList)
            .sortedWith(Comparator { file1, file2 -> -file1.modifiedTime.compareTo(file2.modifiedTime) })
        return Pair(LocalDateTime.now().timestamp(), fileInfoList)
    }

    override fun serviceSearchFileByRegex(
        projectId: String,
        pipelineId: String,
        buildId: String,
        regexPath: String,
        customized: Boolean
    ): Pair<Long, List<FileInfo>> {
        logger.info("serviceSearchFileByRegex, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId, regexPath: $regexPath")
        var queryPath = regexPath
        if (!regexPath.startsWith("/")) {
            queryPath = "/$queryPath"
        }

        val nodeList = bkRepoClient.queryByPattern(
            "",
            projectId,
            if (customized) listOf(RepoUtils.CUSTOM_REPO) else listOf(RepoUtils.PIPELINE_REPO),
            listOf(queryPath),
            mapOf()
        )

        val fileInfoList = bkRepoService.transferFileInfo(projectId, nodeList, emptyList(), false)
        return Pair(LocalDateTime.now().timestamp(), fileInfoList)
    }

    override fun serviceSearchFileAndProperty(
        projectId: String,
        searchProps: List<Property>,
        customized: Boolean?,
        generateShortUrl: Boolean
    ): Pair<Long, List<FileInfo>> {
        logger.info("serviceSearchFileAndProperty, projectId: $projectId, searchProps: $searchProps, customized: $customized")
        val repoNames = when (customized) {
            null -> listOf(RepoUtils.PIPELINE_REPO, RepoUtils.CUSTOM_REPO)
            true -> listOf(RepoUtils.CUSTOM_REPO)
            false -> listOf(RepoUtils.PIPELINE_REPO)
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

        val nodeList = bkRepoClient.queryByNameAndMetadata(
            "",
            projectId,
            repoNames,
            fileNameSet.toList(),
            props.associate { it },
            0,
            10000
        )

        val fileInfoList = bkRepoService.transferFileInfo(projectId, nodeList, emptyList(), false, generateShortUrl)
        return Pair(LocalDateTime.now().timestamp(), fileInfoList)
    }

    override fun serviceSearchFileAndPropertyByOr(
        projectId: String,
        searchProps: List<Property>,
        customized: Boolean?
    ): Pair<Long, List<FileInfo>> {
        return serviceSearchFileAndProperty(projectId, searchProps, customized)
    }

    override fun getJforgInfoByteewTime(page: Int, pageSize: Int, startTime: Long, endTime: Long): List<FileInfo> {
        throw OperationException("not supported")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}