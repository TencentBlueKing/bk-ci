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

package com.tencent.bkrepo.common.security.constant

/**
 * 认证相关
 */
const val BASIC_AUTH_PREFIX = "Basic "
const val BASIC_AUTH_PROMPT = "Basic realm=\"Authentication Required\""
const val PLATFORM_AUTH_PREFIX = "Platform "
const val BEARER_AUTH_PREFIX = "Bearer "
const val AUTH_HEADER_UID = "X-BKREPO-UID"
const val OAUTH_AUTH_PREFIX = "Oauth"

/**
 * micro service header user id key
 */
const val MS_AUTH_HEADER_UID = "X-BKREPO-MS-UID"

/**
 * micro service header security token
 */
const val MS_AUTH_HEADER_SECURITY_TOKEN = "X-BKREPO-SECURITY-TOKEN"

const val ANY_URI_PATTERN = "/**"
