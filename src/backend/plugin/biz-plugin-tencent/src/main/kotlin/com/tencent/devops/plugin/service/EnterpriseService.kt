package com.tencent.devops.plugin.service

import com.tencent.devops.common.client.Client
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.concurrent.TimeUnit

@Service
class EnterpriseService @Autowired constructor(private val client: Client) {

    companion object {
        private val logger = LoggerFactory.getLogger(EnterpriseService::class.java)
    }

    @Value("\${enterprise.url}")
    private val ipList = ""

    @Value("\${enterprise.env}")
    private val env = ""

    fun upload(
        fileStream: InputStream,
        md5: String,
        size: Long,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        fileName: String,
        props: String
    ): String {

        // 调用接口上传ipa包
        ipList.split(",").map { it.trim() }.shuffled().forEach { ip ->
            val url = "http://$ip/upload?projectId=$projectId&pipelineId=$pipelineId&" +
                    "buildId=$buildId&size=$size&md5=$md5&env=$env&properties=$props"

            logger.info("request url >>> $url")

            val fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), fileStream.readBytes())
            val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, fileBody)
                    .build()
            val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

            val timeout = (1 + size / 1024 / 1024 / 1024) * 7
            val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(100, TimeUnit.SECONDS)
                    .writeTimeout(timeout, TimeUnit.MINUTES)
                    .readTimeout(timeout, TimeUnit.MINUTES)
                    .build()

            okHttpClient.newCall(request).execute().use { response ->
                val data = response.body()!!.string()
                logger.info("data>>>> $data")
                if (response.isSuccessful && data == "success") {
                    return "success"
                }
            }
        }
        throw RuntimeException("enterprise sign fail")
    }
}
