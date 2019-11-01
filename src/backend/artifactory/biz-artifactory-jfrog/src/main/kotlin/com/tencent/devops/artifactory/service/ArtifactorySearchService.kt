package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_FILE_NAME
import com.tencent.devops.common.auth.api.AuthPermission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ArtifactorySearchService @Autowired constructor(
    val jFrogAQLService: JFrogAQLService,
    val pipelineService: PipelineService,
    val artifactoryService: ArtifactoryService
) {

    fun search(
        userId: String,
        projectId: String,
        searchProps: SearchProps,
        offset: Int,
        limit: Int
    ): Pair<Long, List<FileInfo>> {
        logger.info("Search file. [ProjectId=$projectId, Props=$searchProps]")

        val repoPathPrefix = JFrogUtil.getRepoPath()
        val pipelinePathPrefix = "/" + JFrogUtil.getPipelinePathPrefix(projectId).removePrefix(repoPathPrefix)
        val customDirPathPrefix = "/" + JFrogUtil.getCustomDirPathPrefix(projectId).removePrefix(repoPathPrefix)

        val relativePathSet = setOf(pipelinePathPrefix, customDirPathPrefix)
        val pipelineHasPermissionList = pipelineService.filterPipeline(userId, projectId, AuthPermission.LIST)

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
            artifactoryService.transferJFrogAQLFileInfo(projectId, jFrogAQLFileInfoList, pipelineHasPermissionList)
        return Pair(LocalDateTime.now().timestamp(), fileInfoList)
    }

    fun serviceSearch(
        projectId: String,
        searchProps: List<Property>,
        offset: Int,
        limit: Int
    ): Pair<Long, List<FileInfo>> {
        logger.info("Service search file and property. [ProjectId=$projectId, Props=$searchProps]")

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

    fun searchFileAndProperty(
        userId: String,
        projectId: String,
        searchProps: SearchProps
    ): Pair<Long, List<FileInfo>> {
        logger.info("Service search file and property. [ProjectId=$projectId, Props=$searchProps]")

        val repoPathPrefix = JFrogUtil.getRepoPath()
        val pipelinePathPrefix = "/" + JFrogUtil.getPipelinePathPrefix(projectId).removePrefix(repoPathPrefix)
        val customDirPathPrefix = "/" + JFrogUtil.getCustomDirPathPrefix(projectId).removePrefix(repoPathPrefix)

        val relativePathSet = setOf(pipelinePathPrefix, customDirPathPrefix)
        val pipelineHasPermissionList = pipelineService.filterPipeline(userId, projectId, AuthPermission.LIST)

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

    fun serviceSearchFileByRegex(
        projectId: String,
        pipelineId: String,
        buildId: String,
        regexPath: String,
        customized: Boolean
    ): Pair<Long, List<FileInfo>> {
        logger.info("Service search file by regex. [ProjectId=$projectId, PipelineId=$pipelineId, BuildId=$buildId, RegexPath=$regexPath]")

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

    fun serviceSearchFileAndProperty(
        projectId: String,
        searchProps: List<Property>,
        customized: Boolean? = null
    ): Pair<Long, List<FileInfo>> {
        logger.info("Service search file and property. [ProjectId=$projectId, Props=$searchProps]")

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

        val jFrogAQLFileInfoList =
            jFrogAQLService.searchFileAndPropertyByPropertyByAnd(repoPathPrefix, relativePathSet, fileNameSet, props)
        val fileInfoList =
            artifactoryService.transferJFrogAQLFileInfo(projectId, jFrogAQLFileInfoList, emptyList(), false).sortedWith(
                Comparator { file1, file2 -> -file1.modifiedTime.compareTo(file2.modifiedTime) }
            )
        return Pair(LocalDateTime.now().timestamp(), fileInfoList)
    }

    fun serviceSearchFileAndPropertyByOr(
        projectId: String,
        searchProps: List<Property>,
        customized: Boolean? = null
    ): Pair<Long, List<FileInfo>> {
        logger.info("Service search file and property by or. [ProjectId=$projectId, Props=$searchProps]")

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

        val jFrogAQLFileInfoList =
            jFrogAQLService.searchFileAndPropertyByPropertyByOr(repoPathPrefix, relativePathSet, fileNameSet, props)
        val fileInfoList =
            artifactoryService.transferJFrogAQLFileInfo(projectId, jFrogAQLFileInfoList, emptyList(), false).sortedWith(
                Comparator { file1, file2 -> -file1.modifiedTime.compareTo(file2.modifiedTime) }
            )
        return Pair(LocalDateTime.now().timestamp(), fileInfoList)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}