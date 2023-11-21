package com.tencent.devops.process.service

import com.tencent.devops.process.dao.PipelineRecentUseDao
import org.apache.lucene.util.NamedThreadFactory
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

/**
 * 最近使用流水线服务
 */
@Service
class PipelineRecentUseService @Autowired constructor(
    private val pipelineRecentUseDao: PipelineRecentUseDao,
    private val dslContext: DSLContext
) {

    private val threadPool = Executors.newSingleThreadExecutor(NamedThreadFactory("RecentUseService"))

    fun listPipelineIds(userId: String, projectId: String, noEmpty: Boolean = true): List<String> {
        val pipelineIds = pipelineRecentUseDao.listRecentPipelineIds(dslContext, projectId, userId, RECENT_USE_LIST_MAX)
        if (noEmpty && pipelineIds.isEmpty()) {
            return listOf("##NONE##")
        }
        return pipelineIds
    }

    fun record(userId: String, projectId: String, pipelineId: String) {
        threadPool.submit {
            pipelineRecentUseDao.add(dslContext, projectId, userId, pipelineId)
            // 按时间倒序, 取出第30条的时间
            val endTime = pipelineRecentUseDao.listLastUseTimes(
                dslContext, projectId, userId, RECENT_USE_LIST_MAX
            ).last()
            // 删除该时间之前时间的数据
            pipelineRecentUseDao.deleteExpire(dslContext, projectId, userId, endTime)
        }
    }

    companion object {
        private const val RECENT_USE_LIST_MAX = 30
    }
}
