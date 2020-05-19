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

package com.tencent.devops.process.service.label

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.model.process.tables.records.TPipelineLabelRecord
import com.tencent.devops.process.dao.label.PipelineGroupDao
import com.tencent.devops.process.dao.label.PipelineLabelDao
import com.tencent.devops.process.dao.label.PipelineLabelPipelineVersionDao
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.process.pojo.classify.PipelineGroupWithLabels
import com.tencent.devops.process.pojo.classify.PipelineLabel
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineGroupVersionService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineGroupDao: PipelineGroupDao,
    private val pipelineLabelDao: PipelineLabelDao,
    private val pipelineLabelPipelineVersionDao: PipelineLabelPipelineVersionDao
) {

    fun getGroups(userId: String, projectId: String, pipelineId: String): List<PipelineGroupWithLabels> {
        val labelRecords = pipelineLabelPipelineVersionDao.listLabels(dslContext, pipelineId)
        val labelIds = labelRecords.map { it.labelId }.toSet()
        val groups = getLabelsGroupByGroup(projectId, labelIds)
        return groups.map {
            PipelineGroupWithLabels(
                it.id,
                it.labels.map { label -> label.id }
            )
        }
    }

    private fun getLabelsGroupByGroup(projectId: String, labelIds: Set<Long>): List<PipelineGroup> {
        val labels = pipelineLabelDao.getByIds(dslContext, labelIds)
        val groups = HashMap<Long, MutableList<TPipelineLabelRecord>>()
        labels.forEach {
            val l = if (groups.containsKey(it.groupId)) {
                groups[it.groupId]!!
            } else {
                val tmp = ArrayList<TPipelineLabelRecord>()
                groups[it.groupId] = tmp
                tmp
            }
            l.add(it)
        }

        val groupIds = groups.keys.map { it }.toSet()

        val groupRecords = pipelineGroupDao.listByIds(dslContext, projectId, groupIds).map { it.id to it }.toMap()

        val result = ArrayList<PipelineGroup>()

        groups.forEach { (groupId, label) ->
            val g = groupRecords[groupId]
            if (g == null) {
                logger.info("The group $groupId of project $projectId is not exist")
                return@forEach
            }
            result.add(
                PipelineGroup(
                    encode(g.id),
                    g.projectId,
                    g.name,
                    g.createTime.timestamp(),
                    g.updateTime.timestamp(),
                    g.updateUser,
                    g.updateUser,
                    label.map {
                        PipelineLabel(
                            encode(it.id),
                            encode(it.groupId),
                            it.name,
                            it.createTime.timestamp(),
                            it.updateTime.timestamp(),
                            it.createUser,
                            it.updateUser
                        )
                    }
                )
            )
        }

        return result
    }

    private fun encode(id: Long) =
        HashUtil.encodeLongId(id)

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineGroupVersionService::class.java)
    }
}
