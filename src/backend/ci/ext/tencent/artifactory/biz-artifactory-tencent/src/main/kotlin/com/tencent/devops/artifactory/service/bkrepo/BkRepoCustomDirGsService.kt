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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.artifactory.service.bkrepo

import com.tencent.devops.artifactory.client.BkRepoClient
import com.tencent.devops.artifactory.client.JFrogApiService
import com.tencent.devops.artifactory.service.JFrogService
import com.tencent.devops.artifactory.service.CustomDirGsService
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.common.api.exception.OperationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

@Service
class BkRepoCustomDirGsService @Autowired constructor(
    private val jFrogApiService: JFrogApiService,
    private val jFrogService: JFrogService,
    private val bkRepoClient: BkRepoClient,
    private val bkRepoService: BkRepoService
) : CustomDirGsService {
    override fun getDownloadUrl(projectId: String, fileName: String, userId: String): String {
        val path = JFrogUtil.getCustomDirPath(projectId, fileName)

        bkRepoClient.getFileDetail(userId, projectId, RepoUtils.CUSTOM_REPO, path)
            ?: throw NotFoundException("文件不存在")

        // todo
        throw OperationException("not implemented")

        // return bkRepoService.internalDownloadUrl(resultPath, 3*24*3600, userId)
    }
}