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

import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.remotedev.dao.RemoteDevFileDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.pojo.OPUserSetting
import com.tencent.devops.remotedev.pojo.RemoteDevSettings
import com.tencent.devops.remotedev.service.transfer.GithubTransferService
import com.tencent.devops.remotedev.service.transfer.TGitTransferService
import org.apache.commons.codec.digest.DigestUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RemoteDevSettingService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val remoteDevFileDao: RemoteDevFileDao,
    private val tGitTransferService: TGitTransferService,
    private val githubTransferService: GithubTransferService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteDevSettingService::class.java)
    }

    fun getRemoteDevSettings(userId: String): RemoteDevSettings {
        logger.info("$userId get remote dev setting")
        val setting = remoteDevSettingDao.fetchAnySetting(dslContext, userId)

        if (setting.projectId.isBlank()) {
            kotlin.runCatching {
                client.get(ServiceTxProjectResource::class).getRemoteDevUserProject(userId)
            }.onFailure { logger.warn("create user project fail ${it.message}", it) }.getOrNull().let {
                if (it?.data == null) {
                    logger.warn("create user project fail ${it?.message}")
                }
                remoteDevSettingDao.updateProjectId(dslContext, userId, it?.data?.englishName ?: "")
                setting.projectId = it?.data?.englishName ?: ""
            }
        }

        return setting.copy(
            envsForFile = remoteDevFileDao.fetchFile(dslContext, userId),
            gitAttached = kotlin.runCatching { tGitTransferService.getAndCheckOauthToken(userId) }.isSuccess,
            githubAttached = kotlin.runCatching { githubTransferService.getAndCheckOauthToken(userId) }.isSuccess
        )
    }

    fun updateRemoteDevSettings(userId: String, setting: RemoteDevSettings): Boolean {
        logger.info("$userId get remote dev setting")
        remoteDevSettingDao.createOrUpdateSetting(dslContext, setting, userId)
        // 删除用户已去掉的文件
        remoteDevFileDao.batchDeleteFile(dslContext, setting.envsForFile.map { it.id ?: -1 }.toSet(), userId)
        // 添加or更新存在的文件
        setting.envsForFile.forEach {
            val computeMd5 = DigestUtils.md5Hex(it.content)
            when {
                it.id == null -> remoteDevFileDao.createFile(
                    dslContext = dslContext,
                    path = it.path,
                    content = it.content,
                    userId = userId,
                    md5 = computeMd5
                )
                it.md5 != computeMd5 -> remoteDevFileDao.updateFile(
                    dslContext = dslContext, file = it, md5 = computeMd5, userId = userId
                )
            }
        }
        return true
    }

    fun updateSetting4Op(data: OPUserSetting) {
        logger.info("updateSettingByOp $data")
        remoteDevSettingDao.createOrUpdateSetting4OP(dslContext, data)
    }
}
