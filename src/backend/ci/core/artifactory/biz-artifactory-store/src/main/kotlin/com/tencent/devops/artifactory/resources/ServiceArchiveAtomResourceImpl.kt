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

package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.ServiceArchiveAtomResource
import com.tencent.devops.artifactory.constant.BK_CI_ATOM_DIR
import com.tencent.devops.artifactory.constant.REALM_BK_REPO
import com.tencent.devops.artifactory.constant.REALM_LOCAL
import com.tencent.devops.artifactory.service.ArchiveAtomService
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.artifactory.util.BkRepoUtils.BKREPO_STORE_PROJECT_ID
import com.tencent.devops.artifactory.util.BkRepoUtils.REPO_NAME_PLUGIN
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@RestResource
class ServiceArchiveAtomResourceImpl @Autowired constructor(
    private val archiveFileService: ArchiveFileService,
    private val archiveAtomService: ArchiveAtomService
) : ServiceArchiveAtomResource {

    @Value("\${artifactory.realm:}")
    private var artifactoryRealm: String = ""

    override fun getAtomFileContent(filePath: String): Result<String> {
        return Result(archiveAtomService.getAtomFileContent(filePath))
    }

    override fun deleteAtomFile(userId: String, projectCode: String, atomCode: String): Result<Boolean> {
        val filePath = when (artifactoryRealm) {
            REALM_LOCAL -> "$BK_CI_ATOM_DIR/$projectCode/$atomCode"
            REALM_BK_REPO -> "$BKREPO_STORE_PROJECT_ID/$REPO_NAME_PLUGIN/$projectCode/$atomCode"
            else -> throw IllegalArgumentException("Unknown artifactory realm")
        }
        archiveFileService.deleteFile(userId, filePath)
        return Result(true)
    }
}
