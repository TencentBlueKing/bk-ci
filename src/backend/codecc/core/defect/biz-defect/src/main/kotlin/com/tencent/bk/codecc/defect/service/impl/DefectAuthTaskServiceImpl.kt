package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.devops.common.api.QueryTaskListReqVO
import com.tencent.devops.common.auth.api.external.AuthTaskService
import com.tencent.devops.common.auth.api.pojo.external.KEY_CREATE_FROM
import com.tencent.devops.common.auth.api.pojo.external.KEY_PIPELINE_ID
import com.tencent.devops.common.auth.api.pojo.external.PREFIX_TASK_INFO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pojo.GongfengBaseInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
@Primary
class DefectAuthTaskServiceImpl @Autowired constructor(
    private val client: Client,
    private val redisTemplate: RedisTemplate<String, String>
) : AuthTaskService {

    /**
     * 查询任务创建来源
     */
    override fun getTaskCreateFrom(
        taskId: Long
    ): String {
        var createFrom = redisTemplate.opsForHash<String, String>().get(PREFIX_TASK_INFO + taskId, KEY_CREATE_FROM)
        if (createFrom.isNullOrEmpty()) {
            val taskInfo = client.get(ServiceTaskRestResource::class.java).getTaskInfoById(taskId)
            if (taskInfo != null && taskInfo.data != null) {
                val taskInfoEntity = taskInfo.data
                if (taskInfoEntity != null && !taskInfoEntity.createFrom.isNullOrEmpty()) {
                    createFrom = taskInfoEntity.createFrom
                    redisTemplate.opsForHash<String, String>().put(PREFIX_TASK_INFO + taskId, KEY_CREATE_FROM, createFrom)
                }
            }

        }
        return createFrom ?: ""
    }

    override fun getGongfengProjInfo(taskId: Long): GongfengBaseInfo? {
        // TODO("Not yet implemented")
        return null
    }

    /**
     * 获取任务所属流水线ID
     */
    override fun getTaskPipelineId(
        taskId: Long
    ): String {
        var pipelineId = redisTemplate.opsForHash<String, String>().get(PREFIX_TASK_INFO + taskId, KEY_PIPELINE_ID)
        if (pipelineId.isNullOrEmpty()) {
            val taskInfo = client.get(ServiceTaskRestResource::class.java).getTaskInfoById(taskId)
            if (taskInfo != null && taskInfo.data != null) {
                val taskInfoEntity = taskInfo.data
                if (taskInfoEntity != null && !taskInfoEntity.pipelineId.isNullOrEmpty()) {
                    pipelineId = taskInfoEntity.pipelineId
                    redisTemplate.opsForHash<String, String>().put(PREFIX_TASK_INFO + taskId, KEY_PIPELINE_ID, pipelineId)
                }
            }
        }
        return pipelineId ?: ""
    }

    override fun getGongfengCIProjInfo(gongfengId: Int): GongfengBaseInfo? {
        // TODO("Not yet implemented")
        return null
    }

    override fun queryPipelineListForUser(user: String, projectId: String, actions: Set<String>): Set<String> {
        val request = QueryTaskListReqVO()
        request.projectId = projectId
        return client.get(ServiceTaskRestResource::class.java).batchGetTaskList(request).data?.map { it.pipelineId }?.toSet()
            ?: setOf()
    }

    override fun queryPipelineListForUser(user: String, projectId: String): Set<String> {
        val request = QueryTaskListReqVO()
        request.projectId = projectId
        request.userId = user
        return client.get(ServiceTaskRestResource::class.java).batchGetTaskList(request).data?.map { it.pipelineId }?.toSet()
            ?: setOf()
    }

    override fun queryPipelineListByProjectId(projectId: String): Set<String> {
        val request = QueryTaskListReqVO()
        request.projectId = projectId
        return client.get(ServiceTaskRestResource::class.java)
            .batchGetTaskList(request).data?.map { it.pipelineId }?.toSet() ?: setOf()
    }

    override fun queryTaskListForUser(user: String, projectId: String, actions: Set<String>): Set<String> {
        val request = QueryTaskListReqVO()
        request.projectId = projectId
        return client.get(ServiceTaskRestResource::class.java).batchGetTaskList(request).data?.map { it.taskId.toString() }?.toSet()
            ?: setOf()
    }

    override fun queryTaskUserListForAction(taskId: String, projectId: String, actions: Set<String>): List<String> {
        val result = mutableListOf<String>()
        val request = QueryTaskListReqVO()
        request.projectId = projectId
        client.get(ServiceTaskRestResource::class.java).batchGetTaskList(request).data?.forEach { result.addAll(it.taskOwner) }
        return result
    }

    override fun queryTaskListByPipelineIds(pipelineIds: Set<String>): Set<String> {
        return client.get(ServiceTaskRestResource::class.java).queryTaskListByPipelineIds(pipelineIds).data ?: setOf()
    }

    override fun queryPipelineIdsByTaskIds(taskIds: Set<Long>): Set<String> {
        return client.get(ServiceTaskRestResource::class.java).getTaskInfosByIds(taskIds.toList()).data
            ?.filter { it != null && !it.pipelineId.isNullOrEmpty() }?.map { it.pipelineId }?.toSet() ?: emptySet()
    }
}