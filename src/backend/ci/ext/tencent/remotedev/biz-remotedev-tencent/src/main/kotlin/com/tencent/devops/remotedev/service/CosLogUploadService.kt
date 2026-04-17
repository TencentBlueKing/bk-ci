package com.tencent.devops.remotedev.service

import com.qcloud.cos.COSClient
import com.qcloud.cos.ClientConfig
import com.qcloud.cos.auth.BasicCOSCredentials
import com.qcloud.cos.http.HttpMethodName
import com.qcloud.cos.region.Region
import com.tencent.devops.remotedev.pojo.LogUploadUrl
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class CosLogUploadService {

    @Value("\${cos.log.secretId:}")
    val secretId: String = ""

    @Value("\${cos.log.secretKey:}")
    val secretKey: String = ""

    @Value("\${cos.log.region:ap-guangzhou}")
    val region: String = "ap-guangzhou"

    @Value("\${cos.log.bucket:}")
    val bucket: String = ""

    @Value("\${cos.log.replaceDomain:anydev-box.it.tencent.com}")
    val replaceDomain: String = "anydev-box.it.tencent.com"

    @Value("\${cos.log.expireSeconds:600}")
    val expireSeconds: Long = 600

    fun generateLogUploadUrl(
        userId: String
    ): LogUploadUrl {
        val now = LocalDateTime.now()
        val dateDir = now.format(DATE_FORMATTER)
        val timePart = now.format(TIME_FORMATTER)
        val key = "cvd-box-logs/$dateDir/$userId-$timePart.zip"

        val cosClient = buildCosClient()
        try {
            val expiration = Date(
                System.currentTimeMillis() + expireSeconds * 1000
            )
            val presignedUrl = cosClient.generatePresignedUrl(
                bucket, key, expiration, HttpMethodName.PUT
            )
            val replacedUrl = replaceDomain(presignedUrl)
            logger.info(
                "Generated log upload url for user=$userId, key=$key"
            )
            return LogUploadUrl(
                url = replacedUrl,
                expireSeconds = expireSeconds
            )
        } finally {
            cosClient.shutdown()
        }
    }

    private fun buildCosClient(): COSClient {
        val credentials = BasicCOSCredentials(secretId, secretKey)
        val clientConfig = ClientConfig(Region(region))
        return COSClient(credentials, clientConfig)
    }

    private fun replaceDomain(originalUrl: URL): String {
        val urlStr = originalUrl.toString()
        val host = originalUrl.host
        return urlStr.replaceFirst(host, replaceDomain)
    }

    companion object {
        private val logger =
            LoggerFactory.getLogger(CosLogUploadService::class.java)
        private val DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HHmmss")
    }
}
