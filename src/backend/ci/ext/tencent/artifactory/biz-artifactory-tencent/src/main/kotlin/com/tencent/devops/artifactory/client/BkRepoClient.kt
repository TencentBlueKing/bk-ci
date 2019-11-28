package com.tencent.devops.artifactory.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.common.api.constant.AUTH_HEADER_USER_ID
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.generic.pojo.FileInfo
import com.tencent.bkrepo.generic.pojo.operate.FileSearchRequest
import com.tencent.bkrepo.repository.pojo.metadata.UserMetadataSaveRequest
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import com.tencent.bkrepo.repository.pojo.node.user.UserNodeCopyRequest
import com.tencent.bkrepo.repository.pojo.node.user.UserNodeMoveRequest
import com.tencent.bkrepo.repository.pojo.node.user.UserNodeRenameRequest
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Okio
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.InputStream
import javax.ws.rs.NotFoundException

@Component
class BkRepoClient @Autowired constructor(
    private val objectMapper: ObjectMapper
) {
    // todo apicode 鉴权
    @Value("\${bkrepo.bkrepoUrl:#{null}}")
    private val BKREPO_URL: String? = null
    @Value("\${bkrepo.appCode:#{null}}")
    private val BKREPO_APP_CODE: String? = null
    @Value("\${bkrepo.appSecret:#{null}}")
    private val BKREPO_APP_SECRET: String? = null

    fun getFileSize(userId: String, projectId: String, repoName: String, path: String): NodeSizeInfo {
        val url = "$BKREPO_URL/repository/api/node/size/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("get file size failed, path: $path, responseContent: $responseContent")
                if (response.code() == 404) {
                    throw NotFoundException("not found")
                }
                throw RuntimeException("get file size failed")
            }

            val responseData = objectMapper.readValue<Response<NodeSizeInfo>>(responseContent)
            if (responseData.isNotOk()) {
                throw RuntimeException("get file size failed: ${responseData.message}")
            }

            return responseData.data!!
        }
    }

    fun setMetadata(userId: String, projectId: String, repoName: String, path: String, metadata: Map<String, String>) {
        logger.info("setMetadata, projectId: $projectId, repoName: $repoName, path: $path, metadata: $metadata")
        val url = "$BKREPO_URL/repository/api/metadata/$projectId/$repoName/$path"
        val requestData = UserMetadataSaveRequest(
            metadata = metadata
        )
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("set file metadata failed, repoName: $repoName, path: $path, responseContent: ${response.body()!!.string()}")
                throw RuntimeException("set file metadata failed")
            }
        }
    }

    fun listMetadata(userId: String, projectId: String, repoName: String, path: String): Map<String, String> {
        logger.info("list metadata of, projectId: $projectId, repoName: $repoName, path: $path")
        val url = "$BKREPO_URL/repository/api/metadata/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("list file metadata failed, path: $path, responseContent: $responseContent")
                if (response.code() == 404) {
                    throw NotFoundException("not found")
                }
                throw RuntimeException("list file metadata failed")
            }

            val responseData = objectMapper.readValue<Response<Map<String, String>>>(responseContent)
            if (responseData.isNotOk()) {
                throw RuntimeException("list file metadata failed: ${responseData.message}")
            }

            return responseData.data!!
        }
    }

    fun listFile(
        userId: String,
        projectId: String,
        repoName: String,
        path: String,
        includeFolders: Boolean = false,
        deep: Boolean = false
    ): List<FileInfo> {
        val url = "$BKREPO_URL/generic/list/$projectId/$repoName/$path?deep=$deep&includeFolder=$includeFolders"
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("list file failed, path: $path, responseContent: $responseContent")
                if (response.code() == 404) {
                    throw NotFoundException("not found")
                }
                throw RuntimeException("get file info failed")
            }

            val responseData = objectMapper.readValue<Response<List<FileInfo>>>(responseContent)
            if (responseData.isNotOk()) {
                throw RuntimeException("get file info failed: ${responseData.message}")
            }

            return responseData.data!!
        }
    }

    fun searchFile(
        userId: String,
        projectId: String,
        repoNames: List<String>,
        filePaterns: List<String>,
        metadatas: Map<String, String>,
        page: Int,
        pageSize: Int
    ): Page<NodeInfo> {
        val url = "$BKREPO_URL/generic/search"
        val requestData = FileSearchRequest(
            projectId = projectId,
            repoNameList = repoNames,
            pathPattern = listOf(),
            metadataCondition = metadatas,
            page = page,
            size = pageSize
        )
        val requestBody = objectMapper.writeValueAsString(requestData)
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("search file failed, requestBody: $requestBody, responseContent: $responseContent")
                throw RuntimeException("get file info failed")
            }

            val responseData = objectMapper.readValue<Response<Page<NodeInfo>>>(responseContent)
            if (responseData.isNotOk()) {
                throw RuntimeException("search file failed: ${responseData.message}")
            }

            return responseData.data!!
        }
    }

    fun uploadFile(userId: String, projectId: String, repoName: String, path: String, inputStream: InputStream) {
        logger.info("upload file, projectId: $projectId, repoName: $repoName, path: $path")
        val url = "$BKREPO_URL/generic/$projectId/$repoName/$path"
        val requestBody = object : RequestBody() {
            override fun writeTo(sink: BufferedSink?) {
                val source = Okio.source(inputStream)
                sink!!.writeAll(source)
            }

            override fun contentType(): MediaType? {
                return MediaType.parse("text/plain")
            }
        }
        val formBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "filename", requestBody)
            .build()
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .post(formBody).build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("upload file  failed, repoName: $repoName, path: $path, responseContent: ${response.body()!!.string()}")
                throw RuntimeException("upload file failed")
            }
        }
    }

    fun downloadFile(userId: String, projectId: String, repo: String, path: String) {
        TODO("not implemented") 
    }

    fun delete(userId: String, projectId: String, repo: String, path: String) {
        val url = "$BKREPO_URL/repository/api/node/$projectId/$repo/$path"
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .delete()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("delete file failed, path: $path, responseContent: ${response.body()!!.string()}")
                throw RuntimeException("delete file info failed")
            }
        }
    }

    fun move(userId: String, projectId: String, repoName: String, fromPath: String, toPath: String) {
        // todo 校验path参数
        logger.info("move, userId: $userId, projectId: $projectId, repoName: $repoName, fromPath: $fromPath, toPath: $toPath")
        val url = "$BKREPO_URL/repository/api/node/move"
        val requestData = UserNodeMoveRequest(
            srcProjectId = projectId,
            srcRepoName = repoName,
            srcFullPath = fromPath,
            destProjectId = projectId,
            destRepoName = repoName,
            destPath = toPath,
            overwrite = true
        )
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("move file failed, fromPath: $fromPath, toPath: $toPath, responseContent: ${response.body()!!.string()}")
                throw RuntimeException("move file failed")
            }
        }
    }

    fun copy(
        userId: String,
        fromProject: String,
        fromRepo: String,
        fromPath: String,
        toProject: String,
        toRepo: String,
        toPath: String
    ) {
        // todo 校验path参数
        logger.info("copy, userId: $userId, fromProject: $fromProject, fromRepo: $fromRepo, fromPath: $fromPath, toProject: $toProject, toRepo: $toRepo, toPath: $toPath")
	val url = "$BKREPO_URL/repository/api/node/copy"
        val requestData = UserNodeCopyRequest(
            srcProjectId = fromProject,
            srcRepoName = fromRepo,
            srcFullPath = fromPath,
            destProjectId = toProject,
            destRepoName = toRepo,
            destPath = toPath,
            overwrite = true
        )
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("copy file failed, fromPath: $fromPath, toPath: $toPath, responseContent: ${response.body()!!.string()}")
                throw RuntimeException("copy file failed")
            }
        }
    }

    fun rename(userId: String, projectId: String, repoName: String, fromPath: String, toPath: String) {
        // todo 校验path参数
        logger.info("rename, userId: $userId, projectId: $projectId, repoName: $repoName, fromPath: $fromPath, toPath: $toPath")
	val url = "$BKREPO_URL/repository/api/node/rename"
        val requestData = UserNodeRenameRequest(projectId, repoName, fromPath, toPath)
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(requestData)
                )
            ).build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("rename failed, responseContent: ${response.body()!!.string()}")
                throw RuntimeException("rename failed")
            }
        }
    }

    fun mkdir(userId: String, projectId: String, repoName: String, path: String) {
        logger.info("mkdir, path: $path")
        val url = "$BKREPO_URL/repository/api/node/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, userId)
            .post(RequestBody.create(null, ""))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.error("mkdir failed, responseContent: ${response.body()!!.string()}")
                throw RuntimeException("mkdir failed")
            }
        }
    }

    fun getFileDetail(userId: String, projectId: String, repoName: String, path: String): NodeDetail? {
        logger.info("getFileInfo, projectId:$projectId, repoName: $repoName, path: $path")
        val url = "$BKREPO_URL/repository/api/node/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            // .header("Authorization", makeCredential())
            .header(AUTH_HEADER_USER_ID, "admin")
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                if (response.code() == 404) {
                    logger.warn("file not found, repoName: $repoName, path: $path")
                    return null
                }
                logger.error("get file info failed, repoName: $repoName, path: $path, responseContent: $responseContent")
                throw RuntimeException("get file info failed")
            }

            val responseData = objectMapper.readValue<Response<NodeDetail>>(responseContent)
            if (responseData.isNotOk()) {
                throw RuntimeException("get file info failed: ${responseData.message}")
            }
            return responseData.data!!
        }
    }

    fun getFileContent(userId: String, projectId: String, repoName: String, path: String): Pair<ByteArray, MediaType> {
        logger.info("getFileContent, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path")
        val url = "$BKREPO_URL/generic/$projectId/$repoName/$path"
        val request = Request.Builder()
            .url(url)
            .header(AUTH_HEADER_USER_ID, "admin")
            // .header("Authorization", makeCredential())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.bytes()
            val mediaType = response.body()!!.contentType()!!
            if (!response.isSuccessful) {
                logger.error("get file content failed, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path")
                throw RuntimeException("get file content failed")
            }
            return Pair(responseContent, mediaType)
        }
    }

    fun externalDownloadUrl(
        userId: String,
        projectId: String,
        repoName: String,
        path: String,
        downloadUser: String,
        ttl: Int,
        directed: Boolean = false
    ): String {
        logger.info("externalDownloadUrl, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path, " +
            "downloadUser: $downloadUser, ttl: $ttl, directed: $directed")
        throw OperationException("TODO")
        // import com.tencent.bkrepo.generic.pojo.devops.ExternalUrlRequest
//        val url = "$BKREPO_URL/generic/devops/createExternalUrl"
//        val requestData = ExternalUrlRequest(
//            projectId = projectId,
//            repoName = repoName,
//            path = path,
//            downloadUser = downloadUser,
//            ttl = ttl,
//            directed = directed
//        )
//        val request = Request.Builder()
//            .url(url)
//            .header(AUTH_HEADER_USER_ID, userId)
//            // .header("Authorization", makeCredential())
//            .post(
//                RequestBody.create(
//                    MediaType.parse("application/json; charset=utf-8"),
//                    objectMapper.writeValueAsString(requestData)
//                )
//            )
//            .build()
//        OkhttpUtils.doHttp(request).use { response ->
//            val responseContent = response.body()!!.bytes()
//            if (!response.isSuccessful) {
//                logger.error("create external download url failed, requestUrl: $url, responseContent: $responseContent")
//                throw RuntimeException("create external download url failed")
//            }
//            val responseData = objectMapper.readValue<Response<String>>(responseContent)
//            if (responseData.isNotOk()) {
//                throw RuntimeException("create external download url failed: ${responseData.message}")
//            }
//            return responseData.data!!
//        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}