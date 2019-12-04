package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.pojo.JFrogFileInfo
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BSAuthPermissionApi
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.service.ServiceJfrogResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class PipelineService @Autowired constructor(
    private val client: Client,
    private val pipelineAuthServiceCode: BSPipelineAuthServiceCode,
    private val bkAuthPermissionApi: BSAuthPermissionApi
) {
    private val resourceType = AuthResourceType.PIPELINE_DEFAULT

    fun hasPermission(userId: String, projectId: String, pipelineId: String, authPermission: AuthPermission): Boolean {
        return validatePermission(userId, projectId, pipelineId, authPermission)
    }

    fun getRootPathFileList(userId: String, projectId: String, path: String, jFrogFileInfoList: List<JFrogFileInfo>, authPermission: AuthPermission): List<FileInfo> {
        val hasPermissionList = filterPipeline(userId, projectId, authPermission)
        val pipelineIdToNameMap = getPipelineNames(projectId, hasPermissionList.toSet())

        val fileInfoList = mutableListOf<FileInfo>()
        jFrogFileInfoList.forEach {
            val fullPath = JFrogUtil.compose(path, it.uri, it.folder)
            val pipelineId = getPipelineId(fullPath)
            if (pipelineIdToNameMap.containsKey(pipelineId)) {
                val pipelineName = pipelineIdToNameMap[pipelineId]!!
                val fullName = getFullName(fullPath, pipelineId, pipelineName)
                fileInfoList.add(
                    FileInfo(
                        pipelineName,
                        fullName,
                        it.uri,
                        fullPath,
                        it.size,
                        it.folder,
                        LocalDateTime.parse(it.lastModified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                        ArtifactoryType.PIPELINE
                    )
                )
            }
        }
        return JFrogUtil.sort(fileInfoList)
    }

    fun getPipelinePathList(projectId: String, path: String, jFrogFileInfoList: List<JFrogFileInfo>): List<FileInfo> {
        val pipelineId = getPipelineId(path)
        val pipelineName = getPipelineName(projectId, pipelineId)
        val buildIdList = jFrogFileInfoList.map { it.uri.removePrefix("/") }
        val buildIdToNameMap = getBuildNames(buildIdList.toSet())

        val fileInfoList = mutableListOf<FileInfo>()
        jFrogFileInfoList.forEach {
            val fullPath = JFrogUtil.compose(path, it.uri, it.folder)
            val buildId = getBuildId(fullPath)
            if (buildIdToNameMap.containsKey(buildId)) {
                val buildName = buildIdToNameMap[buildId]!!
                val fullName = getFullName(fullPath, pipelineId, pipelineName, buildId, buildName)
                fileInfoList.add(
                    FileInfo(
                        buildName,
                        fullName,
                        it.uri,
                        fullPath,
                        it.size,
                        it.folder,
                        LocalDateTime.parse(it.lastModified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                        ArtifactoryType.PIPELINE
                    )
                )
            }
        }
        return JFrogUtil.sort(fileInfoList)
    }

    fun getBuildPathList(projectId: String, path: String, jFrogFileInfoList: List<JFrogFileInfo>): List<FileInfo> {
        val pipelineId = getPipelineId(path)
        val buildId = getBuildId(path)
        val pipelineName = getPipelineName(projectId, pipelineId)
        val buildName = getBuildName(buildId)

        val fileInfoList = jFrogFileInfoList.map {
            val fullPath = JFrogUtil.compose(path, it.uri, it.folder)
            val fullName = getFullName(fullPath, pipelineId, pipelineName, buildId, buildName)
            val name = JFrogUtil.getFileName(fullPath)
            FileInfo(
                name,
                fullName,
                it.uri,
                fullPath,
                it.size,
                it.folder,
                LocalDateTime.parse(it.lastModified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                ArtifactoryType.PIPELINE
            )
        }
        return JFrogUtil.sort(fileInfoList)
    }

    fun getDirectoryName(projectId: String, directory: String): String {
        return when {
            isPipelineDir(directory) -> {
                val pipelineId = getPipelineId(directory)
                getPipelineName(projectId, pipelineId)
            }
            isBuildDir(directory) -> {
                val buildId = getBuildId(directory)
                getBuildName(buildId)
            }
            else -> {
                JFrogUtil.getFileName(directory)
            }
        }
    }

    fun getName(projectId: String, path: String): String {
        val roadList = path.split("/")
        return when {
            // 非法路径
            roadList.size < 2 -> {
                throw RuntimeException("Invalid path $path")
            }
            // 流水线目录
            roadList.size == 2 -> {
                val pipelineId = getPipelineId(path)
                getPipelineName(projectId, pipelineId)
            }
            // 构建目录
            roadList.size == 3 -> {
                val buildId = getBuildId(path)
                getBuildName(buildId)
            }
            // 构建目录
            else -> {
                JFrogUtil.getFileName(path)
            }
        }
    }

    fun getFullName(projectId: String, path: String): String {
        val roadList = path.split("/")
        return when {
            // 非法路径
            roadList.size < 2 -> {
                throw RuntimeException("Invalid path $path")
            }
            // 根目录
            roadList.size == 2 && roadList[1].isBlank() -> {
                path
            }
            // 流水线目录
            roadList.size == 2 && roadList[1].isNotBlank() -> {
                val pipelineId = getPipelineId(path)
                val pipelineName = getPipelineName(projectId, pipelineId)
                return getFullName(path, pipelineId, pipelineName)
            }
            else -> {
                val pipelineId = getPipelineId(path)
                val pipelineName = getPipelineName(projectId, pipelineId)
                val buildId = getBuildId(path)
                val buildName = getBuildName(buildId)
                return getFullName(path, pipelineId, pipelineName, buildId, buildName)
            }
        }
    }

    fun getFullName(path: String, pipelineId: String, pipelineName: String): String {
        return path.replaceFirst("/$pipelineId", "/$pipelineName")
    }

    fun getFullName(path: String, pipelineId: String, pipelineName: String, buildId: String, buildName: String): String {
        return path.replaceFirst("/$pipelineId/$buildId", "/$pipelineName/$buildName")
    }

    fun isRootDir(path: String): Boolean {
        return path == "/"
    }

    fun isPipelineDir(path: String): Boolean {
        val roadList = path.split("/")
        return roadList.size == 2 && roadList[1].isNotBlank()
    }

    private fun isBuildDir(path: String): Boolean {
        val roadList = path.split("/")
        return roadList.size == 3 && roadList[2].isNotBlank()
    }

    fun getPipelineId(path: String): String {
        val roads = path.split("/")
        if (roads.size < 2) throw RuntimeException("Path $path doesn't contain pipelineId")
        return roads[1]
    }

    fun getBuildId(path: String): String {
        val roads = path.split("/")
        if (roads.size < 3) throw RuntimeException("Path $path doesn't contain buildId")
        return roads[2]
    }

    fun getPipelineName(projectId: String, pipelineId: String): String {
        val startTimestamp = System.currentTimeMillis()
        try {
            return client.get(ServiceJfrogResource::class).getPipelineNameByIds(
                projectId,
                setOf(pipelineId)
            ).data!![pipelineId]!!
        } finally {
            logger.info("getPipelineName [$projectId, $pipelineId] cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    fun getPipelineNames(projectId: String, pipelineIds: Set<String>): Map<String, String> {
        val startTimestamp = System.currentTimeMillis()
        try {
            if (pipelineIds.isEmpty()) return emptyMap()
            return client.get(ServiceJfrogResource::class).getPipelineNameByIds(projectId, pipelineIds).data!!
        } finally {
            logger.info("getPipelineNames [$projectId, $pipelineIds] cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    fun getBuildName(buildId: String): String {
        val startTimestamp = System.currentTimeMillis()
        try {
            return client.get(ServiceJfrogResource::class).getBuildNoByBuildIds(setOf(buildId)).data!![buildId]!!
        } finally {
            logger.info("getBuildName [$buildId] cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    fun getBuildNames(buildIds: Set<String>): Map<String, String> {
        val startTimestamp = System.currentTimeMillis()
        try {
            if (buildIds.isEmpty()) return emptyMap()
            return client.get(ServiceJfrogResource::class).getBuildNoByBuildIds(buildIds).data!!
        } finally {
            logger.info("getBuildNames [$buildIds] cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    fun validatePermission(user: String, projectId: String, pipelineId: String, authPermission: AuthPermission, message: String) {
        if (!validatePermission(user, projectId, pipelineId, authPermission)) {
            throw PermissionForbiddenException(message)
        }
    }

    fun validatePermission(user: String, projectId: String, pipelineId: String, authPermission: AuthPermission): Boolean {
        return bkAuthPermissionApi.validateUserResourcePermission(
            user,
            pipelineAuthServiceCode,
            AuthResourceType.PIPELINE_DEFAULT,
            projectId,
            pipelineId,
            authPermission
        )
    }

    fun filterPipeline(user: String, projectId: String, authPermission: AuthPermission): List<String> {
        val startTimestamp = System.currentTimeMillis()
        try {
            return bkAuthPermissionApi.getUserResourceByPermission(
                user,
                pipelineAuthServiceCode,
                resourceType,
                projectId,
                authPermission,
                null
            )
        } finally {
            logger.info("filterPipeline cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineService::class.java)
    }
}