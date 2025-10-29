/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.repository.service.oauth2

import com.tencent.devops.repository.pojo.oauth.OauthTokenInfo

/**
 * oauth2 token存储服务
 */
interface IOauth2TokenStoreService {
    fun support(scmCode: String): Boolean

    fun get(userId: String, scmCode: String): OauthTokenInfo?

    fun store(
        scmCode: String,
        oauthTokenInfo: OauthTokenInfo
    )

    /**
     * 删除指定用户名的OAUTH信息
     * @param username 用户名(server端用户名)
     * @param scmCode 仓库标识
     * @param userId 用户ID(蓝盾用户ID)
     */
    fun delete(userId: String, scmCode: String, username: String)

    /**
     * 获取目标用户下管理的所有OAUTH信息
     * @param userId 用户ID(蓝盾用户ID)
     */
    fun list(userId: String, scmCode: String): List<OauthTokenInfo>
}
