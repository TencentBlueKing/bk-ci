package com.tencent.devops.process.dao.normal

import com.tencent.devops.common.pipeline.pojo.PipelineBuildBaseInfo
import com.tencent.devops.model.process.tables.TPipelineBuildSkipElements
import com.tencent.devops.process.listener.PipelineHardDeleteListener
import org.jooq.DSLContext

/**
 * @Description
 * @Date 2020/2/28
 * @Version 1.0
 */
class BuildSkipElementsDao : PipelineHardDeleteListener {
    override fun onPipelineDeleteHardly(dslContext: DSLContext, operator: String, pipelineBuildBaseInfoList: List<PipelineBuildBaseInfo>): Boolean {
        pipelineBuildBaseInfoList.forEach { pipelineBuildBaseInfo ->
            with(TPipelineBuildSkipElements.T_PIPELINE_BUILD_SKIP_ELEMENTS) {
                pipelineBuildBaseInfo.buildIdList.forEach {
                    dslContext.deleteFrom(this)
                        .where(BUILD_ID.eq(it))
                        .execute()
                }
            }
        }
        return true
    }
}
