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

package com.tencent.bkrepo.auth.message

import com.tencent.bkrepo.common.api.message.MessageCode

enum class AuthMessageCode(private val businessCode: Int, private val key: String) : MessageCode {

    AUTH_DUP_UID(1, "auth.dup.uid"),
    AUTH_USER_NOT_EXIST(2, "auth.user.notexist"),
    AUTH_DELETE_USER_FAILED(3, "auth.delete.user.failed"),
    AUTH_USER_TOKEN_ERROR(4, "auth.user.token.error"),
    AUTH_ROLE_NOT_EXIST(5, "auth.role.notexist"),
    AUTH_DUP_RID(6, "auth.dup.rid"),
    AUTH_DUP_PERMNAME(7, "auth.dup.permname"),
    AUTH_PERMISSION_NOT_EXIST(8, "auth.permission.notexist"),
    AUTH_PERMISSION_FAILED(9, "auth.permission.failed"),
    AUTH_USER_PERMISSION_EXIST(10, "auth.user.permission-exist"),
    AUTH_ROLE_PERMISSION_EXIST(11, "auth.role.permission-exist"),
    AUTH_DUP_APPID(12, "auth.dup.appid"),
    AUTH_APPID_NOT_EXIST(13, "auth.appid.notexist"),
    AUTH_AKSK_CHECK_FAILED(14, "auth.asksk.checkfail"),
    AUTH_DUP_CLUSTERID(15, "auth.dup.clusterid"),
    AUTH_CLUSTER_NOT_EXIST(16, "auth.cluster.notexist"),
    AUTH_PROJECT_NOT_EXIST(17, "auth.project.notexist"),
    AUTH_ASST_USER_EMPTY(18, "auth.group.asst.user.empty"),
    AUTH_USER_TOKEN_EXIST(19, "auth.user.token.exist"),
    AUTH_LOGIN_TOKEN_CHECK_FAILED(20, "auth.login.token.checkfail"),
    AUTH_REPO_NOT_EXIST(21, "auth.repo.notexist");

    override fun getBusinessCode() = businessCode
    override fun getKey() = key
    override fun getModuleCode() = 2
}
