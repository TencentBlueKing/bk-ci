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

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.model.process.tables.records.TPipelineFavorRecord
import com.tencent.devops.model.process.tables.records.TPipelineLabelRecord
import com.tencent.devops.process.dao.PipelineFavorDao
import com.tencent.devops.process.dao.label.PipelineGroupDao
import com.tencent.devops.process.dao.label.PipelineLabelDao
import com.tencent.devops.process.dao.label.PipelineLabelPipelineDao
import com.tencent.devops.process.dao.label.PipelineViewLabelDao
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.process.pojo.classify.PipelineGroupCreate
import com.tencent.devops.process.pojo.classify.PipelineGroupUpdate
import com.tencent.devops.process.pojo.classify.PipelineGroupWithLabels
import com.tencent.devops.process.pojo.classify.PipelineLabel
import com.tencent.devops.process.pojo.classify.PipelineLabelCreate
import com.tencent.devops.process.pojo.classify.PipelineLabelUpdate
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

@Service
class PipelineGroupService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineGroupDao: PipelineGroupDao,
    private val pipelineLabelDao: PipelineLabelDao,
    private val pipelineViewLabelDao: PipelineViewLabelDao,
    private val pipelineFavorDao: PipelineFavorDao,
    private val pipelineLabelPipelineDao: PipelineLabelPipelineDao
) {

    fun getGroups(userId: String, projectId: String): List<PipelineGroup> {
        val groups = pipelineGroupDao.list(dslContext, projectId)

        val groupIds = groups.map {
            it.id
        }.toSet()

        val labelsByGroup = HashMap<Long, MutableList<TPipelineLabelRecord>>()

        val labels = pipelineLabelDao.getByGroupIds(dslContext, groupIds)

        labels.forEach {
            val list = if (labelsByGroup.containsKey(it.groupId)) {
                labelsByGroup[it.groupId]!!
            } else {
                val tmp = ArrayList<TPipelineLabelRecord>()
                labelsByGroup[it.groupId] = tmp
                tmp
            }
            list.add(it)
        }

        return groups.map {
            PipelineGroup(
                encode(it.id),
                it.projectId,
                it.name,
                it.createTime.timestamp(),
                it.updateTime.timestamp(),
                it.createUser,
                it.updateUser,
                labelsByGroup[it.id]?.map { label ->
                    PipelineLabel(
                        encode(label.id),
                        encode(label.groupId),
                        label.name,
                        label.createTime.timestamp(),
                        label.updateTime.timestamp(),
                        label.createUser,
                        label.updateUser
                    )
                } ?: emptyList()
            )
        }.sortedBy { it.createTime }
    }

    fun getGroups(userId: String, projectId: String, pipelineId: String): List<PipelineGroupWithLabels> {
        val labelRecords = pipelineLabelPipelineDao.listLabels(dslContext, pipelineId)
        val labelIds = labelRecords.map { it.labelId }.toSet()
        val groups = getLabelsGroupByGroup(projectId, labelIds)
        return groups.map {
            PipelineGroupWithLabels(
                it.id,
                it.labels.map { label -> label.id }
            )
        }
    }

    fun addGroup(userId: String, pipelineGroup: PipelineGroupCreate): Boolean {
        try {
            pipelineGroupDao.create(dslContext, pipelineGroup.projectId, pipelineGroup.name, userId)
        } catch (t: DuplicateKeyException) {
            logger.warn("Fail to create the group $pipelineGroup by userId $userId")
            throw OperationException("The group is already exist")
        }
        return true
    }

    fun updateGroup(userId: String, pipelineGroup: PipelineGroupUpdate): Boolean {
        try {
            return pipelineGroupDao.update(dslContext, decode(pipelineGroup.id), pipelineGroup.name, userId)
        } catch (t: DuplicateKeyException) {
            logger.warn("Fail to update the group $pipelineGroup by userId $userId")
            throw OperationException("The group is already exist")
        }
    }

    fun deleteGroup(userId: String, groupId: String): Boolean {
        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val id = decode(groupId)
            val result = pipelineGroupDao.delete(context, id, userId)
            pipelineLabelDao.deleteByGroupId(context, id, userId)
            result
        }
    }

    fun addLabel(userId: String, pipelineLabel: PipelineLabelCreate): Boolean {
        try {
            pipelineLabelDao.create(
                dslContext,
                decode(pipelineLabel.groupId),
                pipelineLabel.name,
                userId
            )
        } catch (t: DuplicateKeyException) {
            logger.warn("Fail to add the label $pipelineLabel by userId $userId")
            throw OperationException("The label is already exist")
        }

        return true
    }

    fun deleteLabel(userId: String, labelId: String): Boolean {
        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val id = decode(labelId)
            val result = pipelineLabelDao.deleteById(context, id, userId)
            pipelineViewLabelDao.detachLabel(context, id, userId)
            pipelineLabelPipelineDao.deleteByLabel(context, id, userId)
            result
        }
    }

    fun updateLabel(userId: String, pipelineLabel: PipelineLabelUpdate): Boolean {
        try {
            return pipelineLabelDao.update(dslContext, decode(pipelineLabel.id), pipelineLabel.name, userId)
        } catch (t: DuplicateKeyException) {
            logger.warn("Fail to update the label $pipelineLabel by userId $userId")
            throw OperationException("The label is already exist")
        }
    }

    fun deletePipelineLabel(userId: String, pipelineId: String) {
        pipelineLabelPipelineDao.deleteByPipeline(dslContext, pipelineId, userId)
    }

    fun addPipelineLabel(userId: String, pipelineId: String, labelIds: List<String>) {
        if (labelIds.isEmpty()) {
            return
        }
        try {
            pipelineLabelPipelineDao.batchCreate(dslContext, pipelineId, labelIds.map { decode(it) }.toSet(), userId)
        } catch (t: DuplicateKeyException) {
            logger.warn("Fail to add the pipeline $pipelineId label $labelIds by userId $userId")
            throw OperationException("The label is already exist")
        }
    }

    fun updatePipelineLabel(userId: String, pipelineId: String, labelIds: List<String>) {
        val ids = labelIds.map { decode(it) }.toSet()

        try {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                pipelineLabelPipelineDao.deleteByPipeline(context, pipelineId, userId)
                pipelineLabelPipelineDao.batchCreate(dslContext, pipelineId, ids, userId)
            }
        } catch (t: DuplicateKeyException) {
            logger.warn("Fail to update the pipeline $pipelineId label $labelIds by userId $userId")
            throw OperationException("The label is already exist")
        }
    }

    /*
    fun getViewPipelines(labels: String): List<String>? {
        val labelIds = labels.split(",").filter { it.isNotBlank() }.map { decode(it) }.toSet()
        if (labelIds.isEmpty()) {
            return null
        }
        val pipeline =  pipelineLabelPipelineDao.listPipelines(dslContext, labelIds)

        val groups = HashMap<String/*pipelineId*/, MutableList<Long>/*LabelIds*/>()
        pipeline.forEach {
            if (groups.containsKey(it.pipelineId)) {
                groups[it.pipelineId]!!.add(it.labelId)
            } else {
                groups[it.pipelineId] = mutableListOf(it.labelId)
            }
        }

        val result = mutableListOf<String>()
        groups.forEach { pipelineId, pipelineLabels ->
            if (pipelineLabels.size != labelIds.size) {
                return@forEach
            }
            result.add(pipelineId)
        }
        return result
    }
    */

    fun getViewLabelToPipelinesMap(labels: List<String>): Map<String, List<String>> {
        val labelIds = labels.map { decode(it) }.toSet()
        if (labelIds.isEmpty()) {
            return emptyMap()
        }

        val pipelines = pipelineLabelPipelineDao.listPipelines(dslContext, labelIds)

        val labelToPipelineMap = mutableMapOf<String, MutableList<String>>()
        pipelines.forEach {
            if (labelToPipelineMap.containsKey(encode(it.labelId))) {
                labelToPipelineMap[encode(it.labelId)]!!.add(it.pipelineId)
            } else {
                labelToPipelineMap[encode(it.labelId)] = mutableListOf(it.pipelineId)
            }
        }
        return labelToPipelineMap
    }

    fun getGroupToLabelsMap(labels: List<String>): Map<String, List<String>> {
        val labelIds = labels.map { decode(it) }.toSet()
        val labelRecords = pipelineLabelDao.getByIds(dslContext, labelIds)

        val groupToLabelsMap = mutableMapOf<String, MutableList<String>>()
        labelRecords.forEach {
            if (groupToLabelsMap.containsKey(encode(it.groupId))) {
                groupToLabelsMap[encode(it.groupId)]!!.add(encode(it.id))
            } else {
                groupToLabelsMap[encode(it.groupId)] = mutableListOf(encode(it.id))
            }
        }

        return groupToLabelsMap
    }

    // 收藏流水线
    fun favorPipeline(userId: String, projectId: String, pipelineId: String, favor: Boolean): Boolean {
        if (favor) {
            pipelineFavorDao.create(dslContext, userId, projectId, pipelineId)
        } else {
            pipelineFavorDao.delete(dslContext, userId, pipelineId)
        }
        return true
    }

    // 删除流水线后联带删除整个流水线相关的收藏
    fun deleteAllUserFavorByPipeline(userId: String, pipelineId: String): Int {
        val count = pipelineFavorDao.deleteAllUserFavorByPipeline(dslContext, pipelineId)
        logger.info("Delete pipeline-favor of pipeline $pipelineId by user $userId. count=$count")
        return count
    }

    fun getFavorPipelines(userId: String, projectId: String): List<String> {
        return pipelineFavorDao.list(dslContext, userId, projectId).map { it.pipelineId }
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

    private fun decode(id: String) =
        HashUtil.decodeIdToLong(id)

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineGroupService::class.java)
    }

    fun getFavorPipelinesPage(userId: String, page: Int? = null, pageSize: Int? = null): Result<TPipelineFavorRecord>? {
        return pipelineFavorDao.listByUserId(dslContext, userId)
    }
}
