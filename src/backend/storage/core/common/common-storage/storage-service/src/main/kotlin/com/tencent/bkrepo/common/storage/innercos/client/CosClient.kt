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

package com.tencent.bkrepo.common.storage.innercos.client

import com.tencent.bkrepo.common.storage.credentials.InnerCosCredentials
import com.tencent.bkrepo.common.storage.innercos.exception.InnerCosException
import com.tencent.bkrepo.common.storage.innercos.http.CosHttpClient
import com.tencent.bkrepo.common.storage.innercos.request.AbortMultipartUploadRequest
import com.tencent.bkrepo.common.storage.innercos.request.CheckObjectExistRequest
import com.tencent.bkrepo.common.storage.innercos.request.CompleteMultipartUploadRequest
import com.tencent.bkrepo.common.storage.innercos.request.CopyObjectRequest
import com.tencent.bkrepo.common.storage.innercos.request.CosRequest
import com.tencent.bkrepo.common.storage.innercos.request.DeleteObjectRequest
import com.tencent.bkrepo.common.storage.innercos.request.GetObjectRequest
import com.tencent.bkrepo.common.storage.innercos.request.InitiateMultipartUploadRequest
import com.tencent.bkrepo.common.storage.innercos.request.PartETag
import com.tencent.bkrepo.common.storage.innercos.request.PutObjectRequest
import com.tencent.bkrepo.common.storage.innercos.request.UploadPartRequest
import com.tencent.bkrepo.common.storage.innercos.request.UploadPartRequestFactory
import com.tencent.bkrepo.common.storage.innercos.response.CopyObjectResponse
import com.tencent.bkrepo.common.storage.innercos.response.CosObject
import com.tencent.bkrepo.common.storage.innercos.response.PutObjectResponse
import com.tencent.bkrepo.common.storage.innercos.response.handler.CheckObjectExistResponseHandler
import com.tencent.bkrepo.common.storage.innercos.response.handler.CompleteMultipartUploadResponseHandler
import com.tencent.bkrepo.common.storage.innercos.response.handler.CopyObjectResponseHandler
import com.tencent.bkrepo.common.storage.innercos.response.handler.GetObjectResponseHandler
import com.tencent.bkrepo.common.storage.innercos.response.handler.InitiateMultipartUploadResponseHandler
import com.tencent.bkrepo.common.storage.innercos.response.handler.PutObjectResponseHandler
import com.tencent.bkrepo.common.storage.innercos.response.handler.UploadPartResponseHandler
import com.tencent.bkrepo.common.storage.innercos.response.handler.VoidResponseHandler
import com.tencent.bkrepo.common.storage.innercos.retry
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.ceil
import kotlin.math.max

/**
 * Cos Client
 */
class CosClient(val credentials: InnerCosCredentials) {
    private val config: ClientConfig = ClientConfig(credentials)

    /**
     * 单连接获取文件
     */
    fun getObject(cosRequest: GetObjectRequest): CosObject {
        val httpRequest = buildHttpRequest(cosRequest)
        return CosHttpClient.execute(httpRequest, GetObjectResponseHandler())
    }

    /**
     * 单连接上传InputStream
     */
    fun putStreamObject(key: String, inputStream: InputStream, length: Long): PutObjectResponse {
        return inputStream.use { putObject(PutObjectRequest(key, it, length)) }
    }

    /**
     * 分片上传
     */
    fun putFileObject(key: String, file: File): PutObjectResponse {
        if (!file.exists()) {
            throw InnerCosException("File[$file] does not exist.")
        }
        val length = file.length()
        return if (shouldUseMultipartUpload(length)) {
            multipartUpload(key, file)
        } else {
            putObject(PutObjectRequest(key, file.inputStream(), length))
        }
    }

    /**
     * 单连接上传InputStream
     */
    private fun putObject(cosRequest: PutObjectRequest): PutObjectResponse {
        val httpRequest = buildHttpRequest(cosRequest)
        return CosHttpClient.execute(httpRequest, PutObjectResponseHandler())
    }

    /**
     * 当文件不存在时，也会执行成功，返回200
     */
    fun deleteObject(cosRequest: DeleteObjectRequest) {
        val httpRequest = buildHttpRequest(cosRequest)
        CosHttpClient.execute(httpRequest, VoidResponseHandler())
    }

    /**
     * 检查文件是否存在
     * 文件不存在时，cos返回404
     */
    fun checkObjectExist(cosRequest: CheckObjectExistRequest): Boolean {
        val httpRequest = buildHttpRequest(cosRequest)
        return CosHttpClient.execute(httpRequest, CheckObjectExistResponseHandler())
    }

    /**
     * 复制文件
     */
    fun copyObject(cosRequest: CopyObjectRequest): CopyObjectResponse {
        val httpRequest = buildHttpRequest(cosRequest)
        return CosHttpClient.execute(httpRequest, CopyObjectResponseHandler())
    }

    private fun multipartUpload(key: String, file: File): PutObjectResponse {
        // 计算分片大小
        val length = file.length()
        val optimalPartSize = calculateOptimalPartSize(length)
        // 获取uploadId
        val uploadId = initiateMultipartUpload(key)
        // 生成分片请求
        val factory = UploadPartRequestFactory(key, uploadId, optimalPartSize, file, length)
        val futureList = mutableListOf<Future<PartETag>>()
        while (factory.hasMoreRequests()) {
            val uploadPartRequest = factory.nextUploadPartRequest()
            val future = executors.submit(uploadPart(uploadPartRequest))
            futureList.add(future)
        }
        // 等待所有完成
        try {
            val partETagList = futureList.map { it.get() }
            return completeMultipartUpload(key, uploadId, partETagList)
        } catch (exception: IOException) {
            cancelFutureList(futureList)
            abortMultipartUpload(key, uploadId)
            throw exception
        }
    }

    private fun initiateMultipartUpload(key: String): String {
        val cosRequest = InitiateMultipartUploadRequest(key)
        val httpRequest = buildHttpRequest(cosRequest)
        return CosHttpClient.execute(httpRequest, InitiateMultipartUploadResponseHandler())
    }

    private fun uploadPart(cosRequest: UploadPartRequest): Callable<PartETag> {
        return Callable {
            retry(RETRY_COUNT) {
                val httpRequest = buildHttpRequest(cosRequest)
                val uploadPartResponse = CosHttpClient.execute(httpRequest, UploadPartResponseHandler())
                PartETag(cosRequest.partNumber, uploadPartResponse.eTag)
            }
        }
    }

    private fun completeMultipartUpload(
        key: String,
        uploadId: String,
        partETagList: List<PartETag>
    ): PutObjectResponse {
        retry(RETRY_COUNT) {
            val cosRequest = CompleteMultipartUploadRequest(key, uploadId, partETagList)
            val httpRequest = buildHttpRequest(cosRequest)
            return CosHttpClient.execute(httpRequest, CompleteMultipartUploadResponseHandler())
        }
    }

    private fun abortMultipartUpload(key: String, uploadId: String) {
        val cosRequest = AbortMultipartUploadRequest(key, uploadId)
        val httpRequest = buildHttpRequest(cosRequest)
        try {
            return CosHttpClient.execute(httpRequest, VoidResponseHandler())
        } catch (ignored: IOException) {
        }
    }

    private fun cancelFutureList(futures: List<Future<PartETag>>) {
        for (future in futures) {
            if (!future.isDone) {
                future.cancel(true)
            }
        }
    }

    private fun buildHttpRequest(cosRequest: CosRequest): Request {
        cosRequest.sign(credentials, config)
        return Request.Builder()
            .method(cosRequest.method.name, cosRequest.buildRequestBody())
            .url(cosRequest.url)
            .apply { cosRequest.headers.forEach { (key, value) -> this.header(key, value) } }
            .build()
    }

    private fun shouldUseMultipartUpload(length: Long): Boolean {
        return length > config.multipartUploadThreshold
    }

    private fun calculateOptimalPartSize(length: Long): Long {
        val optimalPartSize = length.toDouble() / config.maxUploadParts
        return max(ceil(optimalPartSize).toLong(), config.minimumUploadPartSize)
    }

    companion object {
        private val executors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2)
        private const val RETRY_COUNT = 5
    }
}
