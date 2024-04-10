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

package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.remotedev.dao.RemoteDevCvmDao
import com.tencent.devops.remotedev.pojo.op.RemotedevCvmData
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RemoteDevCvmService @Autowired constructor(
    private val dslContext: DSLContext,
    private val remoteDevCvmDao: RemoteDevCvmDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteDevCvmService::class.java)
    }

    // 新增模板
    fun batchAddCvm(userId: String, cvmList: List<RemotedevCvmData>): Boolean {
        // 模板信息写入DB
        remoteDevCvmDao.batchAddCvm(
            cvmList = cvmList,
            dslContext = dslContext
        )
        return true
    }

    // 修改模板
    fun updateCvm(
        id: Long,
        remotedevCvmData: RemotedevCvmData
    ): Boolean {
        // 更新模板信息
        remoteDevCvmDao.updateCvm(
            id = id,
            data = remotedevCvmData,
            dslContext = dslContext
        )

        return true
    }

    // 删除模板
    fun deleteCvm(
        id: Long
    ): Boolean {
        // 删除模板信息
        remoteDevCvmDao.deleteCvm(
            id = id,
            dslContext = dslContext
        )

        return true
    }

    fun getAllRemotedevCvm(
        projectId: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): List<RemotedevCvmData> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 6666
        val result = mutableListOf<RemotedevCvmData>()
        remoteDevCvmDao.queryCvmList(
            dslContext = dslContext,
            projectId = projectId,
            zone = null,
            ips = null,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        ).forEach {
            result.add(
                RemotedevCvmData(
                    id = it.id.toInt(),
                    projectId = it.projectId,
                    ip = it.ip,
                    zone = it.zone,
                    availableRegion = it.availableRegion,
                    cpu = it.cpu,
                    memory = it.memory,
                    subnet = it.subnet
                )
            )
        }
        return result
    }

    // 获取工作空间模板
    fun getRemotedevCvmList(
        projectId: String? = null,
        zone: String? = null,
        ips: List<String>? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Page<RemotedevCvmData> {
        logger.info("Start to getRemotedevCvmList")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 6666
        val count = remoteDevCvmDao.countAllCvmList(
            dslContext = dslContext,
            projectId = projectId,
            zone = zone,
            ips = ips
        )
        val result = mutableListOf<RemotedevCvmData>()
        remoteDevCvmDao.queryCvmList(
            dslContext = dslContext,
            projectId = projectId,
            zone = zone,
            ips = ips,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        ).forEach {
            result.add(
                RemotedevCvmData(
                    id = it.id.toInt(),
                    projectId = it.projectId,
                    ip = it.ip,
                    zone = it.zone,
                    availableRegion = it.availableRegion,
                    cpu = it.cpu,
                    memory = it.memory,
                    subnet = it.subnet
                )
            )
        }
        return Page(
            page = pageNotNull, pageSize = pageSizeNotNull, count = count,
            records = result
        )
    }
}
