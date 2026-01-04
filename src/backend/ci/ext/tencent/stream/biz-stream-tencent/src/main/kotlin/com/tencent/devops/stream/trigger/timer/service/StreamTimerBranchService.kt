package com.tencent.devops.stream.trigger.timer.service

import com.tencent.devops.stream.dao.StreamTimerBranchDao
import com.tencent.devops.stream.trigger.timer.pojo.StreamTimerBranch
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class StreamTimerBranchService(
    private val dslContext: DSLContext,
    private val streamTimerBranchDao: StreamTimerBranchDao
) {
    fun save(streamTimerBranch: StreamTimerBranch) {
        streamTimerBranchDao.save(
            dslContext = dslContext,
            streamTimerBranch = streamTimerBranch
        )
    }

    fun get(
        pipelineId: String,
        gitProjectId: Long,
        branch: String
    ): StreamTimerBranch? {
        return streamTimerBranchDao.get(
            dslContext = dslContext,
            pipelineId = pipelineId,
            gitProjectId = gitProjectId,
            branch = branch
        )
    }
}
