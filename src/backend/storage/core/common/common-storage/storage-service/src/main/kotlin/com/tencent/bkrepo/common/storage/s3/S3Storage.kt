/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.storage.s3

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.internal.Constants
import com.amazonaws.services.s3.model.DeleteObjectRequest
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.storage.core.AbstractFileStorage
import com.tencent.bkrepo.common.storage.credentials.S3Credentials
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.io.File
import java.io.InputStream

class S3Storage(
    private val executor: ThreadPoolTaskExecutor
) : AbstractFileStorage<S3Credentials, S3Client>() {

    private var defaultTransferManager: TransferManager? = null

    override fun store(path: String, name: String, file: File, client: S3Client) {
        val transferManager = getTransferManager(client)
        val putObjectRequest = PutObjectRequest(client.bucketName, name, file)
        val upload = transferManager.upload(putObjectRequest)
        upload.waitForCompletion()
        shutdownTransferManager(transferManager)
    }

    override fun store(path: String, name: String, inputStream: InputStream, size: Long, client: S3Client) {
        val metadata = ObjectMetadata().apply { contentLength = size }
        client.s3Client.putObject(client.bucketName, name, inputStream, metadata)
    }

    override fun load(path: String, name: String, range: Range, client: S3Client): InputStream? {
        val getObjectRequest = GetObjectRequest(client.bucketName, name)
        getObjectRequest.setRange(range.start, range.end)
        return client.s3Client.getObject(getObjectRequest).objectContent
    }

    override fun delete(path: String, name: String, client: S3Client) {
        if (exist(path, name, client)) {
            val deleteObjectRequest = DeleteObjectRequest(client.bucketName, name)
            client.s3Client.deleteObject(deleteObjectRequest)
        }
    }

    override fun exist(path: String, name: String, client: S3Client): Boolean {
        return try {
            client.s3Client.doesObjectExist(client.bucketName, name)
        } catch (ignored: Exception) {
            false
        }
    }

    override fun onCreateClient(credentials: S3Credentials): S3Client {
        require(credentials.accessKey.isNotBlank())
        require(credentials.secretKey.isNotBlank())
        require(credentials.endpoint.isNotBlank())
        require(credentials.region.isNotBlank())
        require(credentials.bucket.isNotBlank())

        val config = ClientConfiguration().apply {
            socketTimeout = 60 * 1000 // millsSecond
            maxConnections = 2048
        }
        val endpointConfig = EndpointConfiguration(credentials.endpoint, credentials.region)
        val awsCredentials = BasicAWSCredentials(credentials.accessKey, credentials.secretKey)
        val awsCredentialsProvider = AWSStaticCredentialsProvider(awsCredentials)

        val amazonS3 = AmazonS3Client.builder()
            .withEndpointConfiguration(endpointConfig)
            .withClientConfiguration(config)
            .withCredentials(awsCredentialsProvider)
            .disableChunkedEncoding()
            .withPathStyleAccessEnabled(true)
            .build()

        return S3Client(credentials.bucket, amazonS3)
    }

    private fun getTransferManager(client: S3Client): TransferManager {
        return if (client == defaultClient) {
            if (defaultTransferManager == null) {
                defaultTransferManager = createTransferManager(defaultClient)
            }
            defaultTransferManager!!
        } else {
            createTransferManager(client)
        }
    }

    private fun createTransferManager(client: S3Client): TransferManager {
        val executorService = executor.threadPoolExecutor
        return TransferManagerBuilder.standard()
            .withS3Client(client.s3Client)
            .withMultipartUploadThreshold(10L * Constants.MB)
            .withMinimumUploadPartSize(5L * Constants.MB)
            .withExecutorFactory { executorService }
            .withShutDownThreadPools(false)
            .build()
    }

    private fun shutdownTransferManager(transferManager: TransferManager) {
        if (transferManager != defaultTransferManager) {
            transferManager.shutdownNow(true)
        }
    }
}
