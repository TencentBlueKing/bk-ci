package com.tencent.devops.process.dao.normal

import com.tencent.devops.common.pipeline.pojo.PipelineBuildBaseInfo
import com.tencent.devops.model.process.tables.TPipelineBuildNo
import com.tencent.devops.process.listener.PipelineHardDeleteListener
import org.jooq.DSLContext

/**
 * @Description
 * @Date 2020/2/28
 * @Version 1.0
 */
class BuildNoDao : PipelineHardDeleteListener {
    override fun onPipelineDeleteHardly(dslContext: DSLContext, operator: String, pipelineBuildBaseInfoList: List<PipelineBuildBaseInfo>): Boolean {
        pipelineBuildBaseInfoList.forEach { pipelineBuildBaseInfo ->
            with(TPipelineBuildNo.T_PIPELINE_BUILD_NO) {
                dslContext.deleteFrom(this)
                    .where(PIPELINE_ID.eq(pipelineBuildBaseInfo.pipelineId))
                    .execute()
            }
        }
        return true
    }
}
