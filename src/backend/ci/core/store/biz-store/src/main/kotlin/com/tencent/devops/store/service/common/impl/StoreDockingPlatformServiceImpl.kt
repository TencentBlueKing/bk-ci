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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.store.dao.common.StoreDockingPlatformDao
import com.tencent.devops.store.pojo.common.StoreDockingPlatformInfo
import com.tencent.devops.store.pojo.common.StoreDockingPlatformRequest
import com.tencent.devops.store.service.common.StoreDockingPlatformService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreDockingPlatformServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeDockingPlatformDao: StoreDockingPlatformDao
) : StoreDockingPlatformService {

    override fun create(userId: String, storeDockingPlatformRequest: StoreDockingPlatformRequest): Boolean {
        val platformCode = storeDockingPlatformRequest.platformCode
        // 判断平台代码是否存在
        val codeCount = storeDockingPlatformDao.countByCode(dslContext, platformCode)
        if (codeCount > 0) {
            // 抛出错误提示
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(platformCode)
            )
        }
        val platformName = storeDockingPlatformRequest.platformName
        // 判断平台名称是否存在
        val nameCount = storeDockingPlatformDao.countByName(dslContext, platformName)
        if (nameCount > 0) {
            // 抛出错误提示
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(platformName)
            )
        }
        val errorCodePrefix = storeDockingPlatformRequest.errorCodePrefix
        // 判断平台所属错误码前缀是否存在
        val errorCodePrefixCount = storeDockingPlatformDao.countByErrorCodePrefix(dslContext, errorCodePrefix)
        if (errorCodePrefixCount > 0) {
            // 抛出错误提示
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf("errorCodePrefix:$errorCodePrefix")
            )
        }
        storeDockingPlatformDao.add(dslContext, userId, storeDockingPlatformRequest)
        return true
    }

    override fun delete(userId: String, id: String): Boolean {
        storeDockingPlatformDao.delete(dslContext, id)
        return true
    }

    override fun update(
        userId: String,
        id: String,
        storeDockingPlatformRequest: StoreDockingPlatformRequest
    ): Boolean {
        val storeDockingPlatform = storeDockingPlatformDao.getStoreDockingPlatform(dslContext, id)
        val platformCode = storeDockingPlatformRequest.platformCode
        // 判断平台代码是否存在
        val codeCount = storeDockingPlatformDao.countByCode(dslContext, platformCode)
        if (codeCount > 0) {
            if (null != storeDockingPlatform && platformCode != storeDockingPlatform.platformCode) {
                // 抛出错误提示
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                    params = arrayOf(platformCode)
                )
            }
        }
        val platformName = storeDockingPlatformRequest.platformName
        // 判断平台名称是否存在
        val nameCount = storeDockingPlatformDao.countByName(dslContext, platformName)
        if (nameCount > 0) {
            if (null != storeDockingPlatform && platformName != storeDockingPlatform.platformName) {
                // 抛出错误提示
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                    params = arrayOf(platformName)
                )
            }
        }

        val errorCodePrefix = storeDockingPlatformRequest.errorCodePrefix
        // 判断平台所属错误码前缀是否存在
        val errorCodePrefixCount = storeDockingPlatformDao.countByErrorCodePrefix(dslContext, errorCodePrefix)
        if (errorCodePrefixCount > 0) {
            if (null != storeDockingPlatform && errorCodePrefix != storeDockingPlatform.errorCodePrefix) {
                // 抛出错误提示
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                    params = arrayOf("errorCodePrefix:$errorCodePrefix")
                )
            }
        }
        storeDockingPlatformDao.update(
            dslContext = dslContext,
            id = id,
            userId = userId,
            storeDockingPlatformRequest = storeDockingPlatformRequest
        )
        return true
    }

    override fun getStoreDockingPlatforms(
        userId: String,
        platformName: String?,
        id: String?,
        page: Int,
        pageSize: Int
    ): Page<StoreDockingPlatformInfo>? {
        val storeDockingPlatformInfos = storeDockingPlatformDao.getStoreDockingPlatforms(
            dslContext = dslContext,
            platformName = platformName,
            id = id,
            page = page,
            pageSize = pageSize
        )
        val storeDockingPlatformCount = storeDockingPlatformDao.getStoreDockingPlatformCount(
            dslContext = dslContext,
            platformName = platformName,
            id = id
        )
        val totalPages = PageUtil.calTotalPage(pageSize, storeDockingPlatformCount)
        return Page(
            count = storeDockingPlatformCount,
            page = page,
            pageSize = pageSize,
            totalPages = totalPages,
            records = storeDockingPlatformInfos ?: emptyList()
        )
    }

    override fun isPlatformCodeRegistered(platformCode: String): Boolean {
        return storeDockingPlatformDao.isPlatformCodeRegistered(dslContext, platformCode)
    }
}
