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

package com.tencent.devops.store.common.service.impl

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.constant.BKREPO_DEFAULT_USER
import com.tencent.devops.artifactory.pojo.enums.BkRepoEnum
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import java.net.URLEncoder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class TxStoreI18nMessageServiceImpl : StoreI18nMessageServiceImpl() {

    @Value("\${store.bkrepo.projectId:bk-store}")
    private val bkrepoStoreProjectId: String = "bk-store"

    companion object {
        private val logger = LoggerFactory.getLogger(TxStoreI18nMessageServiceImpl::class.java)
    }

    override fun getFileStr(
        projectCode: String,
        fileDir: String,
        fileName: String,
        repositoryHashId: String?,
        branch: String?
    ): String? {
        logger.info("getFileStr repositoryHashId:$repositoryHashId")
        return if (!repositoryHashId.isNullOrBlank()) {
            // 从工蜂拉取文件
            try {
                client.get(ServiceGitRepositoryResource::class).getFileContent(
                    repoId = repositoryHashId,
                    filePath = fileName,
                    reversion = null,
                    branch = branch,
                    repositoryType = null
                ).data
            } catch (ignored: Throwable) {
                logger.warn("getPropertiesFileStr fileName:$fileName,branch:$branch error", ignored)
                null
            }
        } else {
            try {
                // 直接从仓库拉取文件
                val filePath =
                    URLEncoder.encode("$projectCode/$fileDir/$fileName", Charsets.UTF_8.name())
                return client.get(ServiceArtifactoryResource::class).getFileContent(
                    userId = BKREPO_DEFAULT_USER,
                    projectId = bkrepoStoreProjectId,
                    repoName = BkRepoEnum.PLUGIN.repoName,
                    filePath = filePath
                ).data
            } catch (ignored: Throwable) {
                logger.warn("getPropertiesFileStr ffilePath:${"$projectCode/$fileDir/$fileName"} error", ignored)
                null
            }
        }
    }
}
