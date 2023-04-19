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
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomRepositoryService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AtomRepositoryServiceImpl : AtomRepositoryService {

    @Autowired
    lateinit var client: Client

    @Autowired
    lateinit var marketAtomDao: MarketAtomDao

    @Autowired
    lateinit var storeMemberDao: StoreMemberDao

    @Autowired
    lateinit var dslContext: DSLContext

    private val logger = LoggerFactory.getLogger(AtomRepositoryServiceImpl::class.java)

    /**
     * 更改插件代码库的用户信息
     * @param userId 移交的用户ID
     * @param projectCode 项目代码
     * @param atomCode 插件代码
     */
    override fun updateAtomRepositoryUserInfo(userId: String, projectCode: String, atomCode: String): Result<Boolean> {
        logger.info("updateAtomRepositoryUserInfo userId is:$userId,projectCode is:$projectCode,atomCode is:$atomCode")
        // 判断用户是否是插件管理员，移交代码库只能针对插件管理员
        if (!storeMemberDao.isStoreAdmin(
                dslContext = dslContext,
                userId = userId,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM.type.toByte())
        ) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val atomRecord = marketAtomDao.getLatestAtomByCode(dslContext, atomCode)
            ?: return MessageCodeUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomCode)
            )
        val updateAtomRepositoryUserInfoResult = client.get(ServiceGitRepositoryResource::class)
            .updateRepositoryUserInfo(userId, projectCode, atomRecord.repositoryHashId)
        logger.info("updateAtomRepositoryUserInfoResult is:$updateAtomRepositoryUserInfoResult")
        return updateAtomRepositoryUserInfoResult
    }
}
