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

package com.tencent.devops.remotedev.resources.remotedev

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.remotedev.RemoteDevResource
import com.tencent.devops.remotedev.pojo.RemoteDevOauthBack
import com.tencent.devops.remotedev.service.GitTransferService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.utils.RsaUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.util.Base64

@RestResource
class RemoteDevResourceImpl @Autowired constructor(
    private val gitTransferService: GitTransferService,
    private val workspaceService: WorkspaceService
) : RemoteDevResource {

    @Value("\${remoteDev.callBackSignSecret:}")
    private val signSecret: String = ""

    override fun oauth(
        signature: String,
        key: String,
        userId: String,
        workspaceName: String,
        timestamp: String
    ): Result<RemoteDevOauthBack> {
        if (!checkSignature(signature, key, timestamp)) {
            return Result(403, "Forbidden request")
        }
        val rsaPublicKey = RsaUtil.generatePublicKey(Base64.getDecoder().decode(key))

        // TODO 根据 workspaceName 查找 git 平台以区分不同的oauth
        val oauth = gitTransferService.getAndCheckOauthToken(userId)
        return Result(
            RemoteDevOauthBack(
                host = workspaceService.getWorkspaceHost(workspaceName),
                value = RsaUtil.rsaEncrypt(oauth.accessToken, rsaPublicKey)
            )
        )
    }

    private fun checkSignature(signature: String, key: String, timestamp: String): Boolean {
        val genSignature = ShaUtils.hmacSha1(signSecret.toByteArray(), (key + timestamp).toByteArray())
        logger.info("signature($signature) and generate signature ($genSignature)")
        if (!ShaUtils.isEqual(signature, genSignature)) {
            logger.warn("signature($signature) and generate signature ($genSignature) not match")
            return false
        }

        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteDevResourceImpl::class.java)
    }
}
