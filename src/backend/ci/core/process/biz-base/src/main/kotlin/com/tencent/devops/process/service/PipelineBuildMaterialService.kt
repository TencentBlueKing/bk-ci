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

package com.tencent.devops.process.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineBuildMaterialService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineBuildDao: PipelineBuildDao
) {
    private val logger = LoggerFactory.getLogger(PipelineBuildMaterialService::class.java)

    fun saveBuildMaterial(
        buildId: String,
        projectId: String,
        pipelineBuildMaterials: List<PipelineBuildMaterial>
    ): Int {
        var newPipelineBuildMaterials = pipelineBuildMaterials
        val pipelineBuildHistoryRecord = pipelineBuildDao.getBuildInfo(dslContext, projectId, buildId)
        // 如果找不到构建历史或重试时，不做原材料写入
        if (pipelineBuildHistoryRecord == null ||
            pipelineBuildHistoryRecord.executeCount?.let { it > 1 } == true
        ) {
            return 0
        }
        val material = pipelineBuildHistoryRecord.material
        if (StringUtils.isNoneBlank(material)) {
            val originPipelineBuildMaterials =
                JsonUtil.to(material, object : TypeReference<List<PipelineBuildMaterial>>() {})
            newPipelineBuildMaterials = newPipelineBuildMaterials.plus(originPipelineBuildMaterials)
        }

        val materials = JsonUtil.toJson(newPipelineBuildMaterials, formatted = false)
        logger.info("BuildId: $buildId save material size: ${newPipelineBuildMaterials.size}")
        pipelineBuildDao.updateBuildMaterial(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            material = materials
        )
        return pipelineBuildMaterials.size
    }
}
