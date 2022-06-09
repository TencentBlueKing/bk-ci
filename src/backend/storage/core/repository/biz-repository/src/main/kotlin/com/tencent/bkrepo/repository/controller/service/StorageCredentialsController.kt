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

package com.tencent.bkrepo.repository.controller.service

import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.repository.api.StorageCredentialsClient
import com.tencent.bkrepo.repository.service.repo.StorageCredentialService
import org.springframework.web.bind.annotation.RestController

/**
 * 存储凭证服务接口实现类
 */
@RestController
class StorageCredentialsController(
    private val storageCredentialService: StorageCredentialService,
    private val storageProperties: StorageProperties
) : StorageCredentialsClient {
    override fun findByKey(key: String?): Response<StorageCredentials?> {
        val credentials = if (key.isNullOrBlank()) {
            storageProperties.defaultStorageCredentials()
        } else {
            storageCredentialService.findByKey(key)
        }
        return StorageCredentialsResponse.success(credentials)
    }
    /**
     * 原来的ResponseBuilder是泛型，会导致类型擦出，致使在序列化的时候抽象JsonTypeInfo注解无效，
     * type字段不会序列化，使用StorageCredentialsClient的接收方反序列化时就会报错。
     * 所以这里要使用具体类型
     * */
    class StorageCredentialsResponse(code: Int, data: StorageCredentials?) :
        Response<StorageCredentials>(code, data = data) {
        companion object {
            fun success(data: StorageCredentials?): StorageCredentialsResponse {
                return StorageCredentialsResponse(CommonMessageCode.SUCCESS.getCode(), data)
            }
        }
    }
}
