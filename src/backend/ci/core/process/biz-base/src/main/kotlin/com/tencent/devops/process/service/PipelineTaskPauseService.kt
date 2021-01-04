package com.tencent.devops.process.service

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.dao.PipelinePauseValueDao
import com.tencent.devops.process.engine.pojo.PipelinePauseValue
import com.tencent.devops.process.engine.utils.PauseRedisUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineTaskPauseService @Autowired constructor(
    private val pipelinePauseValueDao: PipelinePauseValueDao,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation
) {
    // 重置暂停任务暂停状态位
    fun pauseTaskFinishExecute(buildId: String, taskId: String) {
        redisOperation.delete(PauseRedisUtils.getPauseRedisKey(buildId, taskId))
    }

    fun savePauseValue(pipelinePauseValue: PipelinePauseValue) {
        pipelinePauseValueDao.save(dslContext, pipelinePauseValue)
    }
}
