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

import com.tencent.devops.common.api.util.JsonUtil
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
    private val sshPublicKeysDao: SshPublicKeysDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(SshPublicKeysService::class.java)
    }

    // 新增SSH公钥
    fun createPublicKey(userId: String, sshPublicKey: SshPublicKey): Boolean {
        logger.info(
            "SshPublicKeysService|addSshPublicKey|userId" +
                "|$userId|sshPublicKey|$sshPublicKey"
        )
        if (checkSshKeyExists(sshPublicKey)) return true
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
        logger.info("SshPublicKeysService|getSshPublicKeysList|userId|$userIds")
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

    fun getSshPublicKeys4Ws(
        userIds: Set<String>
    ): String {
        val res = mapOf<String, List<String>>(
            "keys" to getSshPublicKeysList(userIds).map { it.publicKey }
        )
        logger.info("=======: ${JsonUtil.toJson(res, false)}")
        return Base64Util.encode(JsonUtil.toJson(res, false).toByteArray())
    }

    // 校验sshkey是否已存在
    fun checkSshKeyExists(sshPublicKey: SshPublicKey) = sshPublicKeysDao.getSshKeysRecord(dslContext, sshPublicKey)
        ?.let { true } ?: false
}
