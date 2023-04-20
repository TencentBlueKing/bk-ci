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

package com.tencent.devops.process.service.label

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.enums.PipelineLabelChangeTypeEnum
import com.tencent.devops.common.event.pojo.measure.LabelChangeMetricsBroadCastEvent
import com.tencent.devops.common.event.pojo.measure.PipelineLabelRelateInfo
import com.tencent.devops.model.process.tables.records.TPipelineFavorRecord
import com.tencent.devops.model.process.tables.records.TPipelineGroupRecord
import com.tencent.devops.model.process.tables.records.TPipelineLabelRecord
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_GROUP_COUNT_EXCEEDS_LIMIT
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_LABEL_COUNT_EXCEEDS_LIMIT
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_LABEL_NAME_TOO_LONG
import com.tencent.devops.process.dao.PipelineFavorDao
import com.tencent.devops.process.dao.label.PipelineGroupDao
import com.tencent.devops.process.dao.label.PipelineLabelDao
import com.tencent.devops.process.dao.label.PipelineLabelPipelineDao
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.process.pojo.classify.PipelineGroupCreate
import com.tencent.devops.process.pojo.classify.PipelineGroupLabels
import com.tencent.devops.process.pojo.classify.PipelineGroupUpdate
import com.tencent.devops.process.pojo.classify.PipelineGroupWithLabels
import com.tencent.devops.process.pojo.classify.PipelineLabel
import com.tencent.devops.process.pojo.classify.PipelineLabelCreate
import com.tencent.devops.process.pojo.classify.PipelineLabelUpdate
import com.tencent.devops.process.service.measure.MeasureEventDispatcher
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessException
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.sql.SQLIntegrityConstraintViolationException
import java.time.LocalDateTime

@Suppress("ALL")
@Service
class PipelineGroupService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineGroupDao: PipelineGroupDao,
    private val pipelineLabelDao: PipelineLabelDao,
    private val pipelineFavorDao: PipelineFavorDao,
    private val pipelineLabelPipelineDao: PipelineLabelPipelineDao,
    private val client: Client,
    private val measureEventDispatcher: MeasureEventDispatcher
) {

    fun getGroups(userId: String, projectId: String): List<PipelineGroup> {
        val groups = pipelineGroupDao.list(dslContext, projectId)

        val groupIds = groups.map {
            it.id
        }.toSet()

        val labelsByGroup = HashMap<Long, MutableList<TPipelineLabelRecord>>()

        val labels = pipelineLabelDao.getByGroupIds(dslContext, projectId, groupIds)

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
                id = encode(it.id),
                projectId = it.projectId,
                name = it.name,
                createTime = it.createTime.timestamp(),
                updateTime = it.updateTime.timestamp(),
                createUser = it.createUser,
                updateUser = it.updateUser,
                labels = labelsByGroup[it.id]?.map { label ->
                    PipelineLabel(
                        id = encode(label.id),
                        groupId = encode(label.groupId),
                        name = label.name,
                        createTime = label.createTime.timestamp(),
                        uptimeTime = label.updateTime.timestamp(),
                        createUser = label.createUser,
                        updateUser = label.updateUser
                    )
                }?.sortedBy { label -> label.createTime } ?: emptyList()
            )
        }.sortedBy { it.createTime }
    }

    fun getGroups(userId: String, projectId: String, pipelineId: String): List<PipelineGroupWithLabels> {
        val labelRecords = pipelineLabelPipelineDao.listLabels(dslContext, projectId, pipelineId)
        val labelIds = labelRecords.map { it.labelId }.toSet()
        val groups = getLabelsGroupByGroup(projectId, labelIds)
        return groups.map {
            PipelineGroupWithLabels(
                id = it.id,
                labels = it.labels.map { label -> label.id }
            )
        }
    }

    fun addGroup(userId: String, pipelineGroup: PipelineGroupCreate): Boolean {
        try {
            val groupCount = pipelineGroupDao.count(dslContext = dslContext, projectId = pipelineGroup.projectId)
            if (groupCount >= MAX_GROUP_UNDER_PROJECT) {
                throw ErrorCodeException(
                    errorCode = ERROR_GROUP_COUNT_EXCEEDS_LIMIT,
                    defaultMessage = "At most $MAX_GROUP_UNDER_PROJECT label groups under a project"
                )
            }
            val id = client.get(ServiceAllocIdResource::class).generateSegmentId("PIPELINE_GROUP").data
            pipelineGroupDao.create(
                dslContext = dslContext,
                projectId = pipelineGroup.projectId,
                name = pipelineGroup.name,
                userId = userId,
                id = id
            )
        } catch (t: DataAccessException) {
            if (t.cause is SQLIntegrityConstraintViolationException) {
                logger.warn("Fail to create the group $pipelineGroup by userId $userId")
                throw OperationException("The group is already exist")
            } else throw t
        }
        return true
    }

    fun updateGroup(userId: String, pipelineGroup: PipelineGroupUpdate): Boolean {
        try {
            return pipelineGroupDao.update(
                dslContext = dslContext,
                projectId = pipelineGroup.projectId,
                groupId = decode(pipelineGroup.id),
                name = pipelineGroup.name,
                userId = userId
            )
        } catch (t: DataAccessException) {
            if (t.cause is SQLIntegrityConstraintViolationException) {
                logger.warn("Fail to create the group $pipelineGroup by userId $userId")
                throw OperationException("The group is already exist")
            } else throw t
        }
    }

    fun deleteGroup(userId: String, projectId: String, groupId: String): Boolean {
        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val id = decode(groupId)
            val result = pipelineGroupDao.delete(
                dslContext = context,
                projectId = projectId,
                groupId = id,
                userId = userId
            )
            pipelineLabelDao.deleteByGroupId(
                dslContext = context,
                projectId = projectId,
                groupId = id,
                userId = userId
            )
            result
        }
    }

    fun addLabel(userId: String, projectId: String, pipelineLabel: PipelineLabelCreate): Boolean {
        try {
            val groupId = decode(pipelineLabel.groupId)
            val labelCount = pipelineLabelDao.countByGroupId(
                dslContext = dslContext,
                projectId = projectId,
                groupId = groupId
            )
            if (labelCount >= MAX_LABEL_UNDER_GROUP) {
                throw ErrorCodeException(
                    errorCode = ERROR_LABEL_COUNT_EXCEEDS_LIMIT,
                    defaultMessage = "No more than $MAX_LABEL_UNDER_GROUP labels under a label group"
                )
            }
            if (pipelineLabel.name.length > MAX_LABEL_NAME_LENGTH) {
                throw ErrorCodeException(
                    errorCode = ERROR_LABEL_NAME_TOO_LONG,
                    defaultMessage = "label name cannot exceed $MAX_LABEL_NAME_LENGTH characters"
                )
            }
            val id = client.get(ServiceAllocIdResource::class).generateSegmentId("PIPELINE_LABEL").data
            pipelineLabelDao.create(
                dslContext = dslContext,
                projectId = projectId,
                groupId = groupId,
                name = pipelineLabel.name,
                userId = userId,
                id = id
            )
        } catch (t: DuplicateKeyException) {
            logger.warn("Fail to add the label $pipelineLabel by userId $userId")
            throw OperationException("The label is already exist")
        }

        return true
    }

    fun deleteLabel(userId: String, projectId: String, labelId: String): Boolean {
        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val id = decode(labelId)
            val result = pipelineLabelDao.deleteById(context, projectId = projectId, labelId = id, userId = userId)
            pipelineLabelPipelineDao.deleteByLabel(
                dslContext = context,
                projectId = projectId,
                labelId = id,
                userId = userId
            )
            measureEventDispatcher.dispatch(
                LabelChangeMetricsBroadCastEvent(
                    projectId = projectId,
                    type = PipelineLabelChangeTypeEnum.DELETE,
                    pipelineLabelRelateInfos = listOf(
                        PipelineLabelRelateInfo(
                            projectId = projectId,
                            labelId = id
                        )
                    )
                )
            )
            logger.info("LableChangeMetricsBroadCastEvent： deleteLabel $projectId|$id")
            result
        }
    }

    fun updateLabel(userId: String, projectId: String, pipelineLabel: PipelineLabelUpdate): Boolean {
        try {
            if (pipelineLabel.name.length > MAX_LABEL_NAME_LENGTH) {
                throw ErrorCodeException(
                    errorCode = ERROR_LABEL_NAME_TOO_LONG,
                    defaultMessage = "label name cannot exceed $MAX_LABEL_NAME_LENGTH characters"
                )
            }
            val result = pipelineLabelDao.update(
                dslContext = dslContext,
                projectId = projectId,
                labelId = decode(pipelineLabel.id),
                name = pipelineLabel.name,
                userId = userId
            )
            if (result) {
                measureEventDispatcher.dispatch(
                    LabelChangeMetricsBroadCastEvent(
                        projectId = projectId,
                        userId = userId,
                        type = PipelineLabelChangeTypeEnum.UPDATE,
                        statisticsTime = LocalDateTime.now(),
                        pipelineLabelRelateInfos = listOf(
                            PipelineLabelRelateInfo(
                                projectId = projectId,
                                labelId = decode(pipelineLabel.id),
                                name = pipelineLabel.name
                            )
                        )
                    )
                )
                logger.info("LableChangeMetricsBroadCastEvent： updateLabel $projectId|${decode(pipelineLabel.id)}")
            }
            return result
        } catch (t: DuplicateKeyException) {
            logger.warn("Fail to update the label $pipelineLabel by userId $userId")
            throw OperationException("The label is already exist")
        }
    }

    fun deletePipelineLabel(userId: String, projectId: String, pipelineId: String) {
        dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            pipelineLabelPipelineDao.deleteByPipeline(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId
            )
        }
        measureEventDispatcher.dispatch(
            LabelChangeMetricsBroadCastEvent(
                projectId = projectId,
                pipelineId = pipelineId,
                type = PipelineLabelChangeTypeEnum.DELETE,
                pipelineLabelRelateInfos = listOf(
                    PipelineLabelRelateInfo(
                        projectId = projectId,
                        pipelineId = pipelineId
                    )
                )
            )
        )
        logger.info("LableChangeMetricsBroadCastEvent： deletePipelineLabel $projectId|$pipelineId")
    }

    fun addPipelineLabel(userId: String, projectId: String, pipelineId: String, labelIds: List<String>) {
        if (labelIds.isEmpty()) {
            return
        }
        try {
            val labelIdArr = labelIds.map { decode(it) }.toSet()
            val pipelineLabelSegmentIdPairs = pipelineLabelSegmentIdPairs(labelIdArr)
            pipelineLabelPipelineDao.batchCreate(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineLabelRels = pipelineLabelSegmentIdPairs,
                userId = userId
            )

            val createData = pipelineLabelDao.getByIds(dslContext, projectId, labelIdArr)
            measureEventDispatcher.dispatch(
                LabelChangeMetricsBroadCastEvent(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    type = PipelineLabelChangeTypeEnum.CREATE,
                    pipelineLabelRelateInfos = createData.map {
                        PipelineLabelRelateInfo(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            labelId = it.id,
                            name = it.name,
                            createUser = userId,
                            createTime = it.createTime
                        )
                    }
                )
            )
            logger.info("LableChangeMetricsBroadCastEvent： addPipelineLabel $projectId|$labelIds")
        } catch (t: DuplicateKeyException) {
            logger.warn("Fail to add the pipeline $pipelineId label $labelIds by userId $userId")
            throw OperationException("The label is already exist")
        }
    }

    fun updatePipelineLabel(userId: String, projectId: String, pipelineId: String, labelIds: List<String>) {
        val labelIdArr = labelIds.map { decode(it) }.toSet()
        val pipelineLabelSegmentIdPairs = pipelineLabelSegmentIdPairs(labelIdArr)
        try {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                pipelineLabelPipelineDao.deleteByPipeline(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId
                )
                pipelineLabelPipelineDao.batchCreate(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineLabelRels = pipelineLabelSegmentIdPairs,
                    userId = userId
                )
            }
        } catch (t: DuplicateKeyException) {
            logger.warn("Fail to update the pipeline $pipelineId label $labelIds by userId $userId")
            throw OperationException("The label is already exist")
        }
        val deleteData = PipelineLabelRelateInfo(
            projectId = projectId,
            pipelineId = pipelineId
        )
        val createData = pipelineLabelDao.getByIds(dslContext, projectId, labelIdArr)
        measureEventDispatcher.dispatch(
            LabelChangeMetricsBroadCastEvent(
                projectId = projectId,
                pipelineId = pipelineId,
                type = PipelineLabelChangeTypeEnum.DELETE,
                pipelineLabelRelateInfos = listOf(deleteData)
            )
        )
        logger.info("LableChangeMetricsBroadCastEvent： updatePipelineLabel-delete $projectId|$pipelineId")
        measureEventDispatcher.dispatch(
            LabelChangeMetricsBroadCastEvent(
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                type = PipelineLabelChangeTypeEnum.CREATE,
                pipelineLabelRelateInfos = createData.map {
                    PipelineLabelRelateInfo(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        labelId = it.id,
                        name = it.name,
                        createUser = userId,
                        createTime = it.createTime
                    )
                }
            )
        )
        logger.info(
            "LableChangeMetricsBroadCastEvent： " +
                "updatePipelineLabel-create $projectId|$pipelineId|$labelIdArr|$labelIds"
        )
    }

    private fun pipelineLabelSegmentIdPairs(labelIdArr: Set<Long>): MutableList<Pair<Long, Long?>> {
        val generateSegmentIds = client.get(ServiceAllocIdResource::class)
            .batchGenerateSegmentId(PIPELINE_LABEL_PIPELINE_BIZ_TAG_NAME, labelIdArr.size)
        val pairs = mutableListOf<Pair<Long, Long?>>()
        var index = 0
        labelIdArr.forEach { pairs.add(Pair(it, generateSegmentIds.data!![index++])) }
        return pairs
    }

    fun getViewLabelToPipelinesMap(projectId: String, labels: List<String>): Map<String, List<String>> {
        val labelIds = labels.map { decode(it) }.toSet()
        if (labelIds.isEmpty()) {
            return emptyMap()
        }

        val pipelines = pipelineLabelPipelineDao.listPipelines(dslContext, projectId, labelIds)

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

    fun getGroupToLabelsMap(projectId: String, labels: List<String>): Map<String, List<String>> {
        val labelIds = labels.map { decode(it) }.toSet()
        val labelRecords = pipelineLabelDao.getByIds(dslContext, projectId, labelIds)

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
            val id = client.get(ServiceAllocIdResource::class).generateSegmentId("FAVOR_PIPELINE").data
            pipelineFavorDao.save(
                dslContext = dslContext,
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                id = id
            )
        } else {
            pipelineFavorDao.delete(
                dslContext = dslContext,
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId
            )
        }
        return true
    }

    // 删除流水线后联带删除整个流水线相关的收藏
    fun deleteAllUserFavorByPipeline(userId: String, projectId: String, pipelineId: String): Int {
        val count = pipelineFavorDao.deleteAllUserFavorByPipeline(dslContext, projectId, pipelineId)
        logger.info("Delete pipeline-favor of pipeline $pipelineId by user $userId. count=$count")
        return count
    }

    fun getFavorPipelines(userId: String, projectId: String): List<String> {
        return pipelineFavorDao.list(dslContext, userId, projectId).map { it.pipelineId }
    }

    private fun getLabelsGroupByGroup(projectId: String, labelIds: Set<Long>): List<PipelineGroup> {
        val labels = pipelineLabelDao.getByIds(dslContext, projectId, labelIds)
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
                    id = encode(g.id),
                    projectId = g.projectId,
                    name = g.name,
                    createTime = g.createTime.timestamp(),
                    updateTime = g.updateTime.timestamp(),
                    createUser = g.updateUser,
                    updateUser = g.updateUser,
                    labels = label.map {
                        PipelineLabel(
                            id = encode(it.id),
                            groupId = encode(it.groupId),
                            name = it.name,
                            createTime = it.createTime.timestamp(),
                            uptimeTime = it.updateTime.timestamp(),
                            createUser = it.createUser,
                            updateUser = it.updateUser
                        )
                    }
                )
            )
        }

        return result
    }

    fun getPipelinesGroupLabel(
        pipelineIds: Collection<String>,
        projectId: String
    ): Map<String, List<PipelineGroupLabels>> {
        val pipelineLabelRelRecords = pipelineLabelPipelineDao.listPipelineLabelRels(dslContext, pipelineIds, projectId)
        val result = mutableMapOf<String, MutableList<PipelineGroupLabels>>()
        val labelIds = mutableSetOf<Long>()
        pipelineLabelRelRecords?.forEach { pipelineLabelPipelineRecord ->
            labelIds.add(pipelineLabelPipelineRecord.labelId)
        }
        val labelDataMap = mutableMapOf<Long, TPipelineLabelRecord>()
        val groupIds = mutableSetOf<Long>()
        val pipelineLabelRecords = pipelineLabelDao.getByIds(dslContext, projectId, labelIds)
        pipelineLabelRecords.forEach { pipelineLabelRecord ->
            labelDataMap[pipelineLabelRecord.id] = pipelineLabelRecord
            groupIds.add(pipelineLabelRecord.groupId)
        }
        val groupDataMap = mutableMapOf<Long, TPipelineGroupRecord>()
        val pipelineGroupRecords = pipelineGroupDao.listByIds(dslContext, projectId, groupIds)
        pipelineGroupRecords.forEach { pipelineGroupRecord ->
            groupDataMap[pipelineGroupRecord.id] = pipelineGroupRecord
        }
        pipelineLabelRelRecords?.forEach { pipelineLabelPipelineRecord ->
            val pipelineId = pipelineLabelPipelineRecord.pipelineId
            val pipelineLabelRecord = labelDataMap[pipelineLabelPipelineRecord.labelId]
            val labelName = pipelineLabelRecord?.name
            val groupId = pipelineLabelRecord?.groupId
            val groupName = if (groupId != null) groupDataMap[groupId]?.name else null
            // groupName 和 labelName有可能为空
            if (!pipelineId.isNullOrBlank() && !groupName.isNullOrBlank() && !labelName.isNullOrBlank()) {
                if (result.containsKey(pipelineId)) {
                    var notHasGroupName = true
                    result[pipelineId]!!.forEach { pipelineGroupLabels ->
                        if (pipelineGroupLabels.groupName == groupName) {
                            notHasGroupName = false
                            if (!pipelineGroupLabels.labelName.contains(labelName)) {
                                pipelineGroupLabels.labelName.add(labelName)
                            }
                        }
                    }
                    if (notHasGroupName) {
                        result[pipelineId]!!.add(
                            PipelineGroupLabels(
                                groupName = groupName,
                                labelName = mutableListOf(labelName)
                            )
                        )
                    }
                } else {
                    result[pipelineId] =
                        mutableListOf(PipelineGroupLabels(groupName = groupName, labelName = mutableListOf(labelName)))
                }
            }
        }
        return result
    }

    private fun encode(id: Long) =
        HashUtil.encodeLongId(id)

    private fun decode(id: String) =
        HashUtil.decodeIdToLong(id)

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineGroupService::class.java)
        private const val MAX_GROUP_UNDER_PROJECT = 10
        private const val MAX_LABEL_UNDER_GROUP = 12
        private const val MAX_LABEL_NAME_LENGTH = 20
        private const val PIPELINE_LABEL_PIPELINE_BIZ_TAG_NAME = "PIPELINE_LABEL_PIPELINE"
    }

    @Suppress("UNUSED")
    fun getFavorPipelinesPage(
        userId: String,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<TPipelineFavorRecord>? {
        return pipelineFavorDao.listByUserId(dslContext, userId)
    }
}
