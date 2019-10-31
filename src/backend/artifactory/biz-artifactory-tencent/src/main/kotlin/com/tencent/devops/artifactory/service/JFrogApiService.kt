package com.tencent.devops.artifactory.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.pojo.DockerUser
import com.tencent.devops.artifactory.service.pojo.JFrogApiResponse
import com.tencent.devops.artifactory.service.pojo.JFrogArchiveFileInfo
import com.tencent.devops.artifactory.service.pojo.JFrogArchiveRequest
import com.tencent.devops.artifactory.service.pojo.JFrogFolderCount
import com.tencent.devops.artifactory.service.pojo.JFrogFolderCountRequest
import com.tencent.devops.artifactory.service.pojo.Url
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder

@Service
class JFrogApiService @Autowired constructor(private val objectMapper: ObjectMapper) {
    @Value("\${jfrog.url:#{null}}")
    private val JFROG_BASE_URL: String? = null
    @Value("\${jfrog.username:#{null}}")
    private val JFROG_USERNAME: String? = null
    @Value("\${jfrog.password:#{null}}")
    private val JFROG_PASSWORD: String? = null

    /**
     * 获取用户登录态鉴权的下载链接
     */
    fun downloadUrl(path: String): String {
        val url = "$JFROG_BASE_URL/api/plugins/execute/downloadUrl?params=path=${URLEncoder.encode(path, "UTF-8")}"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to create jfrog $path downloadUrl ($url). $responseContent")
                throw RuntimeException("Fail to create downloadUrl")
            }

            val jFrogApiResponse = objectMapper.readValue<JFrogApiResponse<Url>>(responseContent)
            return jFrogApiResponse.data!!.url
        }
    }

    /**
     * 获取用户登录态鉴权的下载链接
     */
    fun createDockerUser(projectCode: String): DockerUser {
        val url = "$JFROG_BASE_URL/api/plugins/execute/createDockerUser?params=projectCode=$projectCode"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to create docker user $projectCode. $responseContent")
                throw RuntimeException("Fail to create docker user")
            }

            val jFrogApiResponse = objectMapper.readValue<JFrogApiResponse<DockerUser>>(responseContent)
            return jFrogApiResponse.data!!
        }
    }

    /**
     * 获取带token鉴权的下载链接
     */
    fun internalDownloadUrl(path: String, ttl: Int, downloadUsers: String): String {
        val url = "$JFROG_BASE_URL/api/plugins/execute/internalDownloadUrl?params=path=$path;ttl=$ttl;downloadUsers=$downloadUsers"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to create jfrog $path internalDownloadUrl. $responseContent")
                throw RuntimeException("Fail to create internalDownloadUrl")
            }

            val jFrogApiResponse = objectMapper.readValue<JFrogApiResponse<Url>>(responseContent)
            return jFrogApiResponse.data!!.url
        }
    }

    /**
     * 获取带token鉴权的下载链接
     */
    fun ioaDownloadUrl(path: String): String {
        val url = "$JFROG_BASE_URL/api/plugins/execute/ioaDownloadUrl?params=path=$path"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to create jfrog $path ioaDownloadUrl. $responseContent")
                throw RuntimeException("Fail to create ioaDownloadUrl")
            }

            val jFrogApiResponse = objectMapper.readValue<JFrogApiResponse<Url>>(responseContent)
            return jFrogApiResponse.data!!.url
        }
    }

    /**
     * 获取带token鉴权的下载链接
     */
    fun thirdPartyDownloadUrl(path: List<String>, ttl: Int): String {
        val url = "$JFROG_BASE_URL/api/plugins/execute/thirdPartyDownloadUrl?params=path=${path.joinToString(",")};ttl=$ttl"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .get()
            .build()

//        val httpClient = okHttpClient.newBuilder().build()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to create jfrog $path serviceDownloadUrl. $responseContent")
                throw RuntimeException("Fail to create serviceDownloadUrl")
            }

            val jFrogApiResponse = objectMapper.readValue<JFrogApiResponse<Url>>(responseContent)
            return jFrogApiResponse.data!!.url
        }
    }

    /**
     * 获取带token鉴权的下载链接
     */
    fun batchThirdPartyDownloadUrl(path: List<String>, ttl: Int): Map<String, String> {
        if (path.isEmpty()) {
            return emptyMap()
        }
        val url = "$JFROG_BASE_URL/api/plugins/execute/batchThirdPartyDownloadUrl?params=path=${path.joinToString(",")};ttl=$ttl"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to create jfrog $path serviceDownloadUrl. $responseContent")
                throw RuntimeException("Fail to create serviceDownloadUrl")
            }

            val jFrogApiResponse = objectMapper.readValue<JFrogApiResponse<Map<String, String>>>(responseContent)
            return jFrogApiResponse.data!!
        }
    }

    /**
     * 获取外部带token的下载链接
     */
    fun externalDownloadUrl(path: String, userId: String, ttl: Int, directed: Boolean = false): String {
        val url = "$JFROG_BASE_URL/api/plugins/execute/externalDownloadUrl?params=path=$path;downloadUser=$userId;ttl=$ttl;directed=$directed"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to create jfrog $path externalDownloadUrl. $responseContent")
                throw RuntimeException("Fail to create externalDownloadUrl")
            }

            val jFrogApiResponse = objectMapper.readValue<JFrogApiResponse<Url>>(responseContent)
            return jFrogApiResponse.data!!.url
        }
    }

    fun folderCount(path: String): Long {
        val name = path.removeSuffix("/").split("/").last()
        val jFrogFolderCountRequest = JFrogFolderCountRequest(name, path)
        val requestContent = objectMapper.writeValueAsString(jFrogFolderCountRequest)

        val url = "$JFROG_BASE_URL/api/artifactgeneral/artifactsCount?\$no_spinner=true"
        val mediaType = MediaType.parse("application/json")
        val requestBody = RequestBody.create(mediaType, requestContent)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to count folder $path. $responseContent")
                throw RuntimeException("Fail to count folder")
            }

            val jFrogFolderCount = objectMapper.readValue<JFrogFolderCount>(responseContent)
            val countList = jFrogFolderCount.artifactSize.split(" ")
            if (countList.size != 2) {
                logger.error("folder count artifactSize ${jFrogFolderCount.artifactSize} invalid")
                throw RuntimeException("Fail to count folder")
            }

            val size = countList[0].toFloat()
            val unit = when (countList[1]) {
                "bytes" -> 1L
                "KB" -> 1024L
                "MB" -> 1024 * 1024L
                "GB" -> 1024 * 1024 * 1024L
                "TB" -> 1024 * 1024 * 1024 * 1024L
                else -> throw RuntimeException("folder count unit ${countList[1]} invalid")
            }
            return (size * unit).toLong()
        }
    }

    fun archiveList(path: String): List<JFrogArchiveFileInfo> {
        val roadList = path.split("/")
        val repoKey = roadList.first()
        val name = roadList.last()
        val relativePath = path.removePrefix("$repoKey/")
        val jFrogArchiveRequest = JFrogArchiveRequest(relativePath, repoKey, name)
        val requestContent = objectMapper.writeValueAsString(jFrogArchiveRequest)

        val url = "$JFROG_BASE_URL/api/treebrowser?compacted=true&\$no_spinner=true"
        val mediaType = MediaType.parse("application/json")
        val requestBody = RequestBody.create(mediaType, requestContent)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to list archive file $path. $responseContent")
                throw RuntimeException("Fail to list archive file $path")
            }

            return objectMapper.readValue(responseContent)
        }
    }

    private fun makeCredential(): String = Credentials.basic(JFROG_USERNAME!!, JFROG_PASSWORD!!)

    companion object {
        private val logger = LoggerFactory.getLogger(JFrogApiService::class.java)
    }
}