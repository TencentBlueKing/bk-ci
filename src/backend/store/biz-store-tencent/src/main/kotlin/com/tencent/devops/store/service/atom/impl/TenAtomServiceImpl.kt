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

package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitProjectInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.store.pojo.atom.AtomFeatureUpdateRequest
import com.tencent.devops.store.service.atom.TenAtomService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TenAtomServiceImpl : TenAtomService, AtomServiceImpl() {

    @Autowired
    lateinit var bsPipelineAuthServiceCode: BSPipelineAuthServiceCode
    @Autowired
    lateinit var bsAuthProjectApi: BSAuthProjectApi

    private val logger = LoggerFactory.getLogger(TenAtomServiceImpl::class.java)

    /**
     * 把项目迁移到指定项目组下
     */
    override fun moveGitProjectToGroup(
        userId: String,
        groupCode: String?,
        atomCode: String
    ): Result<Boolean> {
        logger.info("moveGitProjectToGroup userId is:$userId, groupCode is:$groupCode, atomCode is:$atomCode")
        val atomRecord = atomDao.getRecentAtomByCode(dslContext, atomCode)
        logger.info("the atomRecord is:$atomRecord")
        if (null == atomRecord) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(atomCode), false)
        }
        val moveProjectToGroupResult: Result<GitProjectInfo?>
        return try {
            moveProjectToGroupResult = client.get(ServiceRepositoryResource::class)
                .moveGitProjectToGroup(userId, groupCode, atomRecord.repositoryHashId, TokenTypeEnum.PRIVATE_KEY)
            logger.info("moveProjectToGroupResult is :$moveProjectToGroupResult")
            if (moveProjectToGroupResult.isOk()) {
                val gitProjectInfo = moveProjectToGroupResult.data!!
                // 批量更新插件数据库的代码地址信息
                atomDao.updateAtomByCode(dslContext, userId, atomCode, AtomFeatureUpdateRequest(gitProjectInfo.repositoryUrl))
                Result(true)
            } else {
                Result(moveProjectToGroupResult.status, moveProjectToGroupResult.message ?: "")
            }
        } catch (e: Exception) {
            logger.error("moveProjectToGroupResult error is :$e", e)
            MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        }
    }

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
        logger.info("updateRepoInfo visibilityLevel is:$visibilityLevel,dbVisibilityLevel is:$dbVisibilityLevel")
        logger.info("updateRepoInfo userId is:$userId,repositoryHashId is:$repositoryHashId")
        if (null != visibilityLevel && visibilityLevel.level != dbVisibilityLevel) {
            // 更新git代码库可见范围
            val updateGitRepositoryResult: Result<Boolean>
            try {
                updateGitRepositoryResult = client.get(ServiceRepositoryResource::class).updateGitCodeRepository(
                    userId,
                    repositoryHashId,
                    UpdateGitProjectInfo(
                        visibilityLevel = visibilityLevel.level,
                        forkEnabled = visibilityLevel == VisibilityLevelEnum.LOGIN_PUBLIC
                    ),
                    TokenTypeEnum.PRIVATE_KEY
                )
            } catch (e: Exception) {
                logger.error("updateGitCodeRepository error  is :$e", e)
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
            }
            logger.info("the updateGitRepositoryResult is :$updateGitRepositoryResult")
            return updateGitRepositoryResult
        }
        return Result(true)
    }
}
