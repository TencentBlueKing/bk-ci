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

package com.tencent.bkrepo.common.api.message

enum class CommonMessageCode(private val key: String) : MessageCode {

    SUCCESS("success") { override fun getCode() = 0 },

    SYSTEM_ERROR("system.error"),
    PARAMETER_MISSING("system.parameter.missing"),
    PARAMETER_EMPTY("system.parameter.empty"),
    PARAMETER_INVALID("system.parameter.invalid"),
    REQUEST_CONTENT_INVALID("system.request.content.invalid"),
    RESOURCE_EXISTED("system.resource.existed"),
    RESOURCE_NOT_FOUND("system.resource.not-found"),
    RESOURCE_EXPIRED("system.resource.expired"),
    METHOD_NOT_ALLOWED("system.method.not-allowed"),
    REQUEST_DENIED("system.request.denied"),
    REQUEST_UNAUTHENTICATED("system.request.unauthenticated"),
    SERVICE_CIRCUIT_BREAKER("system.service.circuit-breaker"),
    SERVICE_CALL_ERROR("system.service.call-error"),
    SERVICE_UNAUTHENTICATED("system.service.unauthenticated"),
    HEADER_MISSING("system.header.missing"),
    MEDIA_TYPE_UNSUPPORTED("system.media-type.unsupported"),
    REQUEST_RANGE_INVALID("system.request-range.invalid"),
    MODIFY_PASSWORD_FAILED("modify.password.failed"),
    ;

    override fun getBusinessCode() = ordinal + 1
    override fun getKey() = key
    override fun getModuleCode() = 1
}
