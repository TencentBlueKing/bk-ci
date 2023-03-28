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

package com.tencent.devops.store.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceFeatureDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.README
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ServiceRepositoryService {
    @Autowired
    lateinit var client: Client

    @Autowired
    lateinit var extServiceDao: ExtServiceDao

    @Autowired
    lateinit var extServiceFeatureDao: ExtServiceFeatureDao

    @Autowired
    lateinit var storeMemberDao: StoreMemberDao

    @Autowired
    lateinit var dslContext: DSLContext

    private val logger = LoggerFactory.getLogger(ServiceRepositoryService::class.java)

    /**
     * 更改扩展代码库的用户信息
     * @param userId 移交的用户ID
     * @param projectCode 项目代码
     * @param serviceCode 扩展代码
     */
    fun updateServiceRepositoryUserInfo(userId: String, projectCode: String, serviceCode: String): Result<Boolean> {
        logger.info("updateServiceRepositoryUserInfo params:[$userId|$projectCode|$serviceCode]")
        // 判断用户是否是插件管理员，移交代码库只能针对插件管理员
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, serviceCode, StoreTypeEnum.ATOM.type.toByte())) {
            return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PERMISSION_DENIED,
                language = I18nUtil.getLanguage(userId))
        }
        val serviceRecord = extServiceFeatureDao.getLatestServiceByCode(dslContext, serviceCode)
            ?: return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(serviceCode),
                language = I18nUtil.getLanguage(userId)
            )
        val updateServiceRepositoryUserInfoResult = client.get(ServiceGitRepositoryResource::class)
            .updateRepositoryUserInfo(userId, projectCode, serviceRecord.repositoryHashId)
        logger.info("updateServiceRepositoryUserInfo is:$updateServiceRepositoryUserInfoResult")
        return updateServiceRepositoryUserInfoResult
    }

    fun getReadMeFile(userId: String, serviceCode: String): Result<String?> {
        val featureRecord = extServiceFeatureDao.getLatestServiceByCode(dslContext, serviceCode)
            ?: throw RuntimeException(MessageUtil.getMessageByLocale(
                messageCode = StoreMessageCode.USER_SERVICE_NOT_EXIST,
                params = arrayOf(serviceCode),
                language = I18nUtil.getLanguage(userId))
            )
        val fileStr = client.get(ServiceGitRepositoryResource::class).getFileContent(
            featureRecord.repositoryHashId,
            README, null, null, null
        ).data
        return Result(fileStr)
    }
}
