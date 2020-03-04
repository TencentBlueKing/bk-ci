package com.tencent.devops.process.dao.normal

import com.tencent.devops.common.pipeline.pojo.PipelineBuildBaseInfo
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import com.tencent.devops.process.listener.PipelineHardDeleteListener
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

/**
 * @Description
 * @Date 2020/2/28
 * @Version 1.0
 */
@Repository
class BuildHistoryDao : PipelineHardDeleteListener {
    override fun onPipelineDeleteHardly(dslContext: DSLContext, pipelineBuildBaseInfoList: List<PipelineBuildBaseInfo>): Boolean {
        // 主干数据，由调用方在关联数据删除完成后最后删除，此处不作处理
        return true
    }

    fun deletePipelinesHardly(dslContext: DSLContext, pipelineBuildBaseInfoList: List<PipelineBuildBaseInfo>): Boolean {
        with(TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.`in`(pipelineBuildBaseInfoList.map { it.pipelineId }))
                .execute()
        }
        return true
    }
}
