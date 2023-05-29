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

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.constant.BKREPO_DEFAULT_USER
import com.tencent.devops.artifactory.constant.BKREPO_STORE_PROJECT_ID
import com.tencent.devops.artifactory.constant.REPO_NAME_PLUGIN
import org.springframework.stereotype.Service
import java.net.URLEncoder

@Service
class SampleStoreI18nMessageServiceImpl : StoreI18nMessageServiceImpl() {

    override fun getPropertiesFileStr(
        projectCode: String,
        fileDir: String,
        i18nDir: String,
        fileName: String,
        repositoryHashId: String?,
        branch: String?
    ): String? {
        val filePath =
            URLEncoder.encode("$projectCode/$fileDir/$i18nDir/$fileName", Charsets.UTF_8.name())
        return client.get(ServiceArtifactoryResource::class).getFileContent(
            userId = BKREPO_DEFAULT_USER,
            projectId = BKREPO_STORE_PROJECT_ID,
            repoName = REPO_NAME_PLUGIN,
            filePath = filePath
        ).data
    }

    override fun getPropertiesFileNames(
        projectCode: String,
        fileDir: String,
        i18nDir: String,
        repositoryHashId: String?,
        branch: String?
    ): List<String>? {
        val filePath = URLEncoder.encode("$projectCode/$fileDir/$i18nDir", Charsets.UTF_8.name())
        return client.get(ServiceArtifactoryResource::class).listFileNamesByPath(
            userId = BKREPO_DEFAULT_USER,
            projectId = BKREPO_STORE_PROJECT_ID,
            repoName = REPO_NAME_PLUGIN,
            filePath = filePath
        ).data
    }
}
