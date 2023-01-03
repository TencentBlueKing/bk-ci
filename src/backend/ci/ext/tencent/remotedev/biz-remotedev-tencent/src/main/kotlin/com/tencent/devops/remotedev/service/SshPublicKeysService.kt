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

package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.SshPublicKeysDao
import com.tencent.devops.remotedev.pojo.SshPublicKey
import org.jolokia.util.Base64Util
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SshPublicKeysService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val sshPublicKeysDao: SshPublicKeysDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(SshPublicKeysService::class.java)
    }

    // 新增SSH公钥
    fun createPublicKey(userId: String, sshPublicKey: SshPublicKey): Boolean {
        logger.info(
            "SshPublicKeysService|addSshPublicKey|userId" +
                "|${userId}|sshPublicKey|${sshPublicKey}"
        )
        // 校验 user信息是否存在
        checkCommonUser(userId)
        // ssh key信息写入DB
        sshPublicKeysDao.createSshKey(
            dslContext = dslContext,
            sshPublicKey = sshPublicKey
        )
        return true
    }

    // 获取用户的SSH公钥列表
    fun getSshPublicKeysList(
        userIds: Set<String>
    ): List<SshPublicKey> {
        logger.info("SshPublicKeysService|getSshPublicKeysList|userId|${userIds}")
        userIds.forEach {
            checkCommonUser(it)
        }
        val result = mutableListOf<SshPublicKey>()
        sshPublicKeysDao.queryUserSshKeys(
            dslContext = dslContext,
            users = userIds
        ).forEach {
            result.add(
                SshPublicKey(
                    user = it.user,
                    publicKey = String(Base64Util.decode(it.publicKey))
                )
            )
        }
        return result
    }

    // 校验用户是否存在
    fun checkCommonUser(userId: String) {
        // get接口先查本地，再查tof
        val userResult = client.get(ServiceTxUserResource::class).get(userId)
        if (userResult.isNotOk()) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.USER_NOT_EXISTS.errorCode.toString(),
                defaultMessage = ErrorCodeEnum.USER_NOT_EXISTS.formatErrorMessage.format(userId)
            )
        }
    }
}
