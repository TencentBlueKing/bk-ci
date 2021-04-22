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

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.repository.dao.repository.StorageCredentialsRepository
import com.tencent.bkrepo.repository.model.TStorageCredentials
import com.tencent.bkrepo.repository.pojo.credendials.StorageCredentialsCreateRequest
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
    private val storageCredentialsRepository: StorageCredentialsRepository,
    private val storageProperties: StorageProperties
) : StorageCredentialService {

    @Transactional(rollbackFor = [Throwable::class])
    override fun create(userId: String, request: StorageCredentialsCreateRequest) {
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
            credentials = request.credentials.toJsonString()
        )
        storageCredentialsRepository.save(storageCredential)
    }

    override fun findByKey(key: String): StorageCredentials? {
        val tStorageCredentials = storageCredentialsRepository.findByIdOrNull(key)
        val storageCredentials = tStorageCredentials?.credentials?.readJsonString<StorageCredentials>()
        return storageCredentials?.apply { this.key = tStorageCredentials.id }
    }

    override fun list(): List<StorageCredentials> {
        return storageCredentialsRepository.findAll().map {
            it.credentials.readJsonString<StorageCredentials>()
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun delete(key: String) {
        return storageCredentialsRepository.deleteById(key)
    }
}
