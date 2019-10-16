package com.tencent.devops.support.services

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.PutObjectRequest
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

@Service
class AwsClientService @Autowired constructor(private val profile: Profile) {

    @Value("\${s3.endpointUrl}")
    private lateinit var endpointUrl: String

    @Value("\${s3.accessKey}")
    private lateinit var accessKey: String

    @Value("\${s3.secretKey}")
    private lateinit var secretKey: String

    @Value("\${s3.bucketName}")
    private lateinit var bucketName: String

    private var client: AmazonS3? = null

    fun uploadFile(file: File): Result<String?> {
        val uploadFileName = file.name
        val index = uploadFileName.lastIndexOf(".")
        val fileType = uploadFileName.substring(index + 1)
        val key = "ieod/${profile.getEnv().name.toLowerCase()}/file/$fileType/$uploadFileName"
        logger.info("the key is:$key")
        val request = PutObjectRequest(bucketName, key, file)
            .withCannedAcl(CannedAccessControlList.PublicRead)
        val client = awsClient()
        client.putObject(request)
        return try {
            val filePath = client.getUrl(bucketName, key).toString() + "?v=${System.currentTimeMillis() / 1000}"
            logger.info("the filePath is:$filePath")
            Result(filePath)
        } catch (e: Exception) {
            logger.warn("upload file error:", e)
            MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        }
    }

    private fun awsClient(): AmazonS3 {
        if (client == null) {
            synchronized(this) {
                if (client == null) {
                    val credentials = BasicAWSCredentials(accessKey, secretKey)
                    client = AmazonS3ClientBuilder.standard()
                        .withCredentials(AWSStaticCredentialsProvider(credentials))
                        .withEndpointConfiguration(
                            AwsClientBuilder.EndpointConfiguration(
                                endpointUrl,
                                ""
                            )
                        )
                        .build()
                }
            }
        }
        return client!!
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AwsClientService::class.java)
    }
}
