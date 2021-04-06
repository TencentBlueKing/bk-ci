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

package com.tencent.bkrepo.common.api.constant

/**
 * 认证成功后username写入request attributes的key
 */
const val USER_KEY = "userId"

/**
 * 认证成功后platform写入request attributes的key
 */
const val PLATFORM_KEY = "platformId"

/**
 * 微服务调用请求标记key
 */
const val MS_REQUEST_KEY = "MSRequest"

/**
 * 匿名用户
 */
const val ANONYMOUS_USER = "anonymous"

/**
 * common logger name
 */
const val EXCEPTION_LOGGER_NAME = "ExceptionLogger"
const val JOB_LOGGER_NAME = "JobLogger"
const val ACCESS_LOGGER_NAME = "AccessLogger"

/**
 * default pagination parameter
 */
const val DEFAULT_PAGE_NUMBER = 1
const val DEFAULT_PAGE_SIZE = 20

/**
 * service name
 */
const val REPOSITORY_SERVICE_NAME = "\${service.prefix:repo-}repository\${service.suffix:}"
const val AUTH_SERVICE_NAME = "\${service.prefix:repo-}auth\${service.suffix:}"
const val REPLICATION_SERVICE_NAME = "\${service.prefix:repo-}replication\${service.suffix:}"
