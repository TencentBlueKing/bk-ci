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

package com.tencent.bkrepo.repository.controller.service

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.api.MetadataClient
import com.tencent.bkrepo.repository.pojo.metadata.MetadataDeleteRequest
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import com.tencent.bkrepo.repository.service.MetadataService
import org.springframework.web.bind.annotation.RestController

/**
 * 元数据服务接口实现类
 */
@RestController
class MetadataController(
    private val metadataService: MetadataService
) : MetadataClient {

    override fun listMetadata(projectId: String, repoName: String, fullPath: String): Response<Map<String, Any>> {
        return ResponseBuilder.success(metadataService.listMetadata(projectId, repoName, fullPath))
    }

    override fun saveMetadata(request: MetadataSaveRequest): Response<Void> {
        metadataService.saveMetadata(request)
        return ResponseBuilder.success()
    }

    override fun deleteMetadata(request: MetadataDeleteRequest): Response<Void> {
        metadataService.deleteMetadata(request)
        return ResponseBuilder.success()
    }

    override fun save(request: MetadataSaveRequest): Response<Void> {
        return saveMetadata(request)
    }

    override fun delete(request: MetadataDeleteRequest): Response<Void> {
        return deleteMetadata(request)
    }
}
