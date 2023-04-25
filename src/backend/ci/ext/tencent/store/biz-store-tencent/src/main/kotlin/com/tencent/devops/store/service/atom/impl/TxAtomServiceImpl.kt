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

package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.store.service.atom.TxAtomService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxAtomServiceImpl : TxAtomService, AtomServiceImpl() {

    @Autowired
    lateinit var bsPipelineAuthServiceCode: BSPipelineAuthServiceCode
    @Autowired
    lateinit var bsAuthProjectApi: AuthProjectApi

    private val logger = LoggerFactory.getLogger(TxAtomServiceImpl::class.java)

    override fun hasManagerPermission(projectCode: String, userId: String): Boolean {
        return bsAuthProjectApi.getProjectUsers(bsPipelineAuthServiceCode, projectCode, BkAuthGroup.MANAGER)
            .contains(userId)
    }

    override fun updateRepoInfo(
        visibilityLevel: VisibilityLevelEnum?,
        dbVisibilityLevel: Int?,
        userId: String,
        repositoryHashId: String
    ): Result<Boolean> {
        logger.info("updateRepoInfo params:[$visibilityLevel|$dbVisibilityLevel|$userId|$repositoryHashId]")
        if (null != visibilityLevel && visibilityLevel.level != dbVisibilityLevel) {
            // 更新git代码库可见范围
            val updateGitRepositoryResult: Result<Boolean>
            try {
                updateGitRepositoryResult = client.get(ServiceGitRepositoryResource::class).updateGitCodeRepository(
                    userId,
                    repositoryHashId,
                    UpdateGitProjectInfo(
                        visibilityLevel = visibilityLevel.level,
                        forkEnabled = visibilityLevel == VisibilityLevelEnum.LOGIN_PUBLIC
                    ),
                    TokenTypeEnum.PRIVATE_KEY
                )
            } catch (ignored: Throwable) {
                logger.warn("updateGitCodeRepository fail!", ignored)
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
            }
            logger.info("the updateGitRepositoryResult is :$updateGitRepositoryResult")
            return updateGitRepositoryResult
        }
        return Result(true)
    }
}
