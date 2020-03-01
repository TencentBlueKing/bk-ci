package com.tencent.devops.process.dao.normal

import com.tencent.devops.common.pipeline.pojo.PipelineBuildBaseInfo
import com.tencent.devops.model.process.tables.TPipelineResource
import com.tencent.devops.process.listener.PipelineHardDeleteListener
import org.jooq.DSLContext

/**
 * @Description
 * @Date 2020/2/28
 * @Version 1.0
 */
class PipelineResourceDao : PipelineHardDeleteListener {
    override fun onPipelineDeleteHardly(dslContext: DSLContext, operator: String, pipelineBuildBaseInfoList: List<PipelineBuildBaseInfo>): Boolean {
        pipelineBuildBaseInfoList.forEach { pipelineBuildBaseInfo ->
            with(TPipelineResource.T_PIPELINE_RESOURCE) {
                dslContext.deleteFrom(this)
                    .where(PIPELINE_ID.eq(pipelineBuildBaseInfo.pipelineId))
                    .execute()
            }
        }
        return true
    }
}
