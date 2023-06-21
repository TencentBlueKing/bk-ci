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

package com.tencent.devops.artifactory.service.impl

import com.tencent.devops.artifactory.config.BkRepoStoreConfig
import com.tencent.devops.artifactory.constant.REALM_BK_REPO
import com.tencent.devops.artifactory.pojo.enums.BkRepoEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "artifactory", name = ["realm"], havingValue = REALM_BK_REPO)
class TxArchiveAtomToBkRepoServiceImpl : ArchiveAtomToBkRepoServiceImpl() {

    @Autowired
    private lateinit var bkRepoStoreConfig: BkRepoStoreConfig

    override fun getBkRepoProjectId(): String {
        return bkRepoStoreConfig.bkrepoStoreProjectName
    }

    override fun getBkRepoName(): String {
        return BkRepoEnum.PLUGIN.repoName
    }

    override fun deleteAtom(userId: String, projectCode: String, atomCode: String) {
        bkRepoClient.delete(
            userId = bkRepoStoreConfig.bkrepoStoreUserName,
            projectId = bkRepoStoreConfig.bkrepoStoreProjectName,
            repoName = BkRepoEnum.PLUGIN.repoName,
            path = atomCode
        )
    }
}
