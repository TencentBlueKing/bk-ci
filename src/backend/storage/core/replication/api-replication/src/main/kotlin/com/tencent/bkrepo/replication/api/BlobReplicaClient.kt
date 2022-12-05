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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.replication.api

import com.tencent.bkrepo.common.api.constant.REPLICATION_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.replication.constant.FeignResponse
import com.tencent.bkrepo.replication.pojo.blob.BlobPullRequest
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile

/**
 * 同一集群不同节点之间的blob数据同步接口
 */
@FeignClient(REPLICATION_SERVICE_NAME, contextId = "BlobReplicaClient")
interface BlobReplicaClient {

    /**
     * 从远程集群拉取文件数据
     * @param request 拉取请求
     */
    @PostMapping(BLOB_PULL_URI)
    fun pull(@RequestBody request: BlobPullRequest): FeignResponse

    /**
     * 推送文件数据到远程集群
     * @param file Multipart格式文件数据
     * @param sha256 文件sha256，用于校验
     */
    @PostMapping(BLOB_PUSH_URI, consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun push(
        @RequestPart file: MultipartFile,
        @RequestParam sha256: String,
        @RequestParam storageKey: String? = null
    ): Response<Void>

    /**
     * 检查文件数据在远程集群是否存在
     * @param sha256 文件sha256，用于校验
     * @param storageKey 存储实例key，为空表示远程集群默认存储
     */
    @GetMapping(BLOB_CHECK_URI)
    fun check(
        @RequestParam sha256: String,
        @RequestParam storageKey: String? = null
    ): Response<Boolean>

    companion object {
        const val BLOB_PULL_URI = "/replica/blob/pull"
        const val BLOB_PUSH_URI = "/replica/blob/push"
        const val BLOB_CHECK_URI = "/replica/blob/check"
    }
}
