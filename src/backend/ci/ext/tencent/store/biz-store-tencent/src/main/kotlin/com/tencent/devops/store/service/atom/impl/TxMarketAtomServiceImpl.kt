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

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.TxMarketAtomService
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxMarketAtomServiceImpl : TxMarketAtomService, MarketAtomServiceImpl() {

    @Autowired
    private lateinit var storeVisibleDeptService: StoreVisibleDeptService

    private val logger = LoggerFactory.getLogger(TxMarketAtomServiceImpl::class.java)

    override fun generateAtomVisibleData(
        storeCodeList: List<String?>,
        storeType: StoreTypeEnum
    ): Result<HashMap<String, MutableList<Int>>?> {
        logger.info("generateAtomVisibleData storeCodeList is:$storeCodeList,storeType is:$storeType")
        return Result(storeVisibleDeptService.batchGetVisibleDept(storeCodeList, storeType).data)
    }

    override fun generateInstallFlag(
        defaultFlag: Boolean,
        members: MutableList<String>?,
        userId: String,
        visibleList: MutableList<Int>?,
        userDeptList: List<Int>
    ): Boolean {
        logger.info("generateInstallFlag defaultFlag is:$defaultFlag,members is:$members,userId is:$userId")
        logger.info("generateInstallFlag visibleList is:$visibleList,userDeptList is:$userDeptList")
        return if (defaultFlag || (members != null && members.contains(userId))) {
            true
        } else {
            visibleList != null && (visibleList.contains(0) || visibleList.intersect(userDeptList).count() > 0)
        }
    }

    override fun getRepositoryInfo(projectCode: String?, repositoryHashId: String?): Result<Repository?> {
        logger.info("getRepositoryInfo projectCode is :$projectCode,repositoryHashId is :$repositoryHashId")
        var repositoryInfo: Repository? = null
        // 历史插件没有代码库，不需要获取代码库信息
        if (!projectCode.isNullOrEmpty() && !repositoryHashId.isNullOrEmpty()) {
            val getGitRepositoryResult =
                client.get(ServiceRepositoryResource::class).get(projectCode!!, repositoryHashId!!, RepositoryType.ID)
            logger.info("the getGitRepositoryResult is :$getGitRepositoryResult")
            if (getGitRepositoryResult.isOk()) {
                repositoryInfo = getGitRepositoryResult.data
            } else {
                Result(getGitRepositoryResult.status, getGitRepositoryResult.message, null)
            }
        }
        logger.info("the repositoryInfo is :$repositoryInfo")
        return Result(repositoryInfo)
    }

    override fun deleteAtomRepository(
        userId: String,
        projectCode: String?,
        repositoryHashId: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        // 删除代码库信息
        if (!projectCode.isNullOrEmpty() && repositoryHashId.isNotBlank()) {
            val delGitRepositoryResult =
                client.get(ServiceGitRepositoryResource::class)
                    .delete(
                        userId = userId,
                        projectId = projectCode!!,
                        repositoryHashId = repositoryHashId,
                        tokenType = tokenType
                    )
            logger.info("the delGitRepositoryResult is :$delGitRepositoryResult")
            return delGitRepositoryResult
        }
        return Result(true)
    }
}
