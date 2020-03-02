package com.tencent.devops.process.dao.normal

import com.tencent.devops.common.pipeline.pojo.PipelineBuildBaseInfo
import com.tencent.devops.model.process.tables.TGrayPipelineBuild
import com.tencent.devops.process.listener.PipelineHardDeleteListener
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

/**
 * @Description
 * @Date 2020/2/28
 * @Version 1.0
 */
@Repository
class GrayPipelineBuildDao : PipelineHardDeleteListener {
    override fun onPipelineDeleteHardly(dslContext: DSLContext, operator: String, pipelineBuildBaseInfoList: List<PipelineBuildBaseInfo>): Boolean {
        pipelineBuildBaseInfoList.forEach {
            with(TGrayPipelineBuild.T_GRAY_PIPELINE_BUILD) {
                dslContext.deleteFrom(this)
                    .where(PIPELINE_ID.eq(it.pipelineId))
                    .execute()
            }
        }
        return true
    }
}
