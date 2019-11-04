/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
