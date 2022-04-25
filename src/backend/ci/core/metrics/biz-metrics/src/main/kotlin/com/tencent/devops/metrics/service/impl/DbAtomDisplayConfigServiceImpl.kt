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

import com.tencent.devops.common.client.Client
import com.tencent.devops.metrics.dao.AtomDisplayConfigDao
import com.tencent.devops.metrics.service.AtomDisplayConfigManageService
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import com.tencent.devops.metrics.pojo.dto.SaveAtomDisplayConfigDTO
import com.tencent.devops.metrics.pojo.po.SaveAtomDisplayConfigPO
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class DbAtomDisplayConfigServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val atomDisplayConfigDao: AtomDisplayConfigDao,
    private val client: Client
) : AtomDisplayConfigManageService {

    override fun saveAtomDisplayConfig(saveAtomDisplayConfigDTO: SaveAtomDisplayConfigDTO): Boolean {
        val atomBaseInfos = saveAtomDisplayConfigDTO.atomBaseInfos
        val saveAtomDisplayConfigPOs = mutableListOf<SaveAtomDisplayConfigPO>()
        atomBaseInfos.forEach { atomBaseInfo ->
            val currentTime = LocalDateTime.now()
            val id = client.get(ServiceAllocIdResource::class).generateSegmentId("ATOM_DISPLAY_CONFIG").data ?: 0
            saveAtomDisplayConfigPOs.add(
                SaveAtomDisplayConfigPO(
                    id = id,
                    projectId = saveAtomDisplayConfigDTO.projectId,
                    userId = saveAtomDisplayConfigDTO.userId,
                    atomCode = atomBaseInfo.atomCode,
                    atomName = atomBaseInfo.atomName,
                    createTime = currentTime,
                    updateTime = currentTime
                )
            )
        }
        atomDisplayConfigDao.batchSaveAtomDisplayConfig(dslContext, saveAtomDisplayConfigPOs)
        return true
    }
}
