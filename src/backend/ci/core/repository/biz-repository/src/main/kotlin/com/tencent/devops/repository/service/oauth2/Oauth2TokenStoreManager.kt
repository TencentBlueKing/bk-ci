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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class Oauth2TokenStoreManager @Autowired constructor(
    private val oauth2TokenStoreServices: List<IOauth2TokenStoreService>,
    private val defaultOauth2TokenStoreService: DefaultOauth2TokenStoreService
) {

    fun get(userId: String, scmCode: String): OauthTokenInfo? {
        return getTokenStoreService(scmCode).get(userId, scmCode)
    }

    fun store(scmCode: String, oauthTokenInfo: OauthTokenInfo) {
        getTokenStoreService(scmCode).store(scmCode = scmCode, oauthTokenInfo = oauthTokenInfo)
    }

    fun delete(userId: String, scmCode: String, username: String) {
        getTokenStoreService(scmCode).delete(
            username = username,
            scmCode = scmCode,
            userId = userId
        )
    }

    fun list(userId: String, scmCode: String): List<OauthTokenInfo> {
        return getTokenStoreService(scmCode).list(userId, scmCode)
    }

    private fun getTokenStoreService(scmCode: String): IOauth2TokenStoreService {
        return oauth2TokenStoreServices.find {
            it.support(scmCode)
        } ?: defaultOauth2TokenStoreService
    }
}
