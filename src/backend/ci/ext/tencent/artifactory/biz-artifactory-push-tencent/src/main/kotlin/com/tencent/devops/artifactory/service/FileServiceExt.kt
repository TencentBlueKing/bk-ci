package com.tencent.devops.artifactory.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.artifactory.constant.PushMessageCode
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.common.service.utils.MessageCodeUtil
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths

@Service
class FileServiceExt @Autowired constructor(
    private val repoGray: RepoGray,
    private val redisOperation: RedisOperation
): FileService {
    @Value("\${gateway.url:#{null}}")
    private val gatewayUrl: String? = null

    override fun downloadFileTolocal(
        projectId: String,
        pipelineId: String,
        buildId: String,
        fileName: String,
        isCustom: Boolean
    ): List<File> {
        val downloadFiles = mutableListOf<File>()
        val destPath = Files.createTempDirectory("").toFile().absolutePath
        val isRepoGray = repoGray.isGray(projectId, redisOperation)

        var count = 0
        fileName.split(",").map {
            it.trim().removePrefix("/").removePrefix("./")
        }.forEach { path ->
            if (isRepoGray) {
                val fileList = matchBkRepoFile(path, projectId, pipelineId, buildId, isCustom)
                val repoName = if (isCustom) "custom" else "pipeline"
                fileList.forEach { bkrepoFile ->
                    logger.info("BKRepoFile匹配到文件：(${bkrepoFile.displayPath})")
                    count++
                    val url =
                        "http://$gatewayUrl/bkrepo/api/service/generic/$projectId/$repoName/${bkrepoFile.fullPath}"
                    val destFile = File(destPath, File(bkrepoFile.displayPath).name)
                    OkhttpUtils.downloadFile(url, destFile, mapOf("X-BKREPO-UID" to "admin")) // todo user
                    downloadFiles.add(destFile)
                    logger.info("save file : ${destFile.canonicalPath} (${destFile.length()})")
                }
            } else {
                val fileList = matchFile(path, projectId, pipelineId, buildId, isCustom)
                fileList.forEach { jfrogFile ->
                    logger.info("Jfrog匹配到文件：(${jfrogFile.uri})")
                    count++
                    val url = if (isCustom) "http://$gatewayUrl/jfrog/storage/service/custom/$projectId${jfrogFile.uri}"
                    else "http://$gatewayUrl/jfrog/storage/service/archive/$projectId/$pipelineId/$buildId${jfrogFile.uri}"
                    val destFile = File(destPath, File(jfrogFile.uri).name)
                    OkhttpUtils.downloadFile(url, destFile)
                    downloadFiles.add(destFile)
                    logger.info("save file : ${destFile.canonicalPath} (${destFile.length()})")
                }
            }
        }
        if (count == 0) {
            throw RuntimeException(MessageCodeUtil.getCodeMessage(PushMessageCode.FILE_NOT_EXITS, arrayOf(fileName)))
        }
        return downloadFiles
    }

    private fun matchBkRepoFile(
        srcPath: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        isCustom: Boolean
    ): List<BkRepoFile> {
        val result = mutableListOf<BkRepoFile>()
        val bkRepoData = getAllBkRepoFiles(projectId, pipelineId, buildId, isCustom)
        val matcher = FileSystems.getDefault().getPathMatcher("glob:$srcPath")
        val pipelinePathPrefix = "/$pipelineId/$buildId"
        bkRepoData.data?.forEach { bkrepoFile ->
            val repoPath = if (isCustom) {
                bkrepoFile.fullPath.removePrefix("/")
            } else {
                bkrepoFile.fullPath.removePrefix(pipelinePathPrefix)
            }
            if (matcher.matches(Paths.get(repoPath))) {
                bkrepoFile.displayPath = repoPath
                result.add(bkrepoFile)
            }
        }
        return result
    }

    private fun getAllBkRepoFiles(
        projectId: String,
        pipelineId: String,
        buildId: String,
        isCustom: Boolean
    ): BkRepoData {
        logger.info("getAllBkrepoFiles, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId, isCustom: $isCustom")
        var url = if (isCustom) {
            "http://$gatewayUrl/bkrepo/api/service/generic/list/$projectId/custom?includeFolder=true&deep=true"
        } else {
            "http://$gatewayUrl/bkrepo/api/service/generic/list/$projectId/pipeline/$pipelineId/$buildId?includeFolder=true&deep=true"
        }
        val request = Request.Builder()
            .url(url)
            .header("X-BKREPO-UID", "admin") // todo user
            .get()
            .build()

        // 获取所有的文件和文件夹
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("get bkrepo files fail: $responseBody")
                throw RuntimeException(MessageCodeUtil.getCodeMessage(PushMessageCode.GET_FILE_FAIL, null))
            }
            try {
                return JsonUtil.getObjectMapper().readValue(responseBody, BkRepoData::class.java)
            } catch (e: Exception) {
                logger.warn("get bkrepo files fail: $responseBody")
                throw RuntimeException(MessageCodeUtil.getCodeMessage(PushMessageCode.GET_FILE_FAIL, null))
            }
        }
    }

    // 匹配文件
    fun matchFile(
        srcPath: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        isCustom: Boolean
    ): List<JfrogFile> {
        val result = mutableListOf<JfrogFile>()
        val data = getAllFiles(projectId, pipelineId, buildId, isCustom)

        val matcher = FileSystems.getDefault()
            .getPathMatcher("glob:$srcPath")
        data.files.forEach { jfrogFile ->
            if (matcher.matches(Paths.get(jfrogFile.uri.removePrefix("/")))) {
                result.add(jfrogFile)
            }
        }
        return result
    }

    // 获取所有的文件和文件夹
    private fun getAllFiles(projectId: String, pipelineId: String, buildId: String, isCustom: Boolean): JfrogFilesData {

        val cusListFilesUrl = "http://$gatewayUrl/jfrog/api/service/custom/$projectId?list&deep=1&listFolders=1"
        val listFilesUrl = "http://$gatewayUrl/jfrog/api/service/archive"

        val url = if (!isCustom) "$listFilesUrl/$projectId/$pipelineId/$buildId?list&deep=1&listFolders=1"
        else cusListFilesUrl

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        // 获取所有的文件和文件夹
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("get jfrog files($url) fail:\n $responseBody")
                throw RuntimeException(MessageCodeUtil.getCodeMessage(PushMessageCode.GET_FILE_FAIL, null))
            }
            try {
                return JsonUtil.getObjectMapper().readValue(responseBody, JfrogFilesData::class.java)
            } catch (e: Exception) {
                logger.warn("get jfrog files($url) fail\n$responseBody")
                throw RuntimeException(MessageCodeUtil.getCodeMessage(PushMessageCode.GET_FILE_FAIL, null))
            }
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }

    data class BkRepoFile(
        val fullPath: String,
        var displayPath: String?,
        val size: Long,
        val folder: Boolean
    )

    data class BkRepoData(
        var code: Int,
        var message: String?,
        var data: List<BkRepoFile>
    )

    data class JfrogFilesData(
        val uri: String,
        val created: String,
        val files: List<JfrogFile>
    )

    data class JfrogFile(
        val uri: String,
        val size: Long,
        val lastModified: String,
        val folder: Boolean,
        @JsonProperty(required = false)
        val sha1: String = ""
    )
}