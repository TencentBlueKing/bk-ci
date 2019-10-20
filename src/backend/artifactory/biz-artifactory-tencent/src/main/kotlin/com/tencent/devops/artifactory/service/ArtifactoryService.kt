package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.AppFileInfo
import com.tencent.devops.artifactory.pojo.CopyToCustomReq
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.CustomFileSearchCondition
import com.tencent.devops.artifactory.pojo.FileChecksums
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FilePipelineInfo
import com.tencent.devops.artifactory.pojo.FolderSize
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.pojo.DockerUser
import com.tencent.devops.artifactory.service.pojo.JFrogAQLFileInfo
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.api.JFrogPropertiesApi
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_NAME
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PROJECT_ID
import com.tencent.devops.common.archive.shorturl.ShortUrlApi
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.auth.api.BkAuthResourceType
import com.tencent.devops.common.auth.api.BkAuthServiceCode
import com.tencent.devops.common.service.utils.HomeHostUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import javax.ws.rs.BadRequestException

@Service
class ArtifactoryService @Autowired constructor(
    val jFrogApiService: JFrogApiService,
    val jFrogAQLService: JFrogAQLService,
    val jFrogService: JFrogService,
    val pipelineService: PipelineService,
    val customDirService: CustomDirService,
    val jFrogPropertiesApi: JFrogPropertiesApi,
    val shortUrlApi: ShortUrlApi
) {

    @Value("\${rdeng.test:#{null}}")
    private val devUrl: String? = null

    fun getDevUrl() = devUrl

    fun hasDownloadPermission(
        userId: String,
        projectId: String,
        serviceCode: BkAuthServiceCode,
        resourceType: BkAuthResourceType,
        path: String
    ): Boolean {
        return if (serviceCode == BkAuthServiceCode.PIPELINE && resourceType == BkAuthResourceType.PIPELINE_DEFAULT) {
            val pipelineId = pipelineService.getPipelineId(path)
            pipelineService.validatePermission(userId, projectId, pipelineId, BkAuthPermission.EXECUTE)
        } else {
            false
        }
    }

    fun list(userId: String, projectId: String, artifactoryType: ArtifactoryType, path: String): List<FileInfo> {
        return when (artifactoryType) {
            ArtifactoryType.PIPELINE -> {
                pipelineService.list(userId, projectId, path)
            }
            ArtifactoryType.CUSTOM_DIR -> {
                customDirService.list(userId, projectId, path)
            }
        }
    }

    fun show(userId: String, projectId: String, artifactoryType: ArtifactoryType, path: String): FileDetail {
        return when (artifactoryType) {
            ArtifactoryType.PIPELINE -> {
                pipelineService.show(userId, projectId, path)
            }
            ArtifactoryType.CUSTOM_DIR -> {
                customDirService.show(userId, projectId, path)
            }
        }
    }

    fun folderSize(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String): FolderSize {
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

    fun setDockerProperties(projectId: String, imageName: String, tag: String, properties: Map<String, String>) {
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

    fun setProperties(
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        properties: Map<String, String>
    ) {
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

    fun getProperties(projectId: String, artifactoryType: ArtifactoryType, argPath: String): List<Property> {
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

    fun getPropertiesByRegex(
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        argPath: String
    ): List<FileDetail> {
        val regex = Pattern.compile(",|;")
        val pathArray = regex.split(argPath)

        val repoPathPrefix = JFrogUtil.getRepoPath()
        val pipelinePathPrefix = "/" + JFrogUtil.getPipelinePathPrefix(projectId).removePrefix(repoPathPrefix)
        val customDirPathPrefix = "/" + JFrogUtil.getCustomDirPathPrefix(projectId).removePrefix(repoPathPrefix)
        val ret = mutableListOf<FileDetail>()

        pathArray.forEach { path ->
            val normalizedPath = JFrogUtil.normalize(path)
            val realPath = if (path.startsWith("/")) normalizedPath else "/$normalizedPath"

            val pathPrefix = if (artifactoryType == ArtifactoryType.PIPELINE) {
                "/" + JFrogUtil.getPipelinePathPrefix(projectId).removePrefix(repoPathPrefix) + "$pipelineId/$buildId/" + JFrogUtil.getParentFolder(
                    realPath
                ).removePrefix("/")
            } else {
                "/" + JFrogUtil.getCustomDirPathPrefix(projectId).removePrefix(repoPathPrefix) + JFrogUtil.getParentFolder(
                    realPath
                ).removePrefix("/")
            }
            val fileName = JFrogUtil.getFileName(path)

            val jFrogAQLFileInfoList =
                jFrogAQLService.searchFileByRegex(repoPathPrefix, setOf(pathPrefix), setOf(fileName))
            logger.info("Path($path) match file list: $jFrogAQLFileInfoList")

            jFrogAQLFileInfoList.forEach {
                val pathTemp = if (it.path.startsWith(pipelinePathPrefix)) {
                    "/" + it.path.removePrefix(pipelinePathPrefix)
                } else {
                    "/" + it.path.removePrefix(customDirPathPrefix)
                }
                ret.add(show(projectId, artifactoryType, pathTemp))
            }
        }
        return ret
    }

    fun getOwnFileList(userId: String, projectId: String, offset: Int, limit: Int): Pair<Long, List<FileInfo>> {
        val startTimestamp = System.currentTimeMillis()

        try {
            val repoPathPrefix = JFrogUtil.getRepoPath()
            val pipelinePathPrefix = "/" + JFrogUtil.getPipelinePathPrefix(projectId).removePrefix(repoPathPrefix)
            val customDirPathPrefix = "/" + JFrogUtil.getCustomDirPathPrefix(projectId).removePrefix(repoPathPrefix)

            val relativePathSet = setOf(pipelinePathPrefix, customDirPathPrefix)
            val pipelineHasPermissionList = pipelineService.filterPipeline(userId, projectId, BkAuthPermission.LIST)

            val jFrogAQLFileInfoList =
                jFrogAQLService.listByCreateTimeDesc(repoPathPrefix, relativePathSet, offset, limit)
            val fileInfoList = transferJFrogAQLFileInfo(projectId, jFrogAQLFileInfoList, pipelineHasPermissionList)

            return Pair(LocalDateTime.now().timestamp(), fileInfoList)
        } finally {
            logger.info("getOwnFileList cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    fun getBuildFileList(userId: String, projectId: String, pipelineId: String, buildId: String): List<AppFileInfo> {
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
            val pipelineCanDownloadList = pipelineService.filterPipeline(userId, projectId, BkAuthPermission.DOWNLOAD)

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
                    it.name,
                    it.fullName,
                    it.path,
                    it.fullPath,
                    it.size,
                    it.folder,
                    it.modifiedTime,
                    it.artifactoryType,
                    show,
                    canDownload,
                    appVersion
                )
            }
        } finally {
            logger.info("getBuildFileList cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    fun getFilePipelineInfo(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): FilePipelineInfo {
        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        val properties = jFrogPropertiesApi.getProperties(realPath)
        if (!properties.containsKey(ARCHIVE_PROPS_PIPELINE_ID)) {
            throw RuntimeException("元数据(pipelineId)不存在")
        }
        val pipelineId = properties[ARCHIVE_PROPS_PIPELINE_ID]!!.first()
        val pipelineName = pipelineService.getPipelineName(projectId, pipelineId)

        return FilePipelineInfo(pipelineId, pipelineName)
    }

    fun show(projectId: String, artifactoryType: ArtifactoryType, path: String): FileDetail {
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

        return if (jFrogFileInfo.checksums == null) {
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
                    jFrogFileInfo.checksums.sha256,
                    jFrogFileInfo.checksums.sha1,
                    jFrogFileInfo.checksums.md5
                ),
                jFrogPropertiesMap
            )
        }
    }

    fun check(projectId: String, artifactoryType: ArtifactoryType, path: String): Boolean {
        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        return jFrogService.exist(realPath)
    }

    fun acrossProjectCopy(
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        targetProjectId: String,
        targetPath: String
    ): Count {
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
                destPath, mapOf(
                ARCHIVE_PROPS_PROJECT_ID to listOf(targetProjectId)
            )
            )
        }

        return Count(fileInfoList.size)
    }

    fun transferJFrogAQLFileInfo(
        projectId: String,
        jFrogAQLFileInfoList: List<JFrogAQLFileInfo>,
        pipelineHasPermissionList: List<String>,
        checkPermission: Boolean = true
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
                    val url =
                        "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=buildArchive&projectId=$projectId&pipelineId=$pipelineId&buildId=$buildId"
                    val shortUrl = shortUrlApi.getShortUrl(url, 300)

                    if ((!checkPermission || pipelineHasPermissionList.contains(pipelineId)) &&
                        pipelineIdToNameMap.containsKey(pipelineId) && buildIdToNameMap.containsKey(buildId)
                    ) {
                        val pipelineName = pipelineIdToNameMap[pipelineId]!!
                        val buildName = buildIdToNameMap[buildId]!!
                        val fullName = pipelineService.getFullName(path, pipelineId, pipelineName, buildId, buildName)
                        fileInfoList.add(
                            FileInfo(
                                it.name,
                                fullName,
                                path,
                                path,
                                it.size,
                                false,
                                LocalDateTime.parse(it.modified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                                ArtifactoryType.PIPELINE,
                                properties,
                                appVersion,
                                shortUrl
                            )
                        )
                    }
                } else {
                    val path = "/" + it.path.removePrefix(customDirPathPrefix)
                    fileInfoList.add(
                        FileInfo(
                            it.name,
                            path,
                            path,
                            path,
                            it.size,
                            false,
                            LocalDateTime.parse(it.modified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                            ArtifactoryType.CUSTOM_DIR,
                            properties,
                            appVersion
                        )
                    )
                }
            }

            return fileInfoList
        } finally {
            logger.info("transferJFrogAQLFileInfo cost: ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    fun transferJFrogAQLFileInfo(jFrogAQLFileInfoList: List<JFrogAQLFileInfo>): List<FileInfo> {
        val startTimestamp = System.currentTimeMillis()
        try {
            var fileInfoList = mutableListOf<FileInfo>()
            var appVersion: String? = null

            jFrogAQLFileInfoList.forEach {
                val properties = it.properties!!.map { itp ->
                    if (itp.key == "appVersion") {
                        appVersion = itp.value ?: ""
                    }
                    Property(itp.key, itp.value ?: "")
                }
                fileInfoList.add(
                    FileInfo(
                        name = it.name,
                        fullName = it.path,
                        path = it.path,
                        fullPath = it.path,
                        size = it.size,
                        folder = false,
                        modifiedTime = LocalDateTime.parse(it.modified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                        artifactoryType = ArtifactoryType.CUSTOM_DIR,
                        properties = properties,
                        appVersion = appVersion
                    )
                )
            }
            return fileInfoList
        } finally {
            logger.info("transferJFrogAQLFileInfo cost: ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    fun createDockerUser(projectCode: String): DockerUser {
        return jFrogApiService.createDockerUser(projectCode)
    }

    fun listCustomFiles(projectId: String, condition: CustomFileSearchCondition): List<String> {
        val allFiles = jFrogAQLService.searchByPathAndProperties(
            "generic-local/bk-custom/$projectId",
            condition.properties
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

    fun copyToCustom(userId: String, projectId: String, pipelineId: String, buildId: String, copyToCustomReq: CopyToCustomReq) {
        checkCopyToCustomReq(copyToCustomReq)
        customDirService.validatePermission(userId, projectId)

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

    private fun checkCopyToCustomReq(copyToCustomReq: CopyToCustomReq) {
        if (!copyToCustomReq.copyAll && copyToCustomReq.files.isEmpty()) {
            throw OperationException("invalid request")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactoryService::class.java)
    }
}