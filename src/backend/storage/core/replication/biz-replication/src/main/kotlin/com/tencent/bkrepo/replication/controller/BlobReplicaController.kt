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

package com.tencent.bkrepo.replication.controller

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.replication.api.BlobReplicaClient
import com.tencent.bkrepo.replication.pojo.blob.BlobPullRequest
import com.tencent.bkrepo.repository.api.StorageCredentialsClient
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.TimeUnit

/**
 * blob数据同步接口
 * 用于同个集群中不同节点之间blob数据同步
 */
@Principal(type = PrincipalType.ADMIN)
@RestController
class BlobReplicaController(
    storageProperties: StorageProperties,
    private val storageService: StorageService,
    private val storageCredentialsClient: StorageCredentialsClient
) {

    private val defaultCredentials = storageProperties.defaultStorageCredentials()
    private val credentialsCache: LoadingCache<String, StorageCredentials> = CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_COUNT)
        .expireAfterWrite(CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
        .build(CacheLoader.from { key -> findStorageCredentials(key) })

    @PostMapping(BlobReplicaClient.BLOB_PULL_URI)
    fun pull(@RequestBody request: BlobPullRequest): ResponseEntity<InputStreamResource> {
        with(request) {
            val credentials = credentialsCache.get(storageKey.orEmpty())
            val inputStream = storageService.load(sha256, range, credentials)
                ?: throw NotFoundException(ArtifactMessageCode.ARTIFACT_DATA_NOT_FOUND)
            return ResponseEntity.ok(InputStreamResource(inputStream))
        }
    }

    @PostMapping(BlobReplicaClient.BLOB_PUSH_URI)
    fun push(
        @RequestPart file: MultipartFile,
        @RequestParam sha256: String,
        @RequestParam storageKey: String? = null
    ): Response<Void> {
        val credentials = credentialsCache.get(storageKey.orEmpty())
        if (storageService.exist(sha256, credentials)) {
            return ResponseBuilder.success()
        }
        val artifactFile = ArtifactFileFactory.build(file, credentials)
        if (artifactFile.getFileSha256() != sha256) {
            throw ErrorCodeException(ArtifactMessageCode.DIGEST_CHECK_FAILED, "sha256")
        }
        storageService.store(sha256, artifactFile, credentials)
        return ResponseBuilder.success()
    }

    @GetMapping(BlobReplicaClient.BLOB_CHECK_URI)
    fun check(
        @RequestParam sha256: String,
        @RequestParam storageKey: String? = null
    ): Response<Boolean> {
        val credentials = credentialsCache.get(storageKey.orEmpty())
        return ResponseBuilder.success(storageService.exist(sha256, credentials))
    }

    private fun findStorageCredentials(storageKey: String?): StorageCredentials {
        if (storageKey.isNullOrBlank()) {
            return defaultCredentials
        }
        return storageCredentialsClient.findByKey(storageKey).data ?: defaultCredentials
    }

    companion object {
        private const val MAX_CACHE_COUNT = 10L
        private const val CACHE_EXPIRE_MINUTES = 5L
    }
}
