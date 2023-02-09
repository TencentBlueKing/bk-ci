package com.tencent.devops.process.service

import com.tencent.devops.process.dao.PipelineRecentUseDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 最近使用流水线服务
 */
@Service
class PipelineRecentUseService @Autowired constructor(
    private val pipelineRecentUseDao: PipelineRecentUseDao,
    private val dslContext: DSLContext
) {

    fun listPipelineIds(userId: String, projectId: String, noEmpty: Boolean = true): List<String> {
        val pipelineIds = pipelineRecentUseDao.listRecentPipelineIds(dslContext, projectId, userId, RECENT_USE_LIST_MAX)
        if (noEmpty && pipelineIds.isEmpty()) {
            return listOf("##NONE##")
        }
        return pipelineIds
    }

    fun record(userId: String, projectId: String, pipelineId: String) {
        pipelineRecentUseDao.add(dslContext, projectId, userId, pipelineId)
    }

    companion object {
        private const val RECENT_USE_LIST_MAX = 30
    }
}
