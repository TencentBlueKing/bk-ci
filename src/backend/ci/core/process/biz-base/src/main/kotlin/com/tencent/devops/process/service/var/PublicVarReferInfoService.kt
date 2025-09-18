/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.service.`var`

import com.tencent.devops.common.client.Client
import com.tencent.devops.process.dao.`var`.PublicVarDao
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReferDTO
import com.tencent.devops.process.pojo.`var`.po.PipelinePublicVarReferPO
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PublicVarReferInfoService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val publicVarDao: PublicVarDao,
    private val publicVarReferInfoDao: PublicVarReferInfoDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarReferInfoService::class.java)
    }

    fun addPublicVarRefer(
        userId: String,
        projectId: String,
        publicVarGroupReferInfo: PublicVarGroupReferDTO
    ): Boolean {
        val referId = publicVarGroupReferInfo.referId
        val referType = publicVarGroupReferInfo.referType
        val publicVarGroupRefs = publicVarGroupReferInfo.publicVarGroupRefs
        val referVersionName = publicVarGroupReferInfo.referVersionName ?: ""

        try {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val publicVarReferPOs = mutableListOf<PipelinePublicVarReferPO>()
                publicVarGroupRefs.forEach { ref ->
                    val groupName = ref.key
                    val version = ref.value ?: return@forEach

                    val publicVars = publicVarDao.queryVarNamesByGroupName(
                        dslContext = context,
                        projectId = projectId,
                        groupName = groupName,
                        version = version
                    )

                    publicVars.forEach { varName ->
                        publicVarReferPOs.add(
                            PipelinePublicVarReferPO(
                                id = client.get(ServiceAllocIdResource::class)
                                    .generateSegmentId("T_PIPELINE_PUBLIC_VAR_REFER_INFO").data ?: 0,
                                projectId = projectId,
                                groupName = groupName,
                                varName = varName,
                                version = version,
                                referId = referId,
                                referType = referType,
                                referVersionName = referVersionName,
                                modifier = userId,
                                updateTime = LocalDateTime.now(),
                                creator = userId,
                                createTime = LocalDateTime.now()
                            )
                        )
                    }
                }
                // 先清理旧的变量引用
                publicVarReferInfoDao.deleteByReferId(
                    dslContext = context,
                    projectId = projectId,
                    referId = referId,
                    referType = referType,
                    referVersionName = referVersionName
                )
                publicVarReferInfoDao.batchSave(context, publicVarReferPOs)
            }
        } catch (t: Throwable) {
            logger.warn("Failed to add variable refer for $referId", t)
            throw t
        }
        return true
    }

}