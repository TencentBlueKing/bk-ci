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

package com.tencent.bkrepo.repository.service.repo.impl

import com.tencent.bkrepo.common.api.exception.BadRequestException
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.dao.repository.StorageCredentialsRepository
import com.tencent.bkrepo.repository.message.RepositoryMessageCode
import com.tencent.bkrepo.repository.model.TStorageCredentials
import com.tencent.bkrepo.repository.pojo.credendials.StorageCredentialsCreateRequest
import com.tencent.bkrepo.repository.pojo.credendials.StorageCredentialsUpdateRequest
import com.tencent.bkrepo.repository.service.repo.StorageCredentialService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 存储凭证服务实现类
 */
@Service
class StorageCredentialServiceImpl(
    private val repositoryDao: RepositoryDao,
    private val storageCredentialsRepository: StorageCredentialsRepository,
    private val storageProperties: StorageProperties
) : StorageCredentialService {

    @Transactional(rollbackFor = [Throwable::class])
    override fun create(userId: String, request: StorageCredentialsCreateRequest): StorageCredentials {
        takeIf { request.key.isNotBlank() } ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "key")
        // 目前的实现方式有个限制：新增的存储方式和默认的存储方式必须相同
        if (storageProperties.defaultStorageCredentials()::class != request.credentials::class) {
            throw throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "type")
        }
        storageCredentialsRepository.findByIdOrNull(request.key)?.run {
            throw ErrorCodeException(CommonMessageCode.RESOURCE_EXISTED, request.key)
        }
        val storageCredential = TStorageCredentials(
            id = request.key,
            createdBy = userId,
            createdDate = LocalDateTime.now(),
            lastModifiedBy = userId,
            lastModifiedDate = LocalDateTime.now(),
            credentials = request.credentials.toJsonString(),
            region = request.region
        )
        val savedCredentials = storageCredentialsRepository.save(storageCredential)
        return convert(savedCredentials)
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun update(userId: String, request: StorageCredentialsUpdateRequest): StorageCredentials {
        requireNotNull(request.key)
        val tStorageCredentials = storageCredentialsRepository.findByIdOrNull(request.key!!)
            ?: throw NotFoundException(RepositoryMessageCode.STORAGE_CREDENTIALS_NOT_FOUND)
        val storageCredentials = tStorageCredentials.credentials.readJsonString<StorageCredentials>()

        storageCredentials.apply {
            cache = cache.copy(
                loadCacheFirst = request.credentials.cache.loadCacheFirst,
                expireDays = request.credentials.cache.expireDays
            )
            upload = upload.copy(localPath = request.credentials.upload.localPath)
        }

        tStorageCredentials.credentials = storageCredentials.toJsonString()
        val updatedCredentials = storageCredentialsRepository.save(tStorageCredentials)
        return convert(updatedCredentials)
    }

    override fun findByKey(key: String): StorageCredentials? {
        return storageCredentialsRepository.findByIdOrNull(key)?.let { convert(it) }
    }

    override fun list(region: String?): List<StorageCredentials> {
        return storageCredentialsRepository.findAll()
            .filter { region.isNullOrBlank() || it.region == region }
            .map { convert(it) }
    }

    override fun default(): StorageCredentials {
        return storageProperties.defaultStorageCredentials()
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun delete(key: String) {
        if (!storageCredentialsRepository.existsById(key)) {
            throw NotFoundException(RepositoryMessageCode.STORAGE_CREDENTIALS_NOT_FOUND)
        }
        val credentialsCount = storageCredentialsRepository.count()
        if (repositoryDao.existsByCredentialsKey(key) || credentialsCount <= 1) {
            throw BadRequestException(RepositoryMessageCode.STORAGE_CREDENTIALS_IN_USE)
        }
        // 可能判断完凭证未被使用后，删除凭证前，又有新增的仓库使用凭证，出现这种情况后需要修改新增仓库的凭证
        return storageCredentialsRepository.deleteById(key)
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun forceDelete(key: String) {
        return storageCredentialsRepository.deleteById(key)
    }

    private fun convert(credentials: TStorageCredentials): StorageCredentials {
        return credentials.credentials.readJsonString<StorageCredentials>().apply { this.key = credentials.id }
    }
}
