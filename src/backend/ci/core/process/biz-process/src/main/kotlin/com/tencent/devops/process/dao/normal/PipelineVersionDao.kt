package com.tencent.devops.process.dao.normal

import com.tencent.devops.common.pipeline.pojo.PipelineBuildBaseInfo
import com.tencent.devops.model.process.tables.TPipelineVersion
import com.tencent.devops.process.listener.PipelineHardDeleteListener
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

/**
 * @Description
 * @Date 2020/2/28
 * @Version 1.0
 */
@Repository
class PipelineVersionDao : PipelineHardDeleteListener {
    override fun onPipelineDeleteHardly(dslContext: DSLContext, pipelineBuildBaseInfoList: List<PipelineBuildBaseInfo>): Boolean {
        with(TPipelineVersion.T_PIPELINE_VERSION) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.`in`(pipelineBuildBaseInfoList.map { it.pipelineId }))
                .execute()
        }
        return true
    }
}
