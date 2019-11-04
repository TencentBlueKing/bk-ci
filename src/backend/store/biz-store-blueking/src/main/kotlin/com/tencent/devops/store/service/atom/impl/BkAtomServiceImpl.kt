/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.BkAuthProjectApi
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.BkPipelineAuthServiceCode
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.service.atom.BkAtomService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BkAtomServiceImpl : BkAtomService, AtomServiceImpl() {

    @Autowired
    lateinit var bkPipelineAuthServiceCode: BkPipelineAuthServiceCode
    @Autowired
    lateinit var bkAuthProjectApi: BkAuthProjectApi

    private val logger = LoggerFactory.getLogger(BkAtomServiceImpl::class.java)

    override fun hasManagerPermission(projectCode: String, userId: String): Boolean {
        return bkAuthProjectApi.getProjectUsers(bkPipelineAuthServiceCode, projectCode, BkAuthGroup.MANAGER)
            .contains(userId)
    }

    override fun updateRepoInfo(
        visibilityLevel: VisibilityLevelEnum?,
        dbVisibilityLevel: Int?,
        userId: String,
        repositoryHashId: String
    ): Result<Boolean> {
        logger.info("updateRepoInfo visibilityLevel is:$visibilityLevel,dbVisibilityLevel is:$dbVisibilityLevel")
        logger.info("updateRepoInfo userId is:$userId,repositoryHashId is:$repositoryHashId")
        return Result(true)
    }
}
