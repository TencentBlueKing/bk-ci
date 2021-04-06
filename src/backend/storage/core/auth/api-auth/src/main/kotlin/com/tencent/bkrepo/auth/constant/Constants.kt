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

package com.tencent.bkrepo.auth.constant

/**
 * 认证相关
 */

const val PROJECT_MANAGE_ID = "project_manage"

const val PROJECT_MANAGE_NAME = "项目管理员"

const val REPO_MANAGE_ID = "repo_manage"

const val REPO_MANAGE_NAME = "仓库管理员"

const val DEFAULT_PASSWORD = "blueking"

const val AUTHORIZATION = "Authorization"

const val AUTH_FAILED_RESPONSE = "{\"code\":401,\"message\":\"Authorization value [%s] " +
    "is not a valid scheme.\",\"data\":null,\"traceId\":\"\"}"

const val BASIC_AUTH_HEADER_PREFIX = "Basic "

const val PLATFORM_AUTH_HEADER_PREFIX = "Platform "

const val RANDOM_KEY_LENGTH = 30

const val BKREPO_TICKET = "bkrepo_ticket"

const val AUTH_REPO_SUFFIX = "/create/repo"

const val AUTH_PROJECT_SUFFIX = "/create/project"

const val AUTH_CLUSTER_PREFIX = "/api/cluster"

const val AUTH_PERMISSION_PREFIX = "/permission"
const val AUTH_API_PERMISSION_PREFIX = "/api/permission"
const val AUTH_SERVICE_PERMISSION_PREFIX = "/service/permission"

const val AUTH_ROLE_PREFIX = "/role"
const val AUTH_API_ROLE_PREFIX = "/api/role"
const val AUTH_SERVICE_ROLE_PREFIX = "/service/role"

const val AUTH_USER_PREFIX = "/user"
const val AUTH_API_USER_PREFIX = "/api/user"
const val AUTH_SERVICE_USER_PREFIX = "/service/user"

const val AUTH_DEPARTMENT_PREFIX = "/department"
const val AUTH_API_DEPARTMENT_PREFIX = "/api/department"
const val AUTH_SERVICE_DEPARTMENT_PREFIX = "/service/department"

const val AUTH_ACCOUNT_PREFIX = "/account"
const val AUTH_SERVICE_ACCOUNT_PREFIX = "/service/account"
const val AUTH_API_ACCOUNT_PREFIX = "/api/account"

const val AUTH_ADMIN = "admin"
const val AUTH_BUILTIN_ADMIN = "repo_admin"
const val AUTH_BUILTIN_USER = "repo_user"
const val AUTH_BUILTIN_VIEWER = "repo_viewer"
