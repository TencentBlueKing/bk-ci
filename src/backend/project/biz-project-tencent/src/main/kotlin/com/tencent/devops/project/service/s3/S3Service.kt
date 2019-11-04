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

package com.tencent.devops.project.service.s3

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.PutObjectRequest
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.lang.RuntimeException

@Service
class S3Service @Autowired constructor(private val profile: Profile) {

    @Value("\${s3.endpointUrl:#{null}}")
    private val endpointUrl: String? = null

    @Value("\${s3.accessKey:#{null}}")
    private val accessKey: String? = null

    @Value("\${s3.secretKey:#{null}}")
    private val secretKey: String? = null

    @Value("\${s3.bucketName:#{null}}")
    private val bucketName: String? = null


    private var client: AmazonS3? = null

    fun saveLogo(logo: File, projectCode: String): String {
        validate()
        val key = logoKey(projectCode)
        val request = PutObjectRequest(bucketName, key, logo)
            .withCannedAcl(CannedAccessControlList.PublicRead)
        try {
            val client = awsClient()
            client.putObject(request)
            return client.getUrl(bucketName, key).toString() + "?v=${System.currentTimeMillis() / 1000}"
        } catch (e: Exception) {
            logger.warn("Fail to save the logo of project $projectCode", e)
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.SAVE_LOGO_FAIL))
        }
    }

    private fun logoKey(projectCode: String): String {
        return "ieod/${profile.getEnv().name.toLowerCase()}/logo/default_$projectCode.png"
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

    private fun validate() {
        if (endpointUrl.isNullOrBlank()) {
            throw RuntimeException("S3 endpoint url is empty")
        }
        if (accessKey.isNullOrBlank()) {
            throw RuntimeException("S3 access key is empty")
        }
        if (secretKey.isNullOrBlank()) {
            throw RuntimeException("S3 secret key is empty")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(S3Service::class.java)
    }
}
