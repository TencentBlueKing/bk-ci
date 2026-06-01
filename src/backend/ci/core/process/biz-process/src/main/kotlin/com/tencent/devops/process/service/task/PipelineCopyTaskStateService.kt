package com.tencent.devops.process.service.task

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 复制任务状态服务
 */
@Service
class PipelineCopyTaskStateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val pipelineBatchTaskDao: PipelineBatchTaskDao
) {
    fun updateTaskStatusWithLock(
        projectId: String,
        taskId: String,
        status: PipelineBatchTaskStatus
    ) {
        val lock = PipelineCopyTaskLock(
            redisOperation = redisOperation,
            projectId = projectId,
            taskId = taskId
        )
        try {
            lock.lock()
            pipelineBatchTaskDao.updateStatus(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                status = status
            )
        } finally {
            lock.unlock()
        }
    }
}
