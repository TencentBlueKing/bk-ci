package com.tencent.bk.codecc.task.service.impl

import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.devops.common.auth.api.external.AuthTaskService
import com.tencent.devops.common.auth.api.pojo.external.KEY_CREATE_FROM
import com.tencent.devops.common.auth.api.pojo.external.KEY_PIPELINE_ID
import com.tencent.devops.common.auth.api.pojo.external.PREFIX_TASK_INFO
import com.tencent.devops.common.pojo.GongfengBaseInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
@Primary
class AuthTaskServiceImpl @Autowired constructor(
    private val taskRepository: TaskRepository,
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
            val taskInfoEntity = taskRepository.findFirstByTaskId(taskId)
            if (!taskInfoEntity.createFrom.isNullOrEmpty()) {
                createFrom = taskInfoEntity.createFrom
                redisTemplate.opsForHash<String, String>().put(PREFIX_TASK_INFO + taskId, KEY_CREATE_FROM, createFrom)
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
            val taskInfoEntity = taskRepository.findFirstByTaskId(taskId)
            if (!taskInfoEntity.pipelineId.isNullOrEmpty()) {
                pipelineId = taskInfoEntity.pipelineId
                redisTemplate.opsForHash<String, String>().put(PREFIX_TASK_INFO + taskId, KEY_PIPELINE_ID, pipelineId)
            }
        }
        return pipelineId ?: ""
    }

    override fun getGongfengCIProjInfo(gongfengId: Int): GongfengBaseInfo? {
        // TODO("Not yet implemented")
        return null
    }

    override fun queryPipelineListForUser(user: String, projectId: String, actions: Set<String>): Set<String> {
        return taskRepository.findByProjectId(projectId).map { it.pipelineId }.toSet()
    }

    override fun queryPipelineListForUser(user: String, projectId: String): Set<String> {
        return taskRepository.findByProjectId(projectId).filter { it.taskMember.contains(user) }.map { it.pipelineId }.toSet()
    }

    override fun queryPipelineListByProjectId(projectId: String): Set<String> {
        return taskRepository.findByProjectId(projectId).map { it.pipelineId }.toSet()
    }

    override fun queryTaskListForUser(user: String, projectId: String, actions: Set<String>): Set<String> {
        return taskRepository.findByProjectId(projectId).map { it.taskId.toString() }.toSet()
    }

    override fun queryTaskUserListForAction(taskId: String, projectId: String, actions: Set<String>): List<String> {
        val result = mutableListOf<String>()
        taskRepository.findByProjectId(projectId).forEach { result.addAll(it.taskOwner) }
        return result
    }

    override fun queryTaskListByPipelineIds(pipelineIds: Set<String>): Set<String> {
        return taskRepository.findByPipelineIdIn(pipelineIds).map { it.taskId.toString() }.toSet()
    }

    override fun queryPipelineIdsByTaskIds(taskIds: Set<Long>): Set<String> {
        return taskRepository.findByTaskIdIn(taskIds).filter { it.pipelineId != null }.map { it.pipelineId }.toSet()
    }
}