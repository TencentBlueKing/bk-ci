/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.artifact.manager

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.EmptyInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.util.http.HttpRangeUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.innercos.http.HttpMethod
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import org.slf4j.LoggerFactory

/**
 * 存储Manager
 *
 * 虽然[StorageService]提供了构件存储服务，但保存一个文件节点需要两步操作:
 *   1. [StorageService]保存文件数据
 *   2. [NodeClient]微服务调用创建文件节点
 * 这样会存在几个问题:
 *   1. 每个地方都会进行同样的操作，增加代码重复率
 *   2. 不支持事务，如果文件保存成功，但节点创建失败，会导致产生垃圾文件并且无法清理
 *
 * 所以提供StorageManager，简化依赖源的操作并减少错误率
 */
@Suppress("TooGenericExceptionCaught")
class StorageManager(
    private val storageService: StorageService,
    private val nodeClient: NodeClient
) {

    /**
     * 存储构件[artifactFile]到[storageCredentials]上，并根据[request]创建节点
     * 操作成功返回节点详情[NodeDetail]
     */
    fun storeArtifactFile(
        request: NodeCreateRequest,
        artifactFile: ArtifactFile,
        storageCredentials: StorageCredentials?
    ): NodeDetail {
        val affectedCount = storageService.store(request.sha256!!, artifactFile, storageCredentials)
        try {
            return nodeClient.createNode(request).data!!
        } catch (exception: Exception) {
            // 当文件有创建，则删除文件
            if (affectedCount == 1) try {
                storageService.delete(request.sha256!!, storageCredentials)
            } catch (exception: Exception) {
                logger.error("Failed to delete new created file[${request.sha256}]", exception)
            }
            // 异常往上抛
            throw exception
        }
    }

    /**
     * 加载ArtifactInputStream
     * 如果node为null，则返回null
     * 如果为head请求则返回empty input stream
     */
    fun loadArtifactInputStream(
        node: NodeDetail?,
        storageCredentials: StorageCredentials?
    ): ArtifactInputStream? {
        if (node == null || node.folder) return null
        try {
            val request = HttpContextHolder.getRequestOrNull()
            val range = request?.let { HttpRangeUtils.resolveRange(it, node.size) } ?: Range.full(node.size)
            if (node.size == 0L || request?.method == HttpMethod.HEAD.name) {
                return ArtifactInputStream(EmptyInputStream.INSTANCE, range)
            }
            return storageService.load(node.sha256.orEmpty(), range, storageCredentials)
        } catch (exception: IllegalArgumentException) {
            throw ErrorCodeException(
                status = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE,
                messageCode = CommonMessageCode.REQUEST_RANGE_INVALID
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StorageManager::class.java)
    }
}
