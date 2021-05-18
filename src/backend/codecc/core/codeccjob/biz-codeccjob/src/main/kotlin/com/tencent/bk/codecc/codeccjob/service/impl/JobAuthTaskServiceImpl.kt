package com.tencent.bk.codecc.codeccjob.service.impl

import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.devops.common.auth.api.external.AuthTaskService
import com.tencent.devops.common.auth.api.pojo.external.KEY_CREATE_FROM
import com.tencent.devops.common.auth.api.pojo.external.KEY_PIPELINE_ID
import com.tencent.devops.common.auth.api.pojo.external.PREFIX_TASK_INFO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pojo.GongfengBaseInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class JobAuthTaskServiceImpl @Autowired constructor(
        private val client: Client,
        private val redisTemplate: RedisTemplate<String, String>
) : AuthTaskService {


    override fun getGongfengProjInfo(taskId: Long): GongfengBaseInfo? {
        return client.get(ServiceTaskRestResource::class.java).getGongfengBaseInfo(taskId).data
    }

    /**
     * 查询任务创建来源
     */
    override fun getTaskCreateFrom(
            taskId: Long
    ): String {
        var createFrom = redisTemplate.opsForHash<String, String>().get(PREFIX_TASK_INFO + taskId, KEY_CREATE_FROM)
        if (createFrom.isNullOrEmpty()) {
            val taskInfo = client.get(ServiceTaskRestResource::class.java).getTaskInfoById(taskId)
            if (taskInfo?.data != null) {
                val taskInfoEntity = taskInfo.data
                if (taskInfoEntity != null && !taskInfoEntity.createFrom.isNullOrEmpty()) {
                    createFrom = taskInfoEntity.createFrom
                    redisTemplate.opsForHash<String, String>().put(PREFIX_TASK_INFO + taskId, KEY_CREATE_FROM, createFrom)
                }
            }

        }
        return createFrom
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
        return pipelineId
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobAuthTaskServiceImpl::class.java)
    }

}