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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.pojo.PipelineBuildMaterial
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

    @SuppressWarnings("CyclomaticComplexMethod", "ComplexMethod")
    fun saveBuildMaterial(
        buildId: String,
        projectId: String,
        pipelineBuildMaterials: List<PipelineBuildMaterial>,
        taskId: String?
    ): Int {
        val materialList = mutableListOf<PipelineBuildMaterial>()
        val pipelineBuildHistoryRecord = pipelineBuildDao.getBuildInfo(dslContext, projectId, buildId) ?: return 0
        // 如果找不到构建历史或重试时，不做原材料写入
        logger.info("save build material|buildId=$buildId|taskId=$taskId|${pipelineBuildMaterials.size}")
        val material = pipelineBuildHistoryRecord.material
        // 重试场景下，只要有空taskId，就直接返回
        val containsEmptyTaskId = material?.find { it.taskId.isNullOrBlank() } != null || taskId.isNullOrBlank()
        if (pipelineBuildHistoryRecord.executeCount?.let { it > 1 } == true && containsEmptyTaskId) {
            logger.info("skip save build material")
            return 0
        }
        val existTaskIds = material?.mapNotNull { it.taskId } ?: listOf()
        if (!material.isNullOrEmpty()) {
            materialList.addAll(material)
            // 存在空TaskId，直接添加
            // 不存在空TaskId，旧数据不存在当前TaskId上报的源材料时则添加
            if ((!containsEmptyTaskId && !existTaskIds.contains(taskId)) || containsEmptyTaskId) {
                materialList.addAll(pipelineBuildMaterials.map { it.copy(taskId = taskId) })
            }
        } else {
            materialList.addAll(pipelineBuildMaterials.map { it.copy(taskId = taskId) })
        }
        // 按顺序保存
        val materials = JsonUtil.toJson(
            bean = materialList.let { list ->
                list.sortedWith { o1, o2 ->
                    val (mainRepo1, createTime1) = (o1.mainRepo ?: false) to (o1.createTime ?: 0)
                    val (mainRepo2, createTime2) = (o2.mainRepo ?: false) to (o2.createTime ?: 0)
                    when {
                        mainRepo1 == mainRepo2 -> if (mainRepo1) {
                            createTime2.compareTo(createTime1)
                        } else {
                            (o1.aliasName ?: "").compareTo(o2.aliasName ?: "")
                        }

                        else -> mainRepo2.compareTo(mainRepo1)
                    }
                }
            },
            formatted = false
        )
        pipelineBuildDao.updateBuildMaterial(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            material = materials
        )
        return pipelineBuildMaterials.size
    }
}
