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

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TxStoreI18nMessageServiceImpl : StoreI18nMessageServiceImpl() {

    companion object {
        private const val DEFAULT_I18N_DIR = "src/main/resources/i18n"
        private val logger = LoggerFactory.getLogger(TxStoreI18nMessageServiceImpl::class.java)
    }

    override fun getPropertiesFileStr(
        projectCode: String,
        fileDir: String,
        fileName: String,
        repositoryHashId: String?,
        branch: String?
    ): String? {
        return try {
            client.get(ServiceGitRepositoryResource::class).getFileContent(
                repoId = repositoryHashId!!,
                filePath = "$DEFAULT_I18N_DIR/$fileName",
                reversion = null,
                branch = branch,
                repositoryType = null
            ).data
        } catch (ignored: Throwable) {
            logger.warn("getPropertiesFileStr fileName:$fileName,branch:$branch error", ignored)
            null
        }
    }

    override fun getPropertiesFileNames(
        projectCode: String,
        fileDir: String,
        repositoryHashId: String?,
        branch: String?
    ): List<String>? {
        val gitRepositoryDirItems = client.get(ServiceGitRepositoryResource::class).getGitRepositoryTreeInfo(
            userId = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
            repoId = repositoryHashId!!,
            refName = branch,
            path = DEFAULT_I18N_DIR,
            tokenType = TokenTypeEnum.PRIVATE_KEY
        ).data
        return gitRepositoryDirItems?.filter { it.type != "tree" }?.map { it.name }
    }
}
