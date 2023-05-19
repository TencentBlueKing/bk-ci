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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.constant.REPO_NAME_CUSTOM
import com.tencent.devops.artifactory.constant.REPO_NAME_PIPELINE
import com.tencent.devops.artifactory.dao.FileDao
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.artifactory.util.BkRepoUtils
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.archive.client.BkRepoClient
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service@Suppress("ALL")
class SamplePipelineBuildArtifactoryService @Autowired constructor(
    private val dslContext: DSLContext,
    private val fileDao: FileDao,
    private val bkRepoClient: BkRepoClient
) : PipelineBuildArtifactoryService {

    override fun getArtifactList(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<FileInfo> {
        return if (bkRepoClient.useBkRepo()) {
            getBkRepoArtifactoryList(userId, projectId, pipelineId, buildId)
        } else {
            getLocalArtifactList(projectId, pipelineId, buildId)
        }
    }

    private fun getBkRepoArtifactoryList(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<FileInfo> {
        logger.info("getBkRepoArtifactoryList, userId: $userId, projectId: $projectId, pipelineId: $pipelineId, " +
            "buildId: $buildId")
        val nodeList = bkRepoClient.queryByNameAndMetadata(
            userId = userId,
            projectId = projectId,
            repoNames = listOf(REPO_NAME_PIPELINE, REPO_NAME_CUSTOM),
            fileNames = listOf(),
            metadata = mapOf("pipelineId" to pipelineId, "buildId" to buildId),
            page = 1,
            pageSize = 1000
        ).records
        return nodeList.map { FileInfo(
            name = it.name,
            fullName = it.name,
            path = it.path,
            fullPath = it.fullPath,
            size = it.size,
            folder = it.folder,
            properties = it.metadata?.map { m -> Property(m.key, m.value.toString()) },
            modifiedTime = LocalDateTime.parse(it.lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
            artifactoryType = BkRepoUtils.parseArtifactoryType(it.repoName)
        ) }
    }

    private fun getLocalArtifactList(projectId: String, pipelineId: String, buildId: String): List<FileInfo> {
        logger.info("getLocalArtifactList, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId")
        val props = mapOf(
            "pipelineId" to pipelineId,
            "buildId" to buildId
        )
        val fileTypeList = listOf(FileTypeEnum.BK_ARCHIVE.fileType, FileTypeEnum.BK_CUSTOM.fileType)
        val fileInfoRecords = fileDao.getFileListByProps(dslContext, projectId, fileTypeList, props, null, null)
        val fileInfoList = mutableListOf<FileInfo>()
        fileInfoRecords?.forEach {
            var artifactoryType = ArtifactoryType.PIPELINE
            if (it["fileType"] == FileTypeEnum.BK_CUSTOM.fileType) {
                artifactoryType = ArtifactoryType.CUSTOM_DIR
            }
            fileInfoList.add(
                FileInfo(
                    name = it["fileName"] as String,
                    fullName = it["fileName"] as String,
                    path = it["filePath"] as String,
                    fullPath = it["filePath"] as String,
                    size = it["fileSize"] as Long,
                    folder = false,
                    modifiedTime = (it["createTime"] as LocalDateTime).timestampmilli(),
                    artifactoryType = artifactoryType
                )
            )
        }
        return fileInfoList
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SamplePipelineBuildArtifactoryService::class.java)
    }
}
