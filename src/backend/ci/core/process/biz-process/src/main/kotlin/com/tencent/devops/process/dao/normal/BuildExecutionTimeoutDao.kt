package com.tencent.devops.process.dao.normal

import com.tencent.devops.common.pipeline.pojo.PipelineBuildBaseInfo
import com.tencent.devops.model.process.tables.TPipelineBuildExecutionTimeout
import com.tencent.devops.process.listener.PipelineHardDeleteListener
import org.jooq.DSLContext

/**
 * @Description
 * @Date 2020/2/28
 * @Version 1.0
 */
class BuildExecutionTimeoutDao : PipelineHardDeleteListener {
    override fun onPipelineDeleteHardly(dslContext: DSLContext, operator: String, pipelineBuildBaseInfoList: List<PipelineBuildBaseInfo>): Boolean {
        pipelineBuildBaseInfoList.forEach {
            with(TPipelineBuildExecutionTimeout.T_PIPELINE_BUILD_EXECUTION_TIMEOUT) {
                dslContext.deleteFrom(this)
                    .where(PIPELINE_ID.eq(it.pipelineId))
            }
        }
        return true
    }
}
