package com.tencent.devops.process.dao.normal

import com.tencent.devops.common.pipeline.pojo.PipelineBuildBaseInfo
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import com.tencent.devops.process.listener.PipelineHardDeleteListener
import org.jooq.DSLContext

/**
 * @Description
 * @Date 2020/2/28
 * @Version 1.0
 */
class BuildHistoryDao : PipelineHardDeleteListener {
    override fun onPipelineDeleteHardly(dslContext: DSLContext, operator: String, pipelineBuildBaseInfoList: List<PipelineBuildBaseInfo>): Boolean {
        pipelineBuildBaseInfoList.forEach { pipelineBuildBaseInfo ->
            with(TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY) {
                dslContext.deleteFrom(this)
                    .where(PROJECT_ID.eq(pipelineBuildBaseInfo.projectCode).and(PIPELINE_ID.eq(pipelineBuildBaseInfo.pipelineId)))
                    .execute()
            }
        }
        return true
    }
}
