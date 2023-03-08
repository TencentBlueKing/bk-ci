package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TPipelineInfo
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class TxPipelineInfoDao {
    fun updateCreator(dslContext: DSLContext, pipelineId: String, newCreate: String) {
        with(TPipelineInfo.T_PIPELINE_INFO) {
            dslContext.update(this).set(CREATOR, newCreate).where(PIPELINE_ID.eq(pipelineId)).execute()
        }
    }
}
