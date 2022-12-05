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

package com.tencent.bkrepo.common.storage.innercos

import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.storage.core.AbstractFileStorage
import com.tencent.bkrepo.common.storage.credentials.InnerCosCredentials
import com.tencent.bkrepo.common.storage.innercos.client.CosClient
import com.tencent.bkrepo.common.storage.innercos.request.CheckObjectExistRequest
import com.tencent.bkrepo.common.storage.innercos.request.CopyObjectRequest
import com.tencent.bkrepo.common.storage.innercos.request.DeleteObjectRequest
import com.tencent.bkrepo.common.storage.innercos.request.GetObjectRequest
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * 内部cos文件存储实现类
 */
open class InnerCosFileStorage : AbstractFileStorage<InnerCosCredentials, CosClient>() {

    override fun store(path: String, name: String, file: File, client: CosClient) {
        client.putFileObject(name, file)
    }

    override fun store(path: String, name: String, inputStream: InputStream, size: Long, client: CosClient) {
        client.putStreamObject(name, inputStream, size)
    }

    override fun load(path: String, name: String, range: Range, client: CosClient): InputStream? {
        val request = GetObjectRequest(name, range.start, range.end)
        return client.getObject(request).inputStream
    }

    override fun delete(path: String, name: String, client: CosClient) {
        return try {
            client.deleteObject(DeleteObjectRequest(name))
        } catch (ignored: IOException) {
            // ignored
        }
    }

    override fun exist(path: String, name: String, client: CosClient): Boolean {
        return try {
            return client.checkObjectExist(CheckObjectExistRequest(name))
        } catch (ignored: IOException) {
            // return false if error
            false
        }
    }

    override fun copy(path: String, name: String, fromClient: CosClient, toClient: CosClient) {
        try {
            require(fromClient.credentials.region == toClient.credentials.region)
            require(fromClient.credentials.secretId == toClient.credentials.secretId)
            require(fromClient.credentials.secretKey == toClient.credentials.secretKey)
        } catch (ignored: IllegalArgumentException) {
            throw IllegalArgumentException("Unsupported to copy object between different cos app id")
        }
        toClient.copyObject(CopyObjectRequest(fromClient.credentials.bucket, name, name))
    }

    override fun onCreateClient(credentials: InnerCosCredentials): CosClient {
        require(credentials.secretId.isNotBlank())
        require(credentials.secretKey.isNotBlank())
        require(credentials.region.isNotBlank())
        require(credentials.bucket.isNotBlank())
        return CosClient(credentials)
    }
}
