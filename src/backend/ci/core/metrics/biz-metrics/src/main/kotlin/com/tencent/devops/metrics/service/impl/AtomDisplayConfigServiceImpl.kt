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

package com.tencent.devops.metrics.service.impl

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.client.Client
import com.tencent.devops.metrics.dao.AtomDisplayConfigDao
import com.tencent.devops.metrics.pojo.`do`.AtomBaseInfoDO
import com.tencent.devops.metrics.service.AtomDisplayConfigManageService
import com.tencent.devops.metrics.pojo.dto.AtomDisplayConfigDTO
import com.tencent.devops.metrics.pojo.po.AtomDisplayConfigPO
import com.tencent.devops.metrics.pojo.vo.AtomDisplayConfigVO
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AtomDisplayConfigServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val atomDisplayConfigDao: AtomDisplayConfigDao
) : AtomDisplayConfigManageService {

    override fun addAtomDisplayConfig(atomDisplayConfigDTO: AtomDisplayConfigDTO): Boolean {
        val atomBaseInfos = atomDisplayConfigDTO.atomBaseInfos
        val atomDisplayConfigPOS = mutableListOf<AtomDisplayConfigPO>()
        atomBaseInfos.forEach { atomBaseInfo ->
            val currentTime = LocalDateTime.now()
            atomDisplayConfigPOS.add(
                AtomDisplayConfigPO(
                    id = client.get(ServiceAllocIdResource::class)
                        .generateSegmentId("ATOM_DISPLAY_CONFIG").data ?: 0,
                    projectId = atomDisplayConfigDTO.projectId,
                    userId = atomDisplayConfigDTO.userId,
                    atomCode = atomBaseInfo.atomCode,
                    atomName = atomBaseInfo.atomName,
                    createTime = currentTime,
                    updateTime = currentTime
                )
            )
        }
        atomDisplayConfigDao.batchAddAtomDisplayConfig(dslContext, atomDisplayConfigPOS)
        return true
    }

    override fun deleteAtomDisplayConfig(projectId: String, userId: String, atomCodes: List<AtomBaseInfoDO>): Boolean {
        logger.info("deleteAtomDisplayConfig atomCodes: $atomCodes")
        return atomDisplayConfigDao.batchDeleteAtomDisplayConfig(
            dslContext,
            projectId,
            userId,
            atomCodes.map { it.atomCode }
        ) > 0
    }

    override fun getAtomDisplayConfig(projectId: String, userId: String, keyword: String?): AtomDisplayConfigVO {
        return AtomDisplayConfigVO(
            atomDisplayConfigDao.getAtomDisplayConfig(
                dslContext,
                projectId,
                keyword
            )
        )
    }

    override fun getOptionalAtomDisplayConfig(
        projectId: String,
        userId: String,
        keyword: String?,
        page: Int,
        pageSize: Int
    ): Page<AtomBaseInfoDO> {
        val atomCodes = atomDisplayConfigDao.getAtomDisplayConfig(
            dslContext,
            projectId,
            keyword
        ).map { it.atomCode }
        return Page(
            page = page,
            pageSize = pageSize,
            count = atomDisplayConfigDao.getOptionalAtomDisplayConfigCount(
                dslContext = dslContext,
                projectId = projectId,
                atomCodes = atomCodes,
                keyword = keyword
            ),
            records = atomDisplayConfigDao.getOptionalAtomDisplayConfig(
                dslContext = dslContext,
                projectId = projectId,
                atomCodes = atomCodes,
                keyword = keyword,
                page = page ?: 1,
                pageSize = pageSize ?: 10
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AtomDisplayConfigServiceImpl::class.java)
    }
}
