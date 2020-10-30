/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.  
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.api.message

enum class CommonMessageCode(private val businessCode: Int, private val key: String) : MessageCode {

    SYSTEM_ERROR(1, "system.error"),
    PARAMETER_MISSING(2, "system.parameter.missing"),
    PARAMETER_INVALID(3, "system.parameter.invalid"),
    REQUEST_CONTENT_INVALID(4, "system.request.content.invalid"),
    RESOURCE_EXISTED(5, "system.resource.existed"),
    RESOURCE_NOT_FOUND(6, "system.resource.notfound"),
    RESOURCE_EXPIRED(7, "system.resource.expired"),
    OPERATION_UNSUPPORTED(8, "system.operation.unsupported"),
    PERMISSION_DENIED(9, "system.permission.denied"),
    SERVICE_CIRCUIT_BREAKER(10, "system.service.circuit-breaker"),
    SERVICE_CALL_ERROR(11, "system.service.call-error"),
    SERVICE_UNAUTHENTICATED(12, "system.service.unauthenticated"),
    HEADER_MISSING(13, "system.header.missing"),
    MEDIA_TYPE_UNSUPPORTED(14, "system.media.type.unsupported"),
    SUCCESS(0, "success") { override fun getCode() = 0 };

    override fun getBusinessCode() = businessCode
    override fun getKey() = key
    override fun getModuleCode() = 1
}
