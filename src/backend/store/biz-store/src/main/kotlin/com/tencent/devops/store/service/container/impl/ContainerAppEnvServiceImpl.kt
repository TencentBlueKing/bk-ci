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
import com.tencent.devops.store.dao.container.ContainerAppsEnvDao
import com.tencent.devops.store.pojo.app.ContainerAppEnv
import com.tencent.devops.store.pojo.app.ContainerAppEnvCreate
import com.tencent.devops.store.service.container.ContainerAppEnvService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 编译环境变量业务逻辑类
 *
 * since: 2018-12-20
 */
@Service
class ContainerAppEnvServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val containerAppsEnvDao: ContainerAppsEnvDao
) : ContainerAppEnvService {

    private val logger = LoggerFactory.getLogger(ContainerAppEnvServiceImpl::class.java)

    /**
     * 根据编译环境ID查找该编译环境下的环境变量
     */
    override fun listByAppId(appId: Int): Result<List<ContainerAppEnv>> {
        val containerAppEnvList =
            containerAppsEnvDao.listByAppId(dslContext, appId).map { containerAppsEnvDao.convert(it) }
        return Result(containerAppEnvList)
    }

    /**
     * 根据id查找编译环境信息
     */
    override fun getContainerAppEnv(id: Int): Result<ContainerAppEnv?> {
        val containerAppEnvRecord = containerAppsEnvDao.getById(dslContext, id)
        logger.info("the containerAppEnvRecord is :{}", containerAppEnvRecord)
        return Result(
            if (containerAppEnvRecord == null) {
                null
            } else {
                containerAppsEnvDao.convert(containerAppEnvRecord)
            }
        )
    }

    /**
     * 保存编译环境变量信息
     */
    override fun saveContainerAppEnv(containerAppEnvRequest: ContainerAppEnvCreate): Result<Boolean> {
        logger.info("the save containerAppEnvRequest is:{}", containerAppEnvRequest)
        containerAppsEnvDao.add(
            dslContext,
            containerAppEnvRequest.appId,
            containerAppEnvRequest.name,
            containerAppEnvRequest.path,
            containerAppEnvRequest.description
        )
        return Result(true)
    }

    /**
     * 更新编译环境信息
     */
    override fun updateContainerAppEnv(id: Int, containerAppEnvRequest: ContainerAppEnvCreate): Result<Boolean> {
        logger.info("the update id is :{},the update containerAppEnvRequest is:{}", id, containerAppEnvRequest)
        containerAppsEnvDao.update(
            dslContext,
            id,
            containerAppEnvRequest.appId,
            containerAppEnvRequest.name,
            containerAppEnvRequest.path,
            containerAppEnvRequest.description
        )
        return Result(true)
    }

    /**
     * 删除编译环境变量信息
     */
    override fun deleteContainerAppEnv(id: Int): Result<Boolean> {
        logger.info("the delete id is :{}", id)
        containerAppsEnvDao.delete(dslContext, id)
        return Result(true)
    }
}
