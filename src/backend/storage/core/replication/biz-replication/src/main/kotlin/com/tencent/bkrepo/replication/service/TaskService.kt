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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.replication.service

import com.tencent.bkrepo.common.api.constant.StringPool.UNKNOWN
import com.tencent.bkrepo.common.api.constant.StringPool.uniqueId
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.replication.api.ReplicationClient
import com.tencent.bkrepo.replication.config.FeignClientFactory
import com.tencent.bkrepo.replication.job.ReplicationContext
import com.tencent.bkrepo.replication.message.ReplicationMessageCode
import com.tencent.bkrepo.replication.model.TReplicationTask
import com.tencent.bkrepo.replication.pojo.request.ReplicationTaskCreateRequest
import com.tencent.bkrepo.replication.pojo.setting.ExecutionPlan
import com.tencent.bkrepo.replication.pojo.setting.RemoteClusterInfo
import com.tencent.bkrepo.replication.pojo.task.ReplicationStatus
import com.tencent.bkrepo.replication.pojo.task.ReplicationStatus.Companion.UNDO_STATUS_SET
import com.tencent.bkrepo.replication.pojo.task.ReplicationTaskInfo
import com.tencent.bkrepo.replication.pojo.task.ReplicationType
import com.tencent.bkrepo.replication.repository.TaskLogRepository
import com.tencent.bkrepo.replication.repository.TaskRepository
import org.quartz.CronExpression
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val taskLogRepository: TaskLogRepository,
    private val mongoTemplate: MongoTemplate
) {

    fun create(userId: String, request: ReplicationTaskCreateRequest): ReplicationTaskInfo {
        with(request) {
            validate(this)
            val task = TReplicationTask(
                key = uniqueId(),
                includeAllProject = includeAllProject,
                localProjectId = if (includeAllProject) null else localProjectId,
                localRepoName = if (includeAllProject) null else localRepoName,
                remoteProjectId = if (includeAllProject) null else remoteProjectId,
                remoteRepoName = if (includeAllProject) null else remoteRepoName,
                type = type,
                setting = setting,
                status = ReplicationStatus.WAITING,

                createdBy = userId,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = userId,
                lastModifiedDate = LocalDateTime.now()
            )
            taskRepository.insert(task)
            logger.info("Create replica task[$request] success.")
            return convert(task)!!
        }
    }

    fun detail(taskKey: String): ReplicationTaskInfo? {
        return taskRepository.findByKey(taskKey)?.let { convert(it) }
    }

    fun listAllRemoteTask(type: ReplicationType): List<TReplicationTask> {
        val typeCriteria = Criteria.where(TReplicationTask::type.name).`is`(type)
        val statusCriteria = Criteria.where(TReplicationTask::status.name).`in`(ReplicationStatus.WAITING, ReplicationStatus.REPLICATING)
        val criteria = Criteria().andOperator(typeCriteria, statusCriteria)

        return mongoTemplate.find(Query(criteria), TReplicationTask::class.java)
    }

    fun listRelativeTask(type: ReplicationType, localProjectId: String?, localRepoName: String?): List<TReplicationTask> {
        val typeCriteria = Criteria.where(TReplicationTask::type.name).`is`(type)
        val statusCriteria = Criteria.where(TReplicationTask::status.name).`in`(ReplicationStatus.WAITING, ReplicationStatus.REPLICATING)
        val includeAllCriteria = Criteria.where(TReplicationTask::includeAllProject.name).`is`(true)
        val projectCriteria = Criteria.where(TReplicationTask::localProjectId.name).`is`(localProjectId)
        val repoCriteria = Criteria.where(TReplicationTask::localProjectId.name).`is`(localProjectId)
            .orOperator(
                Criteria.where(TReplicationTask::localRepoName.name).`is`(localRepoName),
                Criteria.where(TReplicationTask::localRepoName.name).`is`(null)
            )
        val detailCriteria = if (localProjectId == null && localRepoName == null) {
            includeAllCriteria
        } else if (localProjectId != null && localRepoName == null) {
            Criteria().orOperator(includeAllCriteria, projectCriteria)
        } else {
            Criteria().orOperator(includeAllCriteria, repoCriteria)
        }
        val criteria = Criteria().andOperator(typeCriteria, statusCriteria, detailCriteria)

        return mongoTemplate.find(Query(criteria), TReplicationTask::class.java)
    }

    fun listUndoFullTask(): List<TReplicationTask> {
        val criteria = Criteria.where(TReplicationTask::type.name).`is`(ReplicationType.FULL)
            .and(TReplicationTask::status.name).`in`(UNDO_STATUS_SET)
        return mongoTemplate.find(Query(criteria), TReplicationTask::class.java)
    }

    fun list(): List<ReplicationTaskInfo> {
        return taskRepository.findAll().map { convert(it)!! }
    }

    fun interrupt(taskKey: String) {
        val task =
            taskRepository.findByKey(taskKey) ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, taskKey)
        if (task.status == ReplicationStatus.REPLICATING) {
            task.status = ReplicationStatus.INTERRUPTED
            taskRepository.save(task)
        } else {
            throw ErrorCodeException(ReplicationMessageCode.TASK_STATUS_INVALID)
        }
    }

    fun delete(taskKey: String) {
        val task =
            taskRepository.findByKey(taskKey) ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, taskKey)
        taskRepository.delete(task)
        if (task.type == ReplicationType.FULL) {
            taskLogRepository.deleteByTaskKey(taskKey)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun tryConnect(remoteClusterInfo: RemoteClusterInfo) {
        with(remoteClusterInfo) {
            try {
                val replicationService = FeignClientFactory.create(ReplicationClient::class.java, this)
                val authToken = ReplicationContext.encodeAuthToken(username, password)
                replicationService.ping(authToken)
            } catch (exception: RuntimeException) {
                throw ErrorCodeException(ReplicationMessageCode.REMOTE_CLUSTER_CONNECT_ERROR, exception.message ?: UNKNOWN)
            }
        }
    }

    private fun validate(request: ReplicationTaskCreateRequest) {
        with(request) {
            if (!includeAllProject && localProjectId == null) {
                throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, request::localProjectId.name)
            }
            if (!setting.executionPlan.executeImmediately && setting.executionPlan.executeTime == null) {
                val cronExpression = setting.executionPlan.cronExpression
                    ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, ExecutionPlan::cronExpression.name)
                if (!CronExpression.isValidExpression(cronExpression)) {
                    throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, cronExpression)
                }
            }
            if (validateConnectivity) {
                tryConnect(setting.remoteClusterInfo)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskService::class.java)

        private fun convert(task: TReplicationTask?): ReplicationTaskInfo? {
            return task?.let {
                ReplicationTaskInfo(
                    id = it.id!!,
                    key = it.key,
                    includeAllProject = it.includeAllProject,
                    localProjectId = it.localProjectId,
                    localRepoName = it.localRepoName,
                    remoteProjectId = it.remoteProjectId,
                    remoteRepoName = it.remoteRepoName,
                    type = it.type,
                    setting = it.setting,
                    status = it.status,

                    createdBy = it.createdBy,
                    createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    lastModifiedBy = it.lastModifiedBy,
                    lastModifiedDate = it.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME)
                )
            }
        }
    }
}
