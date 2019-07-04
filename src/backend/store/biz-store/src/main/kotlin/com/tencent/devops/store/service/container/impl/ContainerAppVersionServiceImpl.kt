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

package com.tencent.devops.store.service.container.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.dao.container.ContainerAppsVersionDao
import com.tencent.devops.store.pojo.app.ContainerAppVersion
import com.tencent.devops.store.pojo.app.ContainerAppVersionCreate
import com.tencent.devops.store.service.container.ContainerAppVersionService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ContainerAppVersionServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val containerAppsVersionDao: ContainerAppsVersionDao
) : ContainerAppVersionService {
    private val logger = LoggerFactory.getLogger(ContainerAppVersionServiceImpl::class.java)

    /**
     * 根据编译环境id查找编译环境版本信息
     */
    override fun listByAppId(appId: Int): Result<List<ContainerAppVersion>> {
        val containerAppsVersionList =
            containerAppsVersionDao.listByAppId(dslContext, appId).map { containerAppsVersionDao.convert(it) }
        return Result(containerAppsVersionList)
    }

    /**
     * 根据id查找编译环境版本信息
     */
    override fun getContainerAppVersion(id: Int): Result<ContainerAppVersion?> {
        val containerAppVersionRecord = containerAppsVersionDao.getById(dslContext, id)
        logger.info("the containerAppVersionRecord is :$containerAppVersionRecord")
        return Result(
            if (containerAppVersionRecord == null) {
                null
            } else {
                containerAppsVersionDao.convert(containerAppVersionRecord)
            }
        )
    }

    /**
     * 保存编译环境版本信息
     */
    override fun saveContainerAppVersion(containerAppVersionRequest: ContainerAppVersionCreate): Result<Boolean> {
        logger.info("the save containerAppVersionRequest is:$containerAppVersionRequest")
        containerAppsVersionDao.add(dslContext, containerAppVersionRequest.appId, containerAppVersionRequest.version)
        return Result(true)
    }

    /**
     * 更新编译环境版本信息
     */
    override fun updateContainerAppVersion(
        id: Int,
        containerAppVersionRequest: ContainerAppVersionCreate
    ): Result<Boolean> {
        logger.info("the update id is :$id,the update containerAppVersionRequest is:$containerAppVersionRequest")
        containerAppsVersionDao.update(
            dslContext,
            id,
            containerAppVersionRequest.appId,
            containerAppVersionRequest.version
        )
        return Result(true)
    }

    /**
     * 删除编译环境版本信息
     */
    override fun deleteContainerAppVersion(id: Int): Result<Boolean> {
        logger.info("the delete id is :$id")
        containerAppsVersionDao.delete(dslContext, id)
        return Result(true)
    }
}
